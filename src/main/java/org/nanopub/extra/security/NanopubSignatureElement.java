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

// TODO: nanopub signatures are being updated...

// See: https://docs.oracle.com/javase/8/docs/technotes/guides/security/StandardNames.html#Signature

public class NanopubSignatureElement {

	// TODO Update public ontology file

	public static final URI SIGNATURE_ELEMENT = new URIImpl("http://purl.org/nanopub/x/NanopubSignatureElement");
	public static final URI HAS_SIGNATURE_TARGET = new URIImpl("http://purl.org/nanopub/x/hasSignatureTarget");
	public static final URI HAS_ALGORITHM = new URIImpl("http://purl.org/nanopub/x/hasAlgorithm");
	public static final URI HAS_PUBLIC_KEY = new URIImpl("http://purl.org/nanopub/x/hasPublicKey");
	public static final URI HAS_SIGNATURE = new URIImpl("http://purl.org/nanopub/x/hasSignature");
	public static final URI SIGNED_BY = new URIImpl("http://purl.org/nanopub/x/signedBy");

	// Will be deprecated
	public static final URI HAS_SIGNATURE_ELEMENT = new URIImpl("http://purl.org/nanopub/x/hasSignatureElement");

	private URI uri;
	private URI targetNanopubUri;
	private String publicKeyString;
	private SignatureAlgorithm algorithm;
	private byte[] signature;
	private Set<URI> signers = new LinkedHashSet<>();
	private List<Statement> targetStatements = new ArrayList<>();

	NanopubSignatureElement(URI targetNanopubUri, URI uri) {
		this.targetNanopubUri = targetNanopubUri;
		this.uri = uri;
	}

	public URI getUri() {
		return uri;
	}

	public URI getTargetNanopubUri() {
		return targetNanopubUri;
	}

	void setPublicKeyLiteral(Literal publicKeyLiteral) throws MalformedSignatureException {
		if (publicKeyString != null) {
			throw new MalformedSignatureException("Two public keys found for signature element");
		}
		publicKeyString = publicKeyLiteral.getLabel();
	}

	public String getPublicKeyString() {
		return publicKeyString;
	}

	void setSignatureLiteral(Literal signatureLiteral) throws MalformedSignatureException {
		if (signature != null) {
			throw new MalformedSignatureException("Two signatures found for signature element");
		}
		signature = DatatypeConverter.parseBase64Binary(signatureLiteral.getLabel());
	}

	public byte[] getSignature() {
		return signature;
	}

	void setAlgorithm(SignatureAlgorithm algorithm) throws MalformedSignatureException {
		if (this.algorithm != null) {
			throw new MalformedSignatureException("Two algorithms found for signature element");
		}
		this.algorithm = algorithm;
	}

	void setAlgorithm(Literal algorithmLiteral) throws MalformedSignatureException {
		if (algorithm != null) {
			throw new MalformedSignatureException("Two algorithms found for signature element");
		}
		String alString = algorithmLiteral.getLabel().toUpperCase();
		for (SignatureAlgorithm al : SignatureAlgorithm.values()) {
			if (al.name().equals(alString)) {
				algorithm = al;
				break;
			}
		}
		if (algorithm == null) {
			throw new MalformedSignatureException("Algorithm not recognized: " + algorithmLiteral.getLabel());
		}
	}

	public SignatureAlgorithm getAlgorithm() {
		return algorithm;
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
