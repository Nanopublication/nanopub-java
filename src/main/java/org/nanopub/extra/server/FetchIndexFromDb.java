package org.nanopub.extra.server;

import java.io.OutputStream;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.nanopub.MalformedNanopubException;
import org.nanopub.Nanopub;
import org.nanopub.NanopubUtils;
import org.nanopub.extra.index.IndexUtils;
import org.nanopub.extra.index.NanopubIndex;

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

	public int getNanopubCount() {
		return nanopubCount;
	}

	public void setProgressListener(Listener l) {
		listener = l;
	}

}
