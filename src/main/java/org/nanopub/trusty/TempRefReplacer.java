package org.nanopub.trusty;

import java.util.Map;

import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.ContextStatementImpl;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.RDFHandlerException;

public class TempRefReplacer implements RDFHandler {

	private Map<String,String> tempRefMap;
	private RDFHandler nestedHandler;

	public TempRefReplacer(Map<String,String> tempRefMap, RDFHandler nestedHandler) {
		this.tempRefMap = tempRefMap;
		this.nestedHandler = nestedHandler;
	}

	@Override
	public void handleStatement(Statement st) throws RDFHandlerException {
		nestedHandler.handleStatement(new ContextStatementImpl(
				(Resource) replace(st.getSubject()),
				(URI) replace(st.getPredicate()),
				replace(st.getObject()),
				(Resource) replace(st.getContext())));
	}

	@Override
	public void handleNamespace(String prefix, String uri) throws RDFHandlerException {
		nestedHandler.handleNamespace(prefix, uri);
	}

	private Value replace(Value v) {
		String vs = v.stringValue();
		boolean matched = false;
		for (String k : tempRefMap.keySet()) {
			if (vs.startsWith(k)) {
				if (matched == true) {
					throw new RuntimeException("Matched to two temp URI prefixes: " + vs);
				}
				matched = true;
				return new URIImpl(vs.replace(k, tempRefMap.get(k)));
			}
		}
		if (vs.startsWith(TempUriReplacer.tempUri)) {
			throw new RuntimeException("Found unresolvable temp URI: " + vs);
		}
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
