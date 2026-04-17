package org.nanopub;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.junit.jupiter.api.Test;
import org.nanopub.extra.security.MalformedCryptoElementException;
import org.nanopub.extra.security.SignNanopub;
import org.nanopub.extra.security.TransformContext;
import org.nanopub.testsuite.NanopubTestSuite;
import org.nanopub.testsuite.SigningKeyPair;
import org.nanopub.vocabulary.NPX;

import java.io.File;
import java.io.IOException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.nanopub.extra.security.SignatureAlgorithm.RSA;

class NanopubRetractorTest {

    private final SimpleValueFactory vf = SimpleValueFactory.getInstance();

    @Test
    void testValidRetraction() throws Exception {
        Nanopub orig = loadNanopubToRetract();
        TransformContext tc = loadTransformationContext();

        Nanopub retraction = NanopubRetractor.createRetraction(orig, tc);

        Statement expectedRetractionStatement = vf.createStatement(tc.getSigner(), NPX.RETRACTS, orig.getUri(),
                vf.createIRI(retraction.getUri() + "/assertion"));

        assertTrue(retraction.getAssertion().contains(expectedRetractionStatement));
    }

    private Nanopub loadNanopubToRetract() throws MalformedNanopubException, IOException {
        File file = NanopubTestSuite.getLatest().getTransformCases("rsa-key2").getFirst().getSignedEntry().toFile();
        return new NanopubImpl(file);
    }

    private TransformContext loadTransformationContext() throws NoSuchAlgorithmException, IOException, InvalidKeySpecException {
        SigningKeyPair signingKeyPair = NanopubTestSuite.getLatest().getSigningKey("rsa-key2");
        KeyPair key = SignNanopub.loadKey(signingKeyPair.getPrivateKeyFile().getPath(), RSA);
        IRI signer = vf.createIRI("https://orcid.org/0000-0002-4808-1845");
        return new TransformContext(RSA, key, signer, false, false, false);
    }

    @Test
    void testWrongKey() throws Exception {
        Nanopub orig = loadNanopubToRetract();
        TransformContext tc = loadWrongTransformationContext();

        assertThrows(MalformedCryptoElementException.class, () -> NanopubRetractor.createRetraction(orig, tc));
    }

    private TransformContext loadWrongTransformationContext() throws NoSuchAlgorithmException, IOException, InvalidKeySpecException {
        SigningKeyPair signingKeyPair = NanopubTestSuite.getLatest().getSigningKey("rsa-key1");
        KeyPair key = SignNanopub.loadKey(signingKeyPair.getPrivateKeyFile().getPath(), RSA);
        IRI signer = vf.createIRI("https://orcid.org/0000-0002-4808-1845");
        return new TransformContext(RSA, key, signer, false, false, false);
    }

}