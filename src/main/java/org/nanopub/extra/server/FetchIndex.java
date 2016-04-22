package org.nanopub.extra.server;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.trustyuri.TrustyUriUtils;

import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.nanopub.Nanopub;
import org.nanopub.NanopubUtils;
import org.nanopub.extra.index.IndexUtils;
import org.nanopub.extra.index.NanopubIndex;
import org.openrdf.model.URI;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;

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
	private int nanopubCount;
	private Listener listener;
	private HttpClient httpClient;

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
		ServerIterator serverIterator = new ServerIterator();
		while (serverIterator.hasNext()) {
			ServerInfo serverInfo = serverIterator.next();
			servers.add(serverInfo);
			serverLoad.put(serverInfo.getPublicUrl(), new HashSet<FetchNanopubTask>());
			serverPatterns.put(serverInfo.getPublicUrl(), new NanopubSurfacePattern(serverInfo));
		}
		try {
			ServerIterator.writeCachedServers(servers);
		} catch (Exception ex) {}
		nanopubCount = 0;
		RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(5000)
				.setConnectionRequestTimeout(100).setSocketTimeout(5000).build();
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
		}
	}

	private void checkTasks() {
		for (FetchNanopubTask task : new ArrayList<>(fetchTasks)) {
			if (task.isRunning()) continue;
			if (task.getLastServerUrl() != null) {
				serverLoad.get(task.getLastServerUrl()).remove(task);
			}
			if (task.getNanopub() == null) {
				if (task.getTriedServersCount() == servers.size()) {
					task.resetServers();
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
				if (fetchTasks.size() < 1000) {
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
							for (URI elementUri : npi.getElements()) {
								fetchTasks.add(new FetchNanopubTask(elementUri.toString(), false));
							}
						}
						for (URI subIndexUri : npi.getSubIndexes()) {
							fetchTasks.add(new FetchNanopubTask(subIndexUri.toString(), true));
						}
						if (npi.getAppendedIndex() != null) {
							fetchTasks.add(new FetchNanopubTask(npi.getAppendedIndex().toString(), true));
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
		private String lastServerUrl;

		public FetchNanopubTask(String npUri, boolean isIndex) {
			this.npUri = npUri;
			this.isIndex = isIndex;
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
			try {
				nanopub = GetNanopub.get(TrustyUriUtils.getArtifactCode(npUri), serverUrl, httpClient);
			} catch (Exception ex) {
				if (listener != null) listener.exceptionHappened(ex);
			} finally {
				running = false;
			}
		}

	}


	public static interface Listener {

		public void progress(int count);

		public void exceptionHappened(Exception ex);

	}

}
