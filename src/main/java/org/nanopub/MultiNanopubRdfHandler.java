package org.nanopub;

import static org.nanopub.Nanopub.HAS_ASSERTION_URI;
import static org.nanopub.Nanopub.HAS_PROVENANCE_URI;
import static org.nanopub.Nanopub.HAS_PUBINFO_URI;
import static org.nanopub.Nanopub.SUB_GRAPH_OF;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.helpers.RDFHandlerBase;

/**
 * Handles files or streams with a sequence of nanopubs.
 *
 * @author Tobias Kuhn
 */
public class MultiNanopubRdfHandler extends RDFHandlerBase {

	public static void process(RDFFormat format, InputStream in, NanopubHandler npHandler)
			throws IOException, RDFParseException, RDFHandlerException {
		RDFParser p = NanopubUtils.getParser(format);
		p.setRDFHandler(new MultiNanopubRdfHandler(npHandler));
		try {
			p.parse(in, "");
		} finally {
			in.close();
		}
	}

	public static void process(RDFFormat format, File file, NanopubHandler npHandler)
			throws IOException, RDFParseException, RDFHandlerException {
		InputStream in = new BufferedInputStream(new FileInputStream(file));
		process(format, in, npHandler);
	}

	private NanopubHandler npHandler;

	private URI headUri = null;
	private boolean headComplete = false;
	private Map<URI,Boolean> graphs = new HashMap<>();
	private List<Statement> statements = new ArrayList<>();
	private List<String> nsPrefixes = new ArrayList<>();
	private Map<String,String> ns = new HashMap<>();

	private List<String> newNsPrefixes = new ArrayList<>();
	private List<String> newNs = new ArrayList<>();

	public MultiNanopubRdfHandler(NanopubHandler npHandler) {
		this.npHandler = npHandler;
	}

	@Override
	public void handleStatement(Statement st) throws RDFHandlerException {
		if (!headComplete) {
			if (headUri == null) {
				headUri = (URI) st.getContext();
				graphs.put(headUri, true);
			}
			if (headUri.equals(st.getContext())) {
				URI p = st.getPredicate();
				if (p.equals(HAS_ASSERTION_URI) || p.equals(HAS_PROVENANCE_URI) || p.equals(HAS_PUBINFO_URI)) {
					graphs.put((URI) st.getObject(), true);
				} else if (p.equals(SUB_GRAPH_OF)) {
					graphs.put((URI) st.getSubject(), true);
				}
			} else {
				headComplete = true;
			}
		}
		if (headComplete && !graphs.containsKey(st.getContext())) {
			finishNanopub();
			handleStatement(st);
		} else {
			addNamespaces();
			statements.add(st);
		}
	}

	@Override
	public void handleNamespace(String prefix, String uri) throws RDFHandlerException {
		newNs.add(uri);
		newNsPrefixes.add(prefix);
	}

	public void addNamespaces() throws RDFHandlerException {
		for (int i = 0 ; i < newNs.size() ; i++) {
			String prefix = newNsPrefixes.get(i);
			String nsUri = newNs.get(i);
			nsPrefixes.remove(prefix);
			nsPrefixes.add(prefix);
			ns.put(prefix, nsUri);
		}
		newNs.clear();
		newNsPrefixes.clear();
	}

	@Override
	public void endRDF() throws RDFHandlerException {
		finishNanopub();
	}

	private void finishNanopub() {
		try {
			npHandler.handleNanopub(new NanopubImpl(statements, nsPrefixes, ns));
		} catch (MalformedNanopubException ex) {
			throw new RuntimeException(ex);
		}
		headUri = null;
		headComplete = false;
		graphs.clear();
		statements.clear();
	}


	public interface NanopubHandler {

		public void handleNanopub(Nanopub np);

	}

}
