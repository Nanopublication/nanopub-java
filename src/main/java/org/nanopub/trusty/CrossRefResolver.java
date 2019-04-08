package org.nanopub.trusty;

import java.util.Map;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.rio.RDFHandler;
import org.eclipse.rdf4j.rio.RDFHandlerException;

public class CrossRefResolver implements RDFHandler {

	private Map<Resource,IRI> tempRefMap;
	private RDFHandler nestedHandler;

	public CrossRefResolver(Map<Resource,IRI> tempRefMap, RDFHandler nestedHandler) {
		this.tempRefMap = tempRefMap;
		this.nestedHandler = nestedHandler;
	}

	@Override
	public void handleStatement(Statement st) throws RDFHandlerException {
		nestedHandler.handleStatement(SimpleValueFactory.getInstance().createStatement(
				(Resource) replace(st.getSubject()),
				(IRI) replace(st.getPredicate()),
				replace(st.getObject()),
				(Resource) replace(st.getContext())));
	}

	@Override
	public void handleNamespace(String prefix, String uri) throws RDFHandlerException {
		String transformedUri = replace(SimpleValueFactory.getInstance().createIRI(uri)).stringValue();
		nestedHandler.handleNamespace(prefix, transformedUri);
	}

	private Value replace(Value v) {
		if (!(v instanceof Resource)) return v;
		IRI i = tempRefMap.get(v);
		if (i != null) return i;
		return v;
	}

	@Override
	public void startRDF() throws RDFHandlerException {
		nestedHandler.startRDF();
	}

	@Override
	public void endRDF() throws RDFHandlerException {
		nestedHandler.endRDF();
	}

	@Override
	public void handleComment(String comment) throws RDFHandlerException {
		nestedHandler.handleComment(comment);
	}

}
