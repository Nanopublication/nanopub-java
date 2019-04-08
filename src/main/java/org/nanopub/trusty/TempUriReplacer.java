package org.nanopub.trusty;

import java.util.Map;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.rio.RDFHandler;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.nanopub.Nanopub;

/**
 * You can use temporary URIs for your nanopublications that start with
 * "http://purl.org/nanopub/temp/". These then become "http://purl.org/np/ARTIFACTCODE-PLACEHOLDER/"
 * before being transformed to trusty nanopublications, and as final trusty nanopublications have
 * the actual artifact code instead of the placeholder.
 *
 * @author Tobias Kuhn
 */
public class TempUriReplacer implements RDFHandler {

	public static final String tempUri = "http://purl.org/nanopub/temp/";
	public static final String normUri = "http://purl.org/np/ARTIFACTCODE-PLACEHOLDER/";

	private String uriPrefix;
	private RDFHandler nestedHandler;
	private Map<Resource,IRI> transformMap;

	public TempUriReplacer(Nanopub np, RDFHandler nestedHandler, Map<Resource,IRI> transformMap) {
		this.uriPrefix = np.getUri().stringValue();
		this.nestedHandler = nestedHandler;
		this.transformMap = transformMap;
	}

	public static boolean hasTempUri(Nanopub np) {
		return np.getUri().stringValue().startsWith(tempUri);
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
		if (uri.startsWith(uriPrefix)) {
			uri = uri.replace(uriPrefix, normUri);
		}
		nestedHandler.handleNamespace(prefix, uri);
	}

	private Value replace(Value v) {
		if (v instanceof IRI && v.stringValue().startsWith(uriPrefix)) {
			IRI i = SimpleValueFactory.getInstance().createIRI(v.stringValue().replace(uriPrefix, normUri));
			if (transformMap != null) transformMap.put((IRI) v, i);
			return i;
		} else {
			return v;
		}
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
