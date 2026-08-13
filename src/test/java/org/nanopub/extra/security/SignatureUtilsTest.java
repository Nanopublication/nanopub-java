package org.nanopub.extra.security;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.junit.jupiter.api.Test;
import org.nanopub.Nanopub;
import org.nanopub.NanopubCreator;
import org.nanopub.testsuite.NanopubTestSuite;
import org.nanopub.testsuite.SigningKeyPair;
import org.nanopub.vocabulary.NPX;

import java.security.KeyPair;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.nanopub.utils.TestUtils.anyIri;
import static org.nanopub.utils.TestUtils.vf;

class SignatureUtilsTest {

    private final String pathSuffix = ".nanopub/";
    private final String fileNamePrefix = "id";

    // TODO
//    @Test
//    void getFullFilePathFromHomePath() {
//        String pathPrefix = "~/";
//        String relativeFilePathAndFileNamePrefix = pathPrefix + pathSuffix + fileNamePrefix;
//        String fullFilePath = SignatureUtils.getFullFilePath(relativeFilePathAndFileNamePrefix);
//        assertEquals((System.getProperty("user.home") + "/" + pathSuffix), fullFilePath);
//    }

    @Test
    void getFullFilePathWithFullPath() {
        String pathPrefix = "/home/user/";
        String fullFilePath = pathPrefix + pathSuffix + fileNamePrefix;
        String result = SignatureUtils.getFullFilePath(fullFilePath);
        assertEquals(fullFilePath, result);
    }

    @Test
    void getFullFilePathExpandsTheHomeDirectory() {
        String expanded = SignatureUtils.getFullFilePath("~/" + pathSuffix + fileNamePrefix);

        assertTrue(expanded.startsWith(System.getProperty("user.home")), expanded);
        assertTrue(expanded.endsWith(pathSuffix + fileNamePrefix), expanded);
    }

    private static final IRI NP_URI = vf.createIRI("https://example.org/np1#");
    private static final IRI SIG = vf.createIRI("https://example.org/np1#sig");

    /**
     * A nanopub whose pubinfo carries exactly the given statements. Building these by hand keeps the
     * malformed cases reachable, which a well-formed nanopub could not express.
     */
    private static Nanopub nanopubWithPubinfo(Statement... pubinfo) {
        Nanopub nanopub = mock(Nanopub.class);
        when(nanopub.getUri()).thenReturn(NP_URI);
        when(nanopub.getHead()).thenReturn(Set.of());
        when(nanopub.getAssertion()).thenReturn(Set.of());
        when(nanopub.getProvenance()).thenReturn(Set.of());
        when(nanopub.getPubinfo()).thenReturn(new LinkedHashSet<>(List.of(pubinfo)));
        return nanopub;
    }

    private static Statement signatureTarget() {
        return vf.createStatement(SIG, NPX.HAS_SIGNATURE_TARGET, NP_URI);
    }

    // ----------------------------------------------------- seemsToHaveSignature

    @Test
    void seemsToHaveSignatureRecognisesEverySignaturePredicate() {
        assertTrue(SignatureUtils.seemsToHaveSignature(
                nanopubWithPubinfo(vf.createStatement(NP_URI, NPX.HAS_SIGNATURE_ELEMENT, SIG))));
        assertTrue(SignatureUtils.seemsToHaveSignature(nanopubWithPubinfo(signatureTarget())));
        assertTrue(SignatureUtils.seemsToHaveSignature(
                nanopubWithPubinfo(vf.createStatement(SIG, NPX.HAS_SIGNATURE, vf.createLiteral("AAAA")))));
        assertTrue(SignatureUtils.seemsToHaveSignature(
                nanopubWithPubinfo(vf.createStatement(SIG, NPX.HAS_PUBLIC_KEY, vf.createLiteral("a key")))));
    }

    @Test
    void seemsToHaveSignatureIsFalseWithoutOne() {
        assertFalse(SignatureUtils.seemsToHaveSignature(
                nanopubWithPubinfo(vf.createStatement(NP_URI, RDFS.LABEL, vf.createLiteral("a label")))));
    }

    // ------------------------------------------------------ getSignatureElement

    @Test
    void getSignatureElementWithoutOneIsNull() throws Exception {
        assertNull(SignatureUtils.getSignatureElement(
                nanopubWithPubinfo(vf.createStatement(NP_URI, RDFS.LABEL, vf.createLiteral("a label")))));
    }

    @Test
    void getSignatureElementIgnoresTargetsOfOtherNanopubs() throws Exception {
        assertNull(SignatureUtils.getSignatureElement(nanopubWithPubinfo(
                vf.createStatement(SIG, NPX.HAS_SIGNATURE_TARGET, vf.createIRI("https://example.org/other")))));
    }

