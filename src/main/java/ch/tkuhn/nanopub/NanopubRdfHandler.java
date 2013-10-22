package ch.tkuhn.nanopub;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openrdf.model.Statement;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.RDFHandlerException;

public class NanopubRdfHandler implements RDFHandler {

	private List<Statement> statements = new ArrayList<>();
	private List<String> nsPrefixes = new ArrayList<>();
	private Map<String,String> ns = new HashMap<>();

	private boolean finished = false;

	@Override
	public void startRDF() throws RDFHandlerException {}

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

	@Override
	public void handleComment(String comment) throws RDFHandlerException {}

	public Nanopub getNanopub() throws MalformedNanopubException {
		if (!finished) {
			throw new RuntimeException("No complete RDF document received");
		}
		return new NanopubImpl(statements, nsPrefixes, ns);
	}

}
