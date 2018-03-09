package org.nanopub.extra.security;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.KeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.DatatypeConverter;

import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.RDFHandlerException;

import com.google.common.collect.ImmutableList;

// TODO: nanopub signatures are being updated...

public class SignatureRemover implements RDFHandler {

	private RDFHandler handler;
	private URI graph;

	private static KeyFactory kf;

	private List<Resource> signatureElements = new ArrayList<>();
	private Map<Resource,byte[]> signatures = new HashMap<>();
	private Map<Resource,PublicKey> publicKeys = new HashMap<>();

	public SignatureRemover(RDFHandler handler, URI graph) {
		this.handler = handler;
		this.graph = graph;

		try {
			kf = KeyFactory.getInstance("DSA");
		} catch (NoSuchAlgorithmException ex) {
			ex.printStackTrace();
			throw new RuntimeException(ex);
		}
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
		if (st.getContext() != null && st.getContext().equals(graph)) {
			if (st.getPredicate().equals(NanopubSignatureElement.HAS_SIGNATURE_ELEMENT)) {
				if (!(st.getObject() instanceof Resource)) {
					throw new RDFHandlerException("Object of " + NanopubSignatureElement.HAS_SIGNATURE_ELEMENT + " has to be a resource");
				}
				Resource sigElement = (Resource) st.getObject();
				if (signatureElements.contains(sigElement)) {
					throw new RDFHandlerException("Signature element introduced twice");
				}
				signatureElements.add(sigElement);
				return;
			}
			if (st.getPredicate().equals(NanopubSignatureElement.HAS_PUBLIC_KEY)) {
				if (publicKeys.get(st.getSubject()) != null) {
					throw new RDFHandlerException("Two public keys found");
				}
				if (!(st.getObject() instanceof Literal)) {
					throw new RDFHandlerException("Object of " + NanopubSignatureElement.HAS_PUBLIC_KEY + " has to be a literal");
				}
				try {
					byte[] publicKeyBytes = DatatypeConverter.parseBase64Binary(((Literal) st.getObject()).getLabel());
					KeySpec publicSpec = new X509EncodedKeySpec(publicKeyBytes);
					publicKeys.put(st.getSubject(), kf.generatePublic(publicSpec));
				} catch (Exception ex) {
					ex.printStackTrace();
					throw new RuntimeException(ex);
				}
				return;
			}
			if (st.getPredicate().equals(NanopubSignatureElement.HAS_SIGNATURE)) {
				if (signatures.get(st.getSubject()) != null) {
					throw new RDFHandlerException("Two signatures found");
				}
				if (!(st.getObject() instanceof Literal)) {
					throw new RDFHandlerException("Object of " + NanopubSignatureElement.HAS_SIGNATURE + " has to be a literal");
				}
				try {
					byte[] signature = DatatypeConverter.parseBase64Binary(((Literal) st.getObject()).getLabel());
					signatures.put(st.getSubject(), signature);
				} catch (Exception ex) {
					ex.printStackTrace();
					throw new RuntimeException(ex);
				}
				return;
			}
			if (st.getPredicate().equals(NanopubSignatureElement.SIGNED_BY)) return;
		}
		handler.handleStatement(st);
	}

	@Override
	public void handleComment(String comment) throws RDFHandlerException {
		handler.handleComment(comment);
	}

	public List<Resource> getSignatureElements() {
		return ImmutableList.copyOf(signatureElements);
	}

	public byte[] getSignature(Resource signatureElement) {
		return signatures.get(signatureElement);
	}

	public PublicKey getPublicKey(Resource signatureElement) {
		return publicKeys.get(signatureElement);
	}

}
