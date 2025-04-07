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

import org.apache.http.conn.ConnectionPoolTimeoutException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.nanopub.Nanopub;
import org.nanopub.NanopubUtils;
import org.nanopub.extra.index.IndexUtils;
import org.nanopub.extra.index.NanopubIndex;
import org.nanopub.extra.server.RegistryInfo.RegistryInfoException;

import net.trustyuri.TrustyUriUtils;

public class FetchIndex {

	public static final int maxParallelRequestsPerServer = 5;

	private OutputStream out;
	private RDFFormat format;
	private boolean writeIndex, writeContent;
	private boolean running = false;
	private List<FetchNanopubTask> fetchTasks;
	private List<RegistryInfo> registries;
	private RegistryInfo localRegistryInfo;
	private Map<RegistryInfo,Set<FetchNanopubTask>> serverLoad;
	private Map<RegistryInfo,Integer> serverUsage;
	private int nanopubCount;
	private Listener listener;

	protected FetchIndex() {
	}

	public FetchIndex(String indexUri, OutputStream out, RDFFormat format, boolean writeIndex, boolean writeContent, String localRegistryUrl) {
		this.out = out;
		this.format = format;
		this.writeIndex = writeIndex;
		this.writeContent = writeContent;
		fetchTasks = new ArrayList<>();
		fetchTasks.add(new FetchNanopubTask(indexUri, true));
		registries = new ArrayList<>();
		serverLoad = new HashMap<>();
		serverUsage = new HashMap<>();
		ServerIterator serverIterator = new ServerIterator();
		while (serverIterator.hasNext()) {
			RegistryInfo registryInfo = serverIterator.next();
			registries.add(registryInfo);
			serverLoad.put(registryInfo, new HashSet<FetchNanopubTask>());
			serverUsage.put(registryInfo, 0);
		}
		try {
			ServerIterator.writeCachedServers(registries);
		} catch (Exception ex) {}
		if (localRegistryUrl != null) {
			try {
				localRegistryInfo = RegistryInfo.load(localRegistryUrl);
				registries.add(localRegistryInfo);
				serverLoad.put(localRegistryInfo, new HashSet<FetchNanopubTask>());
				serverUsage.put(localRegistryInfo, 0);
			} catch (RegistryInfoException ex) {
				ex.printStackTrace();
				return;
			}
		}
		nanopubCount = 0;
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
				serverLoad.get(task.getLastRegistry()).remove(task);
				continue;
			}
			if (task.getLastRegistry() != null) {
				serverLoad.get(task.getLastRegistry()).remove(task);
			}
			if (task.getNanopub() == null) {
				if (task.getTriedServersCount() == registries.size()) {
					System.err.println("Failed to get " + task.getNanopubUri());
					fetchTasks.remove(task);
					continue;
				}
				if (localRegistryInfo != null && !task.hasServerBeenTried(localRegistryInfo)) {
					assignTask(task, localRegistryInfo);
					break;
				}
				List<RegistryInfo> shuffledServers = new ArrayList<>(registries);
				Collections.shuffle(shuffledServers);
				for (RegistryInfo registryInfo : shuffledServers) {
					if (task.hasServerBeenTried(registryInfo)) continue;
					int load = serverLoad.get(registryInfo).size();
					if (load >= maxParallelRequestsPerServer) {
						continue;
					}
					assignTask(task, registryInfo);
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

	public List<RegistryInfo> getRegistries() {
		return new ArrayList<>(registries);
	}

	public int getServerUsage(RegistryInfo r) {
		return serverUsage.get(r);
	}

	public void setProgressListener(Listener l) {
		listener = l;
	}

	private void assignTask(final FetchNanopubTask task, final RegistryInfo r) {
		task.prepareForTryingServer(r);
		serverLoad.get(r).add(task);
		Runnable runFetchTask = new Runnable() {

			@Override
			public void run() {
				task.tryServer(r);
			}

		};
		Thread thread = new Thread(runFetchTask);
		thread.start();
	}

	private class FetchNanopubTask {

		private String npUri;
		private boolean isIndex;
		private Nanopub nanopub;
		private Set<RegistryInfo> registries = new HashSet<>();
		private boolean running = false;
		private boolean cancelled = false;
		private RegistryInfo lastRegistry;
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

		public boolean hasServerBeenTried(RegistryInfo r) {
			return registries.contains(r);
		}

		public int getTriedServersCount() {
			return registries.size();
		}

		public RegistryInfo getLastRegistry() {
			return lastRegistry;
		}

		public void ignoreServer(RegistryInfo r) {
			registries.add(r);
		}

		public void prepareForTryingServer(RegistryInfo r) {
			registries.add(r);
			lastRegistry = r;
			running = true;
		}

		public void tryServer(RegistryInfo r) {
			boolean serverTried = false;
			try {
				serverTried = true;
				nanopub = GetNanopub.get(TrustyUriUtils.getArtifactCode(npUri), r);
			} catch (ConnectionPoolTimeoutException ex) {
				serverTried = false;
				// too many connection attempts; try again later
			} catch (Exception ex) {
				if (listener != null) listener.exceptionHappened(ex, r, TrustyUriUtils.getArtifactCode(npUri));
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
							serverUsage.put(r, serverUsage.get(r) + 1);
						}
					}
				}
			}
		}

	}


	public static interface Listener {

		public void progress(int count);

		public void exceptionHappened(Exception ex, RegistryInfo r, String artifactCode);

	}

}
