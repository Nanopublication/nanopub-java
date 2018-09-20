package org.nanopub.trusty;

import org.nanopub.Nanopub;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.ContextStatementImpl;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.RDFHandlerException;

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

	public TempUriReplacer(Nanopub np, RDFHandler nestedHandler) {
		uriPrefix = np.getUri().stringValue();
		this.nestedHandler = nestedHandler;
	}

	public static boolean hasTempUri(Nanopub np) {
		return np.getUri().stringValue().startsWith(tempUri);
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
		if (uri.startsWith(uriPrefix)) {
			uri = uri.replace(uriPrefix, normUri);
		}
		nestedHandler.handleNamespace(prefix, uri);
	}

	private Value replace(Value v) {
		if (v instanceof URI && v.stringValue().startsWith(uriPrefix)) {
			return new URIImpl(v.stringValue().replace(uriPrefix, normUri));
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
