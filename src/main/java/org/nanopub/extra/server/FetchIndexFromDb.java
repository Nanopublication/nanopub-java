package org.nanopub.extra.server;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.nanopub.MalformedNanopubException;
import org.nanopub.Nanopub;
import org.nanopub.NanopubUtils;
import org.nanopub.extra.index.IndexUtils;
import org.nanopub.extra.index.NanopubIndex;

import java.io.OutputStream;

/**
 * Fetches a nanopub index from a database.
 */
public class FetchIndexFromDb extends FetchIndex {

    /**
     * Max parallel requests.
     */
    public static final int maxParallelRequestsPerServer = 5;

    private String indexUri;
    private NanopubDb db;
    private OutputStream out;
    private RDFFormat format;
    private boolean writeIndex, writeContent;
    private int nanopubCount;
    private FetchIndex.Listener listener;

    /**
     * Constructor for fetching an index from a database.
     *
     * @param indexUri     the URI of the index to fetch
     * @param db           the database to fetch from
     * @param out          the output stream to write the nanopubs to
     * @param format       the RDF format to use for writing
     * @param writeIndex   whether to write the index nanopub itself
     * @param writeContent whether to write the content nanopubs referenced by the index
     */
    public FetchIndexFromDb(String indexUri, NanopubDb db, OutputStream out, RDFFormat format, boolean writeIndex, boolean writeContent) {
        this.indexUri = indexUri;
        this.db = db;
        this.out = out;
        this.format = format;
        this.writeIndex = writeIndex;
        this.writeContent = writeContent;
        nanopubCount = 0;
    }

    /**
     * Fetches the index and its content from the database.
     */
    public void run() {
        try {
            getIndex(indexUri);
        } catch (RDFHandlerException | MalformedNanopubException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void getIndex(String indexUri) throws RDFHandlerException, MalformedNanopubException {
        NanopubIndex npi = getIndex(indexUri, db);
        while (npi != null) {
            if (writeIndex) {
                writeNanopub(npi);
            }
            if (writeContent) {
                for (IRI elementUri : npi.getElements()) {
                    writeNanopub(GetNanopub.get(elementUri.toString(), db));
                }
            }
            for (IRI subIndexUri : npi.getSubIndexes()) {
                getIndex(subIndexUri.toString());
            }
            if (npi.getAppendedIndex() != null) {
                npi = getIndex(npi.getAppendedIndex().toString(), db);
            } else {
                npi = null;
            }
        }
    }

    private static NanopubIndex getIndex(String indexUri, NanopubDb db) throws MalformedNanopubException {
        Nanopub np = GetNanopub.get(indexUri, db);
        if (!IndexUtils.isIndex(np)) {
            throw new RuntimeException("NOT AN INDEX: " + np.getUri());
        }
        return IndexUtils.castToIndex(np);
    }

    private void writeNanopub(Nanopub np) throws RDFHandlerException {
        nanopubCount++;
        if (listener != null && nanopubCount % 100 == 0) {
            listener.progress(nanopubCount);
        }
        NanopubUtils.writeToStream(np, out, format);
    }

    /**
     * Returns the count of the nanopubs fetched.
     *
     * @return the number of nanopubs fetched
     */
    public int getNanopubCount() {
        return nanopubCount;
    }

    /**
     * Sets a listener to be notified of progress.
     *
     * @param l the listener to set
     */
    public void setProgressListener(Listener l) {
        listener = l;
    }

}
