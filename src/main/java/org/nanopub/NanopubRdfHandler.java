package org.nanopub;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.helpers.AbstractRDFHandler;

/**
 * @author Tobias Kuhn
 */
public class NanopubRdfHandler extends AbstractRDFHandler {

	private List<Statement> statements = new ArrayList<>();
	private List<String> nsPrefixes = new ArrayList<>();
	private Map<String,String> ns = new HashMap<>();

	private boolean finished = false;

	@Override
	public void endRDF() throws RDFHandlerException {
		finished = true;
	}

	@Override
	public void handleNamespace(String prefix, String uri) throws RDFHandlerException {
		nsPrefixes.add(prefix);
		ns.put(prefix, uri);
	}

	@Override
	public void handleStatement(Statement st) throws RDFHandlerException {
		statements.add(st);
	}

	public Nanopub getNanopub() throws MalformedNanopubException {
		if (!finished) {
			throw new RuntimeException("No complete RDF document received");
		}
		return new NanopubImpl(statements, nsPrefixes, ns);
	}

}
