package org.nanopub.extra.security;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

import java.io.Serializable;

/**
 * Abstract class representing a cryptographic element used in signatures.
 */
public abstract class CryptoElement implements Serializable {

    public static final IRI CRYPTO_ELEMENT = SimpleValueFactory.getInstance().createIRI("http://purl.org/nanopub/x/CryptoElement");
    public static final IRI HAS_ALGORITHM = SimpleValueFactory.getInstance().createIRI("http://purl.org/nanopub/x/hasAlgorithm");
    public static final IRI HAS_PUBLIC_KEY = SimpleValueFactory.getInstance().createIRI("http://purl.org/nanopub/x/hasPublicKey");

    protected IRI uri;
    protected String publicKeyString;
    protected SignatureAlgorithm algorithm;

    /**
     * Constructor for CryptoElement.
     *
     * @param uri the IRI of the crypto element
     */
    protected CryptoElement(IRI uri) {
        this.uri = uri;
    }

    /**
     * Returns the IRI of the crypto element.
     *
     * @return the IRI of the crypto element
     */
    public IRI getUri() {
        return uri;
    }

    /**
     * Sets the public key string for this crypto element.
     *
     * @param publicKeyLiteral the literal containing the public key string
     * @throws MalformedCryptoElementException if a public key is already set
     */
    public void setPublicKeyLiteral(Literal publicKeyLiteral) throws MalformedCryptoElementException {
        if (publicKeyString != null) {
            throw new MalformedCryptoElementException("Two public keys found for signature element");
        }
        publicKeyString = publicKeyLiteral.getLabel();
    }

    /**
     * Returns the public key string for this crypto element.
     *
     * @return the public key string
     */
    public String getPublicKeyString() {
        return publicKeyString;
    }

    /**
     * Sets the algorithm used for this crypto element.
     *
     * @param algorithm the signature algorithm to set
     * @throws MalformedCryptoElementException if an algorithm is already set
     */
    public void setAlgorithm(SignatureAlgorithm algorithm) throws MalformedCryptoElementException {
        if (this.algorithm != null) {
            throw new MalformedCryptoElementException("Two algorithms found for signature element");
        }
        this.algorithm = algorithm;
    }

    /**
     * Sets the algorithm used for this crypto element from a Literal.
     *
     * @param algorithmLiteral the literal containing the algorithm name
     * @throws MalformedCryptoElementException if an algorithm is already set or if the algorithm is not recognized
     */
    public void setAlgorithm(Literal algorithmLiteral) throws MalformedCryptoElementException {
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

    /**
     * Returns the algorithm used for this crypto element.
     *
     * @return the signature algorithm
     */
    public SignatureAlgorithm getAlgorithm() {
        return algorithm;
    }

}
