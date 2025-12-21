package org.nanopub.extra.server;

import net.trustyuri.TrustyUriUtils;
import org.apache.http.conn.ConnectionPoolTimeoutException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.nanopub.ArtifactCode;
import org.nanopub.ArtifactCodeImpl;
import org.nanopub.Nanopub;
import org.nanopub.NanopubUtils;
import org.nanopub.extra.index.IndexUtils;
import org.nanopub.extra.index.NanopubIndex;
import org.nanopub.extra.server.RegistryInfo.RegistryInfoException;

import java.io.OutputStream;
import java.util.*;

/**
 * Fetches index.
 */
public class FetchIndex {

    /**
     * The maximum number of parallel requests that can be made to a single server.
     */
    public static final int maxParallelRequestsPerServer = 5;

    private OutputStream out;
    private RDFFormat format;
    private boolean writeIndex, writeContent;
    private boolean running = false;
    private List<FetchNanopubTask> fetchTasks;
    private List<RegistryInfo> registries;
    private RegistryInfo localRegistryInfo;
    private Map<RegistryInfo, Set<FetchNanopubTask>> serverLoad;
    private Map<RegistryInfo, Integer> serverUsage;
    private int nanopubCount;
    private Listener listener;

    /**
     * Default constructor for FetchIndex.
     */
    protected FetchIndex() {
    }

