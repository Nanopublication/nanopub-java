package org.nanopub;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.helpers.RDFHandlerBase;

import static org.nanopub.Nanopub.*;

/**
 * Handles files or streams with a sequence of nanopubs.
 *
 * @author Tobias Kuhn
 */
public class MultiNanopubRdfHandler extends RDFHandlerBase {

	private NanopubHandler npHandler;

	private URI headUri = null;
	private boolean headComplete = false;
	private Map<URI,Boolean> graphs = new HashMap<>();
	private List<Statement> statements = new ArrayList<>();
	private List<String> nsPrefixes = new ArrayList<>();
	private Map<String,String> ns = new HashMap<>();

	public MultiNanopubRdfHandler(NanopubHandler npHandler) {
		this.npHandler = npHandler;
	}

	@Override
	public void handleStatement(Statement st) throws RDFHandlerException {
		if (!headComplete) {
			if (headUri == null) {
				headUri = (URI) st.getContext();
				graphs.put(headUri, true);
			} else if (headUri.equals(st.getContext())) {
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
			statements.add(st);
		}
	}

	@Override
	public void handleNamespace(String prefix, String uri) throws RDFHandlerException {
		nsPrefixes.remove(prefix);
		nsPrefixes.add(prefix);
		ns.put(prefix, uri);
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