    @Test
    void getSignatureElementRefusesANonUriSignatureElement() {
        Nanopub nanopub = nanopubWithPubinfo(
                vf.createStatement(vf.createBNode(), NPX.HAS_SIGNATURE_TARGET, NP_URI));

        MalformedCryptoElementException ex = assertThrows(MalformedCryptoElementException.class,
                () -> SignatureUtils.getSignatureElement(nanopub));
        assertEquals("Signature element must be identified by URI", ex.getMessage());
    }

    @Test
    void getSignatureElementRefusesMultipleSignatureElements() {
        Nanopub nanopub = nanopubWithPubinfo(signatureTarget(),
                vf.createStatement(vf.createIRI("https://example.org/np1#sig2"), NPX.HAS_SIGNATURE_TARGET, NP_URI));

        MalformedCryptoElementException ex = assertThrows(MalformedCryptoElementException.class,
                () -> SignatureUtils.getSignatureElement(nanopub));
        assertEquals("Multiple signature elements found", ex.getMessage());
    }

    @Test
    void getSignatureElementRefusesANonLiteralSignature() {
        Nanopub nanopub = nanopubWithPubinfo(signatureTarget(),
                vf.createStatement(SIG, NPX.HAS_SIGNATURE, NP_URI));

        MalformedCryptoElementException ex = assertThrows(MalformedCryptoElementException.class,
                () -> SignatureUtils.getSignatureElement(nanopub));
        assertTrue(ex.getMessage().startsWith("Literal expected as signature: "), ex.getMessage());
    }

    @Test
    void getSignatureElementRefusesANonLiteralPublicKey() {
        Nanopub nanopub = nanopubWithPubinfo(signatureTarget(),
                vf.createStatement(SIG, NPX.HAS_PUBLIC_KEY, NP_URI));

        MalformedCryptoElementException ex = assertThrows(MalformedCryptoElementException.class,
                () -> SignatureUtils.getSignatureElement(nanopub));
        assertTrue(ex.getMessage().startsWith("Literal expected as public key: "), ex.getMessage());
    }

    @Test
    void getSignatureElementRefusesANonLiteralAlgorithm() {
        Nanopub nanopub = nanopubWithPubinfo(signatureTarget(),
                vf.createStatement(SIG, NPX.HAS_ALGORITHM, NP_URI));

        MalformedCryptoElementException ex = assertThrows(MalformedCryptoElementException.class,
                () -> SignatureUtils.getSignatureElement(nanopub));
        assertTrue(ex.getMessage().startsWith("Literal expected as algorithm: "), ex.getMessage());
    }

    @Test
    void getSignatureElementRefusesANonUriSigner() {
        Nanopub nanopub = nanopubWithPubinfo(signatureTarget(),
                vf.createStatement(SIG, NPX.SIGNED_BY, vf.createLiteral("not a URI")));

        MalformedCryptoElementException ex = assertThrows(MalformedCryptoElementException.class,
                () -> SignatureUtils.getSignatureElement(nanopub));
        assertTrue(ex.getMessage().startsWith("URI expected as signer: "), ex.getMessage());
    }

    @Test
    void getSignatureElementRefusesAMissingSignature() {
        Nanopub nanopub = nanopubWithPubinfo(signatureTarget(),
                vf.createStatement(SIG, NPX.HAS_PUBLIC_KEY, vf.createLiteral("a key")));

        MalformedCryptoElementException ex = assertThrows(MalformedCryptoElementException.class,
                () -> SignatureUtils.getSignatureElement(nanopub));
        assertEquals("Signature element without signature", ex.getMessage());
    }

    @Test
    void getSignatureElementRefusesAMissingAlgorithm() {
        Nanopub nanopub = nanopubWithPubinfo(signatureTarget(),
                vf.createStatement(SIG, NPX.HAS_SIGNATURE, vf.createLiteral("AAAA")),
                vf.createStatement(SIG, NPX.HAS_PUBLIC_KEY, vf.createLiteral("a key")));

        MalformedCryptoElementException ex = assertThrows(MalformedCryptoElementException.class,
                () -> SignatureUtils.getSignatureElement(nanopub));
        assertEquals("Signature element without algorithm", ex.getMessage());
    }

    @Test
    void getSignatureElementRefusesAMissingPublicKey() {
        Nanopub nanopub = nanopubWithPubinfo(signatureTarget(),
                vf.createStatement(SIG, NPX.HAS_SIGNATURE, vf.createLiteral("AAAA")),
                vf.createStatement(SIG, NPX.HAS_ALGORITHM, vf.createLiteral("RSA")));

        MalformedCryptoElementException ex = assertThrows(MalformedCryptoElementException.class,
                () -> SignatureUtils.getSignatureElement(nanopub));
        assertEquals("Signature element without public key", ex.getMessage());
    }