    /**
     * Creates a new FetchIndex instance.
     *
     * @param indexUri         the URI of the index to fetch
     * @param out              the output stream to write the fetched nanopubs to
     * @param format           the RDF format to use for writing nanopubs
     * @param writeIndex       true if the index nanopub should be written, false otherwise
     * @param writeContent     true if the content nanopubs should be written, false otherwise
     * @param localRegistryUrl the URL of a local registry to use, or null if not needed
     */
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
            serverLoad.put(registryInfo, new HashSet<>());
            serverUsage.put(registryInfo, 0);
        }
        try {
            ServerIterator.writeCachedServers(registries);
        } catch (Exception ex) {
        }
        if (localRegistryUrl != null) {
            try {
                localRegistryInfo = RegistryInfo.load(localRegistryUrl);
                registries.add(localRegistryInfo);
                serverLoad.put(localRegistryInfo, new HashSet<>());
                serverUsage.put(localRegistryInfo, 0);
            } catch (RegistryInfoException ex) {
                ex.printStackTrace();
                return;
            }
        }
        nanopubCount = 0;
    }

    /**
     * Starts the fetching process.
     */
    public void run() {
        synchronized (this) {
            if (running) return;
            running = true;
        }
        while (!fetchTasks.isEmpty()) {
            checkTasks();
            try {
                Thread.sleep(5);
            } catch (InterruptedException ex) {
            }
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
                            fetchTasks.addFirst(t1);
                            FetchNanopubTask t2 = new FetchNanopubTask(subIndexUri.toString(), true, t1);
                            fetchTasks.addFirst(t2);
                            FetchNanopubTask t3 = new FetchNanopubTask(subIndexUri.toString(), true, t1, t2);
                            fetchTasks.addFirst(t3);
                        }
                        if (npi.getAppendedIndex() != null) {
                            // Failing to get appended indexes can block the entire process, therefore
                            // we launch three sibling tasks at the same time:
                            FetchNanopubTask t1 = new FetchNanopubTask(npi.getAppendedIndex().toString(), true);
                            fetchTasks.addFirst(t1);
                            FetchNanopubTask t2 = new FetchNanopubTask(npi.getAppendedIndex().toString(), true, t1);
                            fetchTasks.addFirst(t2);
                            FetchNanopubTask t3 = new FetchNanopubTask(npi.getAppendedIndex().toString(), true, t1, t2);
                            fetchTasks.addFirst(t3);
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

    /**
     * Returns the number of nanopubs fetched so far.
     *
     * @return the number of nanopubs fetched so far
     */
    public int getNanopubCount() {
        return nanopubCount;
    }

    /**
     * Returns the list of registries that are being used.
     *
     * @return the list of registries
     */
    public List<RegistryInfo> getRegistries() {
        return new ArrayList<>(registries);
    }

    /**
     * Returns the number of times a server has been used to fetch a nanopub.
     *
     * @param r the registry info of the server
     * @return the number of times the server has been used
     */
    public int getServerUsage(RegistryInfo r) {
        return serverUsage.get(r);
    }

    /**
     * Sets a listener that will be notified about progress and exceptions.
     *
     * @param l the listener to set
     */
    public void setProgressListener(Listener l) {
        listener = l;
    }

    private void assignTask(final FetchNanopubTask task, final RegistryInfo r) {
        task.prepareForTryingServer(r);
        serverLoad.get(r).add(task);
        Runnable runFetchTask = () -> task.tryServer(r);
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

        /**
         * Creates a new task to fetch a nanopub.
         *
         * @param npUri    the URI of the nanopub to fetch
         * @param isIndex  true if the nanopub is an index, false otherwise
         * @param siblings other tasks that are siblings of this task (for sub-indexes)
         */
        public FetchNanopubTask(String npUri, boolean isIndex, FetchNanopubTask... siblings) {
            this.npUri = npUri;
            this.isIndex = isIndex;
            this.siblings = new HashSet<>(Arrays.asList(siblings));
            for (FetchNanopubTask s : siblings) {
                s.siblings.add(this);
            }
        }

        /**
         * Returns whether the nanopub is an index or not.
         *
         * @return true if the nanopub is an index, false otherwise
         */
        public boolean isIndex() {
            return isIndex;
        }

        /**
         * Returns the nanopub fetched by this task.
         *
         * @return the fetched nanopub, or null if not yet fetched
         */
        public Nanopub getNanopub() {
            return nanopub;
        }

        /**
         * Returns the URI of the nanopub to be fetched.
         *
         * @return the URI of the nanopub
         */
        public String getNanopubUri() {
            return npUri;
        }

        /**
         * Returns whether the task is currently running.
         *
         * @return true if the task is running, false otherwise
         */
        public boolean isRunning() {
            return running;
        }

        /**
         * Returns if the task has been cancelled.
         */
        public boolean isCancelled() {
            return cancelled;
        }

        /**
         * Returns whether the server has already been tried for this task.
         *
         * @param r the registry info of the server
         * @return true if the server has been tried, false otherwise
         */
        public boolean hasServerBeenTried(RegistryInfo r) {
            return registries.contains(r);
        }

        /**
         * Returns the number of servers that have been tried so far for this task.
         *
         * @return the number of tried servers
         */
        public int getTriedServersCount() {
            return registries.size();
        }

        /**
         * Returns the last registry that was used for this task.
         *
         * @return the last registry info, or null if no server has been tried yet
         */
        public RegistryInfo getLastRegistry() {
            return lastRegistry;
        }

        /**
         * Prepares the task for trying a server.
         *
         * @param r the registry info of the server to try
         */
        public void prepareForTryingServer(RegistryInfo r) {
            registries.add(r);
            lastRegistry = r;
            running = true;
        }

        /**
         * Attempts to fetch the nanopub from the specified server.
         *
         * @param registryInfo the registry info of the server to try
         */
        public void tryServer(RegistryInfo registryInfo) {
            ArtifactCode artifactCode = new ArtifactCodeImpl(TrustyUriUtils.getArtifactCode(npUri));
            boolean serverTried = false;
            try {
                serverTried = true;
                nanopub = GetNanopub.get(artifactCode, registryInfo);
            } catch (ConnectionPoolTimeoutException ex) {
                serverTried = false;
                // too many connection attempts; try again later
            } catch (Exception ex) {
                if (listener != null) listener.exceptionHappened(ex, registryInfo, artifactCode);
            } finally {
                running = false;
                if (serverTried) {
                    synchronized (FetchIndex.class) {
                        if (cancelled) {
                            // Sibling already did the work...
                        } else {
                            for (FetchNanopubTask s : siblings) {
                                s.cancelled = true;
                            }
                            serverUsage.put(registryInfo, serverUsage.get(registryInfo) + 1);
                        }
                    }
                }
            }
        }

    }


    /**
     * Listener interface for progress and exception handling.
     */
    public static interface Listener {

        /**
         * Called to report progress.
         *
         * @param count the number of nanopubs processed so far
         */
        public void progress(int count);

        /**
         * Called when an exception occurs during fetching.
         *
         * @param ex           the exception that occurred
         * @param r            the registry info of the server where the exception occurred
         * @param artifactCode the artifact code of the nanopub that caused the exception
         */
        public void exceptionHappened(Exception ex, RegistryInfo r, ArtifactCode artifactCode);

    }

}
