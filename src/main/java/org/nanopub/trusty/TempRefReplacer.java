package org.nanopub.trusty;

import java.util.Map;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.rio.RDFHandler;
import org.eclipse.rdf4j.rio.RDFHandlerException;

import net.trustyuri.TrustyUriUtils;
import net.trustyuri.rdf.RdfUtils;

public class TempRefReplacer implements RDFHandler {

	private Map<String,String> tempRefMap;
	private RDFHandler nestedHandler;

	public TempRefReplacer(Map<String,String> tempRefMap, RDFHandler nestedHandler) {
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
		String vs = v.stringValue();
		boolean matched = false;
		for (String k : tempRefMap.keySet()) {
			if (vs.startsWith(k)) {
				if (matched == true) {
					throw new RuntimeException("Matched to two temp URI prefixes: " + vs);
				}
				matched = true;
				String trustyUriString = tempRefMap.get(k);
				String artifactCode = TrustyUriUtils.getArtifactCode(trustyUriString);
				String baseUriString = trustyUriString.split(artifactCode)[0];
				String suffix = vs.replace(k, "");
				if (suffix.isEmpty()) suffix = null;
				String uriString = RdfUtils.getTrustyUriString(SimpleValueFactory.getInstance().createIRI(baseUriString), artifactCode, suffix);
				return SimpleValueFactory.getInstance().createIRI(uriString);
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
