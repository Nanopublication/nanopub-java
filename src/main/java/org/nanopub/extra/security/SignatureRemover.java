package org.nanopub.extra.security;

import org.openrdf.model.Statement;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.RDFHandlerException;

public class SignatureRemover implements RDFHandler {

	private RDFHandler handler;

	public SignatureRemover(RDFHandler handler) {
		this.handler = handler;
	}

	@Override
	public void startRDF() throws RDFHandlerException {
		handler.startRDF();
	}

	@Override
	public void endRDF() throws RDFHandlerException {
		handler.endRDF();
	}

	@Override
	public void handleNamespace(String prefix, String uri) throws RDFHandlerException {
		handler.handleNamespace(prefix, uri);
	}

	@Override
	public void handleStatement(Statement st) throws RDFHandlerException {
		if (st.getPredicate().equals(NanopubSignature.HAS_SIGNATURE_ELEMENT_URI)) return;
		if (st.getPredicate().equals(NanopubSignature.HAS_PUBLIC_KEY)) return;
		if (st.getPredicate().equals(NanopubSignature.HAS_SIGNATURE)) return;
		if (st.getPredicate().equals(NanopubSignature.SIGNED_BY)) return;
		handler.handleStatement(st);
	}

	@Override
	public void handleComment(String comment) throws RDFHandlerException {
		handler.handleComment(comment);
	}

}
