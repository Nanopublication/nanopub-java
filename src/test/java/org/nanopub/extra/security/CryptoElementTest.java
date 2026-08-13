package org.nanopub.extra.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.nanopub.utils.TestUtils.vf;

/**
 * Tests the shared behaviour of the crypto elements, through one of its concrete subclasses.
 */
class CryptoElementTest {

    private static CryptoElement element() {
        return new KeyDeclaration(vf.createIRI("https://example.org/key"));
    }

    @Test
    void getUri() {
        assertEquals("https://example.org/key", element().getUri().stringValue());
    }

    @Test
    void setPublicKeyLiteral() throws Exception {
        CryptoElement element = element();

        element.setPublicKeyLiteral(vf.createLiteral("a public key"));

        assertEquals("a public key", element.getPublicKeyString());
    }

    @Test
    void refusesASecondPublicKey() throws Exception {
        CryptoElement element = element();
        element.setPublicKeyLiteral(vf.createLiteral("a public key"));

        MalformedCryptoElementException ex = assertThrows(MalformedCryptoElementException.class,
                () -> element.setPublicKeyLiteral(vf.createLiteral("another public key")));
        assertEquals("Two public keys found for signature element", ex.getMessage());
    }

    @Test
    void setAlgorithmDirectly() throws Exception {
        CryptoElement element = element();

        element.setAlgorithm(SignatureAlgorithm.RSA);

        assertEquals(SignatureAlgorithm.RSA, element.getAlgorithm());
    }

    @Test
    void refusesASecondAlgorithm() throws Exception {
        CryptoElement element = element();
        element.setAlgorithm(SignatureAlgorithm.RSA);

        MalformedCryptoElementException ex = assertThrows(MalformedCryptoElementException.class,
                () -> element.setAlgorithm(SignatureAlgorithm.DSA));
        assertEquals("Two algorithms found for signature element", ex.getMessage());
    }

    @Test
    void setAlgorithmFromALiteral() throws Exception {
        CryptoElement rsa = element();
        rsa.setAlgorithm(vf.createLiteral("RSA"));
        assertEquals(SignatureAlgorithm.RSA, rsa.getAlgorithm());

        CryptoElement dsa = element();
        dsa.setAlgorithm(vf.createLiteral("DSA"));
        assertEquals(SignatureAlgorithm.DSA, dsa.getAlgorithm());
    }

    @Test
    void setAlgorithmFromALiteralIsCaseInsensitive() throws Exception {
        CryptoElement element = element();

        element.setAlgorithm(vf.createLiteral("rsa"));

        assertEquals(SignatureAlgorithm.RSA, element.getAlgorithm());
    }

    @Test
    void refusesAnUnknownAlgorithmLiteral() {
        CryptoElement element = element();

        MalformedCryptoElementException ex = assertThrows(MalformedCryptoElementException.class,
                () -> element.setAlgorithm(vf.createLiteral("magic")));
        assertEquals("Algorithm not recognized: magic", ex.getMessage());
    }

    @Test
    void refusesASecondAlgorithmLiteral() throws Exception {
        CryptoElement element = element();
        element.setAlgorithm(vf.createLiteral("RSA"));

        MalformedCryptoElementException ex = assertThrows(MalformedCryptoElementException.class,
                () -> element.setAlgorithm(vf.createLiteral("DSA")));
        assertEquals("Two algorithms found for signature element", ex.getMessage());
    }

    @Test
    void refusesASecondSignature() throws Exception {
        NanopubSignatureElement element = new NanopubSignatureElement(
                vf.createIRI("https://example.org/np1#"), vf.createIRI("https://example.org/np1#sig"));
        element.setSignatureLiteral(vf.createLiteral("AAAA"));

        assertNotNull(element.getSignature());
        MalformedCryptoElementException ex = assertThrows(MalformedCryptoElementException.class,
                () -> element.setSignatureLiteral(vf.createLiteral("BBBB")));
        assertEquals("Two signatures found for signature element", ex.getMessage());
    }

}
