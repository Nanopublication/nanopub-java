package org.nanopub.extra.security;

import org.openrdf.model.Literal;
import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;

public abstract class CryptoElement {

	public static final URI CRYPTO_ELEMENT = new URIImpl("http://purl.org/nanopub/x/CryptoElement");
	public static final URI HAS_ALGORITHM = new URIImpl("http://purl.org/nanopub/x/hasAlgorithm");
	public static final URI HAS_PUBLIC_KEY = new URIImpl("http://purl.org/nanopub/x/hasPublicKey");

	protected URI uri;
	protected String publicKeyString;
	protected SignatureAlgorithm algorithm;

	protected CryptoElement(URI uri) {
		this.uri = uri;
	}

	public URI getUri() {
		return uri;
	}

	void setPublicKeyLiteral(Literal publicKeyLiteral) throws MalformedCryptoElementException {
		if (publicKeyString != null) {
			throw new MalformedCryptoElementException("Two public keys found for signature element");
		}
		publicKeyString = publicKeyLiteral.getLabel();
	}

	public String getPublicKeyString() {
		return publicKeyString;
	}

	void setAlgorithm(SignatureAlgorithm algorithm) throws MalformedCryptoElementException {
		if (this.algorithm != null) {
			throw new MalformedCryptoElementException("Two algorithms found for signature element");
		}
		this.algorithm = algorithm;
	}

	void setAlgorithm(Literal algorithmLiteral) throws MalformedCryptoElementException {
		if (algorithm != null) {
			throw new MalformedCryptoElementException("Two algorithms found for signature element");
		}
		String alString = algorithmLiteral.getLabel().toUpperCase();
		for (SignatureAlgorithm al : SignatureAlgorithm.values()) {
			if (al.name().equals(alString)) {
				algorithm = al;
				break;
			}
		}
		if (algorithm == null) {
			throw new MalformedCryptoElementException("Algorithm not recognized: " + algorithmLiteral.getLabel());
		}
	}

	public SignatureAlgorithm getAlgorithm() {
		return algorithm;
	}

}