    @Test
    void getSignatureElementReadsAWellFormedElement() throws Exception {
        IRI signer = vf.createIRI("https://orcid.org/0000-0000-0000-0001");
        Nanopub nanopub = nanopubWithPubinfo(signatureTarget(),
                vf.createStatement(SIG, NPX.HAS_SIGNATURE, vf.createLiteral("AAAA")),
                vf.createStatement(SIG, NPX.HAS_ALGORITHM, vf.createLiteral("RSA")),
                vf.createStatement(SIG, NPX.HAS_PUBLIC_KEY, vf.createLiteral("a key")),
                vf.createStatement(SIG, NPX.SIGNED_BY, signer),
                // not part of the signature vocabulary, and about something else entirely
                vf.createStatement(NP_URI, RDFS.LABEL, vf.createLiteral("a label")));

        NanopubSignatureElement element = SignatureUtils.getSignatureElement(nanopub);

        assertNotNull(element);
        assertEquals(SignatureAlgorithm.RSA, element.getAlgorithm());
        assertEquals("a key", element.getPublicKeyString());
        assertEquals(Set.of(signer), element.getSigners());
        // everything except the signature itself is signed over
        assertEquals(5, element.getTargetStatements().size());
    }

    // ------------------------------------------------------------- public keys

    private static Nanopub signedNanopub() throws Exception {
        NanopubCreator creator = new NanopubCreator(true);
        creator.addAssertionStatement(anyIri, RDFS.LABEL, vf.createLiteral("an assertion"));
        creator.addProvenanceStatement(anyIri, anyIri);
        creator.addPubinfoStatement(anyIri, anyIri);
        return SignNanopub.signAndTransform(creator.finalizeNanopub(), transformContext());
    }

    private static TransformContext transformContext() throws Exception {
        SigningKeyPair keyPair = NanopubTestSuite.getLatest().getSigningKey("rsa-key1");
        KeyPair key = SignNanopub.loadKey(keyPair.getPrivateKeyFile().getPath(), SignatureAlgorithm.RSA);
        return new TransformContext(SignatureAlgorithm.RSA, key,
                vf.createIRI("https://orcid.org/0000-0000-0000-0000"), false, false, false);
    }

    @Test
    void encodePublicKey() throws Exception {
        String encoded = SignatureUtils.encodePublicKey(transformContext().getKey().getPublic());

        assertFalse(encoded.isBlank());
        assertFalse(encoded.contains(" "));
    }

    @Test
    void getPubKeyOfASignedNanopub() throws Exception {
        Nanopub signed = signedNanopub();

        assertEquals(SignatureUtils.encodePublicKey(transformContext().getKey().getPublic()),
                SignatureUtils.getPubKey(signed));
    }

    @Test
    void getPubKeyOfAnUnsignedNanopubIsNull() throws Exception {
        assertNull(SignatureUtils.getPubKey(org.nanopub.utils.TestUtils.createNanopub()));
    }

    @Test
    void getPubKeyOfAMalformedSignatureIsNull() {
        Nanopub nanopub = nanopubWithPubinfo(signatureTarget(),
                vf.createStatement(SIG, NPX.HAS_PUBLIC_KEY, vf.createLiteral("a key")));

        assertNull(SignatureUtils.getPubKey(nanopub));
    }

    @Test
    void assertMatchingPubkeysAcceptsTheSigningKey() throws Exception {
        Nanopub signed = signedNanopub();

        assertDoesNotThrow(() -> SignatureUtils.assertMatchingPubkeys(transformContext(), signed));
    }

    @Test
    void assertMatchingPubkeysRefusesAnotherKey() throws Exception {
        Nanopub signed = signedNanopub();
        SigningKeyPair otherKeyPair = NanopubTestSuite.getLatest().getSigningKey("rsa-key2");
        KeyPair otherKey = SignNanopub.loadKey(otherKeyPair.getPrivateKeyFile().getPath(), SignatureAlgorithm.RSA);
        TransformContext other = new TransformContext(SignatureAlgorithm.RSA, otherKey, null, false, false, false);

        MalformedCryptoElementException ex = assertThrows(MalformedCryptoElementException.class,
                () -> SignatureUtils.assertMatchingPubkeys(other, signed));
        assertEquals("The old public key does not match the new public key", ex.getMessage());
    }

    @Test
    void hasValidSignatureOfASignedNanopub() throws Exception {
        NanopubSignatureElement element = SignatureUtils.getSignatureElement(signedNanopub());

        assertTrue(SignatureUtils.hasValidSignature(element));
    }

}