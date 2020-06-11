package org.nanopub.extra.server;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.ConnectionPoolTimeoutException;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.nanopub.Nanopub;
import org.nanopub.NanopubUtils;
import org.nanopub.extra.index.IndexUtils;
import org.nanopub.extra.index.NanopubIndex;

import net.trustyuri.TrustyUriUtils;

public class FetchIndex {

	public static final int maxParallelRequestsPerServer = 5;

	private OutputStream out;
	private RDFFormat format;
	private boolean writeIndex, writeContent;
	private boolean running = false;
	private List<FetchNanopubTask> fetchTasks;
	private List<ServerInfo> servers;
	private Map<String,Set<FetchNanopubTask>> serverLoad;
	private Map<String,NanopubSurfacePattern> serverPatterns;
	private Map<String,Integer> serverUsage;
	private int nanopubCount;
	private Listener listener;
	private HttpClient httpClient;

	protected FetchIndex() {
	}

	public FetchIndex(String indexUri, OutputStream out, RDFFormat format, boolean writeIndex, boolean writeContent) {
		this.out = out;
		this.format = format;
		this.writeIndex = writeIndex;
		this.writeContent = writeContent;
		fetchTasks = new ArrayList<>();
		fetchTasks.add(new FetchNanopubTask(indexUri, true));
		servers = new ArrayList<>();
		serverLoad = new HashMap<>();
		serverPatterns = new HashMap<>();
		serverUsage = new HashMap<>();
		ServerIterator serverIterator = new ServerIterator();
		while (serverIterator.hasNext()) {
			ServerInfo serverInfo = serverIterator.next();
			servers.add(serverInfo);
			serverLoad.put(serverInfo.getPublicUrl(), new HashSet<FetchNanopubTask>());
			serverPatterns.put(serverInfo.getPublicUrl(), new NanopubSurfacePattern(serverInfo));
			serverUsage.put(serverInfo.getPublicUrl(), 0);
		}
		try {
			ServerIterator.writeCachedServers(servers);
		} catch (Exception ex) {}
		nanopubCount = 0;
		RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(2000)
				.setConnectionRequestTimeout(100).setSocketTimeout(2000).build();
		PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager();
		connManager.setDefaultMaxPerRoute(10);
		connManager.setMaxTotal(1000);
		httpClient = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig)
				.setConnectionManager(connManager).build();
	}

	public void run() {
		if (running) return;
		running = true;
		while (!fetchTasks.isEmpty()) {
			checkTasks();
			try {
				Thread.sleep(5);
			} catch (InterruptedException ex) {}
		}
	}

	private void checkTasks() {
		for (FetchNanopubTask task : new ArrayList<>(fetchTasks)) {
			if (task.isRunning()) continue;
			if (task.isCancelled()) {
				fetchTasks.remove(task);
				continue;
			}
			if (task.getLastServerUrl() != null) {
				serverLoad.get(task.getLastServerUrl()).remove(task);
			}
			if (task.getNanopub() == null) {
				if (task.getTriedServersCount() == servers.size()) {
//					task.resetServers();
					System.err.println("Failed to get " + task.getNanopubUri());
					fetchTasks.remove(task);
					continue;
				}
				List<ServerInfo> shuffledServers = new ArrayList<>(servers);
				Collections.shuffle(shuffledServers);
				for (ServerInfo serverInfo : shuffledServers) {
					String serverUrl = serverInfo.getPublicUrl();
					if (task.hasServerBeenTried(serverUrl)) continue;
					if (!serverPatterns.get(serverUrl).matchesUri(task.getNanopubUri())) {
						task.ignoreServer(serverUrl);
						continue;
					}
					int load = serverLoad.get(serverUrl).size();
					if (load >= maxParallelRequestsPerServer) {
						task.ignoreServer(serverUrl);
						continue;
					}
					assignTask(task, serverUrl);
					break;
				}
			} else if (task.isIndex()) {
				if (fetchTasks.size() < 3000) {
					try {
						Nanopub np = task.getNanopub();
						if (!IndexUtils.isIndex(np)) {
							throw new RuntimeException("NOT AN INDEX: " + np.getUri());
						}
						NanopubIndex npi = IndexUtils.castToIndex(np);
						if (writeIndex) {
							writeNanopub(npi);
						}
						if (writeContent) {
							for (IRI elementUri : npi.getElements()) {
								fetchTasks.add(new FetchNanopubTask(elementUri.toString(), false));
							}
						}
						for (IRI subIndexUri : npi.getSubIndexes()) {
							// Failing to get subindexes can block the entire process, therefore
							// we launch three sibling tasks at the same time:
							FetchNanopubTask t1 = new FetchNanopubTask(subIndexUri.toString(), true);
							fetchTasks.add(0, t1);
							FetchNanopubTask t2 = new FetchNanopubTask(subIndexUri.toString(), true, t1);
							fetchTasks.add(0, t2);
							FetchNanopubTask t3 = new FetchNanopubTask(subIndexUri.toString(), true, t1, t2);
							fetchTasks.add(0, t3);
						}
						if (npi.getAppendedIndex() != null) {
							// Failing to get appended indexes can block the entire process, therefore
							// we launch three sibling tasks at the same time:
							FetchNanopubTask t1 = new FetchNanopubTask(npi.getAppendedIndex().toString(), true);
							fetchTasks.add(0, t1);
							FetchNanopubTask t2 = new FetchNanopubTask(npi.getAppendedIndex().toString(), true, t1);
							fetchTasks.add(0, t2);
							FetchNanopubTask t3 = new FetchNanopubTask(npi.getAppendedIndex().toString(), true, t1, t2);
							fetchTasks.add(0, t3);
						}
					} catch (Exception ex) {
						throw new RuntimeException(ex);
					}
					fetchTasks.remove(task);
				}
			} else {
				try {
					writeNanopub(task.getNanopub());
				} catch (Exception ex) {
					throw new RuntimeException(ex);
				}
				fetchTasks.remove(task);
			}
		}
	}

	private void writeNanopub(Nanopub np) throws RDFHandlerException {
		nanopubCount++;
		if (listener != null && nanopubCount % 100 == 0) {
			listener.progress(nanopubCount);
		}
		NanopubUtils.writeToStream(np, out, format);
	}

	public int getNanopubCount() {
		return nanopubCount;
	}

	public List<ServerInfo> getServers() {
		return new ArrayList<>(servers);
	}

	public int getServerUsage(ServerInfo si) {
		return serverUsage.get(si.getPublicUrl());
	}

	public void setProgressListener(Listener l) {
		listener = l;
	}

	private void assignTask(final FetchNanopubTask task, final String serverUrl) {
		task.prepareForTryingServer(serverUrl);
		serverLoad.get(serverUrl).add(task);
		Runnable runFetchTask = new Runnable() {

			@Override
			public void run() {
				task.tryServer(serverUrl);
			}

		};
		Thread thread = new Thread(runFetchTask);
		thread.start();
	}

	private class FetchNanopubTask {

		private String npUri;
		private boolean isIndex;
		private Nanopub nanopub;
		private Set<String> servers = new HashSet<>();
		private boolean running = false;
		private boolean cancelled = false;
		private String lastServerUrl;
		private Set<FetchNanopubTask> siblings;

		public FetchNanopubTask(String npUri, boolean isIndex, FetchNanopubTask... siblings) {
			this.npUri = npUri;
			this.isIndex = isIndex;
			this.siblings = new HashSet<>(Arrays.asList(siblings));
			for (FetchNanopubTask s : siblings) {
				s.siblings.add(this);
			}
		}

		public boolean isIndex() {
			return isIndex;
		}

		public Nanopub getNanopub() {
			return nanopub;
		}

		public String getNanopubUri() {
			return npUri;
		}

		public boolean isRunning() {
			return running;
		}

		public boolean isCancelled() {
			return cancelled;
		}

		public boolean hasServerBeenTried(String serverUrl) {
			return servers.contains(serverUrl);
		}

		public void resetServers() {
			servers.clear();
		}

		public int getTriedServersCount() {
			return servers.size();
		}

		public String getLastServerUrl() {
			return lastServerUrl;
		}

		public void ignoreServer(String serverUrl) {
			servers.add(serverUrl);
		}

		public void prepareForTryingServer(String serverUrl) {
			servers.add(serverUrl);
			lastServerUrl = serverUrl;
			running = true;
		}

		public void tryServer(String serverUrl) {
			boolean serverTried = false;
			try {
				serverTried = true;
				nanopub = GetNanopub.get(TrustyUriUtils.getArtifactCode(npUri), serverUrl, httpClient);
			} catch (ConnectionPoolTimeoutException ex) {
				serverTried = false;
				// too many connection attempts; try again later
			} catch (Exception ex) {
				if (listener != null) listener.exceptionHappened(ex, serverUrl, TrustyUriUtils.getArtifactCode(npUri));
			} finally {
				running = false;
				if (serverTried) {
					synchronized (FetchIndex.class) {
						if (cancelled == true) {
							// Sibling already did the work...
						} else {
							for (FetchNanopubTask s : siblings) {
								s.cancelled = true;
							}
							serverUsage.put(serverUrl, serverUsage.get(serverUrl) + 1);
						}
					}
				}
			}
		}

	}


	public static interface Listener {

		public void progress(int count);

		public void exceptionHappened(Exception ex, String serverUrl, String artifactCode);

	}

}
