package org.nanopub.extra.security;

import java.io.Serializable;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

public abstract class CryptoElement implements Serializable {

	private static final long serialVersionUID = 1L;

	public static final IRI CRYPTO_ELEMENT = SimpleValueFactory.getInstance().createIRI("http://purl.org/nanopub/x/CryptoElement");
	public static final IRI HAS_ALGORITHM = SimpleValueFactory.getInstance().createIRI("http://purl.org/nanopub/x/hasAlgorithm");
	public static final IRI HAS_PUBLIC_KEY = SimpleValueFactory.getInstance().createIRI("http://purl.org/nanopub/x/hasPublicKey");

	protected IRI uri;
	protected String publicKeyString;
	protected SignatureAlgorithm algorithm;

	protected CryptoElement(IRI uri) {
		this.uri = uri;
	}

	public IRI getUri() {
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
