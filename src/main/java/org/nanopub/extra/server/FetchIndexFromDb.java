package org.nanopub.extra.server;

import java.io.OutputStream;

import org.nanopub.MalformedNanopubException;
import org.nanopub.Nanopub;
import org.nanopub.NanopubUtils;
import org.nanopub.extra.index.IndexUtils;
import org.nanopub.extra.index.NanopubIndex;
import org.nanopub.extra.index.NanopubIndexImpl;
import org.openrdf.model.URI;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;

public class FetchIndexFromDb extends FetchIndex {

	public static final int maxParallelRequestsPerServer = 5;

	private String indexUri;
	private NanopubDb db;
	private OutputStream out;
	private RDFFormat format;
	private boolean writeIndex, writeContent;
	private int nanopubCount;
	private FetchIndex.Listener listener;

	public FetchIndexFromDb(String indexUri, NanopubDb db, OutputStream out, RDFFormat format, boolean writeIndex, boolean writeContent) {
		this.indexUri = indexUri;
		this.db = db;
		this.out = out;
		this.format = format;
		this.writeIndex = writeIndex;
		this.writeContent = writeContent;
		nanopubCount = 0;
	}

	public void run() {
		try {
			getIndex(indexUri);
		} catch (RDFHandlerException | MalformedNanopubException ex) {
			throw new RuntimeException(ex);
		}
	}

	private void getIndex(String indexUri) throws RDFHandlerException, MalformedNanopubException {
		Nanopub np = GetNanopub.get(indexUri, db);
		if (!IndexUtils.isIndex(np)) {
			throw new RuntimeException("NOT AN INDEX: " + np.getUri());
		}
		NanopubIndex npi = IndexUtils.castToIndex(np);
		if (writeIndex) {
			writeNanopub(npi);
		}
		if (writeContent) {
			for (URI elementUri : npi.getElements()) {
				writeNanopub(GetNanopub.get(elementUri.toString(), db));
			}
		}
		for (URI subIndexUri : npi.getSubIndexes()) {
			getIndex(subIndexUri.toString());
		}
		if (npi.getAppendedIndex() != null) {
			getIndex(npi.getAppendedIndex().toString());
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

}
