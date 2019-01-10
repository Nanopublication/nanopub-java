package org.nanopub.extra.security;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.xml.bind.DatatypeConverter;

import org.openrdf.model.Literal;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;

public class NanopubSignatureElement extends CryptoElement {

	public static final URI SIGNATURE_ELEMENT = new URIImpl("http://purl.org/nanopub/x/NanopubSignatureElement");
	public static final URI HAS_SIGNATURE_TARGET = new URIImpl("http://purl.org/nanopub/x/hasSignatureTarget");
	public static final URI HAS_SIGNATURE = new URIImpl("http://purl.org/nanopub/x/hasSignature");
	public static final URI SIGNED_BY = new URIImpl("http://purl.org/nanopub/x/signedBy");

	// Deprecated; used for legacy signatures
	public static final URI HAS_SIGNATURE_ELEMENT = new URIImpl("http://purl.org/nanopub/x/hasSignatureElement");

	private URI targetNanopubUri;
	private byte[] signature;
	private Set<URI> signers = new LinkedHashSet<>();
	private List<Statement> targetStatements = new ArrayList<>();

	NanopubSignatureElement(URI targetNanopubUri, URI uri) {
		super(uri);
		this.targetNanopubUri = targetNanopubUri;
	}

	public URI getTargetNanopubUri() {
		return targetNanopubUri;
	}

	void setSignatureLiteral(Literal signatureLiteral) throws MalformedCryptoElementException {
		if (signature != null) {
			throw new MalformedCryptoElementException("Two signatures found for signature element");
		}
		signature = DatatypeConverter.parseBase64Binary(signatureLiteral.getLabel());
	}

	public byte[] getSignature() {
		return signature;
	}

	void addSigner(URI signer) {
		signers.add(signer);
	}

	public Set<URI> getSigners() {
		return signers;
	}

	void addTargetStatement(Statement st) {
		targetStatements.add(st);
	}

	public List<Statement> getTargetStatements() {
		return targetStatements;
	}

}
