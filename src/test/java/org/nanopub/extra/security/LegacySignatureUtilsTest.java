package org.nanopub.extra.security;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.nanopub.*;
import org.nanopub.utils.TestUtils;
import org.nanopub.vocabulary.NPX;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.nanopub.utils.TestUtils.anyIri;
import static org.nanopub.utils.TestUtils.vf;

class LegacySignatureUtilsTest {

    private static final IRI SIGNER = vf.createIRI("https://orcid.org/0000-0000-0000-0001");

    private static KeyPair dsaKey;

    @BeforeAll
    static void generateKey() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("DSA");
        generator.initialize(1024);
        dsaKey = generator.generateKeyPair();
    }

    /**
     * A nanopub that is ready to be signed: it has a temporary URI and some content.
     */
    private static Nanopub preNanopub() throws Exception {
        NanopubCreator creator = new NanopubCreator(true);
        creator.addAssertionStatement(anyIri, RDFS.LABEL, vf.createLiteral("an assertion"));
        creator.addProvenanceStatement(anyIri, anyIri);
        creator.addPubinfoStatement(anyIri, anyIri);
        return creator.finalizeNanopub();
    }

    private static Nanopub legacySignedNanopub(IRI signer) throws Exception {
        return LegacySignatureUtils.createSignedNanopub(preNanopub(), dsaKey, signer);
    }

    /**
     * Builds a nanopub whose pubinfo carries exactly the given statements, on top of the minimum a
     * nanopub needs. Used to feed {@code getSignatureElement} inputs it should reject.
     */
    private static Nanopub nanopubWithPubinfo(java.util.function.BiFunction<IRI, IRI, List<Statement>> pubinfo) throws Exception {
        NanopubCreator creator = TestUtils.getNanopubCreator("https://example.org/np1#");
        IRI npUri = creator.getNanopubUri();
        IRI signatureElementUri = vf.createIRI("https://example.org/np1#sig");
        creator.addAssertionStatement(anyIri, anyIri, anyIri);
        creator.addProvenanceStatement(anyIri, anyIri);
        for (Statement st : pubinfo.apply(npUri, signatureElementUri)) {
            creator.addPubinfoStatement(st);
        }
        return creator.finalizeNanopub();
    }

    private static List<Statement> statements(Statement... statements) {
        return new ArrayList<>(List.of(statements));
    }

    // ------------------------------------------------------ signing and verifying

    @Test
    void createSignedNanopubProducesAVerifiableSignature() throws Exception {
        Nanopub signed = legacySignedNanopub(SIGNER);

        NanopubSignatureElement element = LegacySignatureUtils.getSignatureElement(signed);

        assertNotNull(element);
        assertEquals(SignatureAlgorithm.DSA, element.getAlgorithm());
        assertNotNull(element.getSignature());
        assertNotNull(element.getPublicKeyString());
        assertEquals(java.util.Set.of(SIGNER), element.getSigners());
        assertTrue(LegacySignatureUtils.hasValidSignature(element));
    }

    @Test
    void createSignedNanopubWithoutASigner() throws Exception {
        Nanopub signed = legacySignedNanopub(null);

        NanopubSignatureElement element = LegacySignatureUtils.getSignatureElement(signed);

        assertNotNull(element);
        assertTrue(element.getSigners().isEmpty());
        assertTrue(LegacySignatureUtils.hasValidSignature(element));
    }

    @Test
    void hasValidSignatureRejectsATamperedNanopub() throws Exception {
        Nanopub signed = legacySignedNanopub(SIGNER);

        // keep the signature, but change what it was computed over
        NanopubSignatureElement element = LegacySignatureUtils.getSignatureElement(signed);
        element.addTargetStatement(vf.createStatement(anyIri, RDFS.COMMENT,
                vf.createLiteral("added after signing"), signed.getAssertionUri()));

        assertFalse(LegacySignatureUtils.hasValidSignature(element));
    }

    @Test
    void hasValidSignatureRejectsAnotherKeysSignature() throws Exception {
        Nanopub signed = legacySignedNanopub(SIGNER);
        NanopubSignatureElement element = LegacySignatureUtils.getSignatureElement(signed);

        KeyPairGenerator generator = KeyPairGenerator.getInstance("DSA");
        generator.initialize(1024);
        NanopubSignatureElement otherKey = new NanopubSignatureElement(
                element.getTargetNanopubUri(), element.getUri());
        for (Statement st : element.getTargetStatements()) otherKey.addTargetStatement(st);
        otherKey.setSignatureLiteral(vf.createLiteral(
                jakarta.xml.bind.DatatypeConverter.printBase64Binary(element.getSignature())));
        otherKey.setPublicKeyLiteral(vf.createLiteral(jakarta.xml.bind.DatatypeConverter
                .printBase64Binary(generator.generateKeyPair().getPublic().getEncoded())));

        assertFalse(LegacySignatureUtils.hasValidSignature(otherKey));
    }

    // -------------------------------------------------------- getSignatureElement

    @Test
    void getSignatureElementWithoutASignatureIsNull() throws Exception {
        assertNull(LegacySignatureUtils.getSignatureElement(TestUtils.createNanopub()));
    }

    @Test
    void getSignatureElementKeepsUnrelatedPubinfoStatementsAsTargets() throws Exception {
        Nanopub signed = legacySignedNanopub(SIGNER);

        NanopubSignatureElement element = LegacySignatureUtils.getSignatureElement(signed);

        // head, assertion and provenance are all signed over, plus the unrelated pubinfo statements
        assertTrue(element.getTargetStatements().size() >= signed.getHead().size()
                                                          + signed.getAssertion().size() + signed.getProvenance().size());
    }

    @Test
    void getSignatureElementRefusesANonUriSignatureElement() throws Exception {
        Nanopub nanopub = nanopubWithPubinfo((np, sig) -> statements(
                vf.createStatement(np, NPX.HAS_SIGNATURE_ELEMENT, vf.createLiteral("not a URI"))));

        MalformedCryptoElementException ex = assertThrows(MalformedCryptoElementException.class,
                () -> LegacySignatureUtils.getSignatureElement(nanopub));
        assertEquals("Signature element must be identified by URI", ex.getMessage());
    }

    @Test
    void getSignatureElementRefusesMultipleSignatureElements() throws Exception {
        Nanopub nanopub = nanopubWithPubinfo((np, sig) -> statements(
                vf.createStatement(np, NPX.HAS_SIGNATURE_ELEMENT, sig),
                vf.createStatement(np, NPX.HAS_SIGNATURE_ELEMENT, vf.createIRI("https://example.org/np1#sig2"))));

        MalformedCryptoElementException ex = assertThrows(MalformedCryptoElementException.class,
                () -> LegacySignatureUtils.getSignatureElement(nanopub));
        assertEquals("Multiple signature elements found", ex.getMessage());
    }

    @Test
    void getSignatureElementRefusesANonLiteralSignature() throws Exception {
        Nanopub nanopub = nanopubWithPubinfo((np, sig) -> statements(
                vf.createStatement(np, NPX.HAS_SIGNATURE_ELEMENT, sig),
                vf.createStatement(sig, NPX.HAS_SIGNATURE, anyIri)));

        MalformedCryptoElementException ex = assertThrows(MalformedCryptoElementException.class,
                () -> LegacySignatureUtils.getSignatureElement(nanopub));
        assertTrue(ex.getMessage().startsWith("Literal expected as signature: "), ex.getMessage());
    }

    @Test
    void getSignatureElementRefusesANonLiteralPublicKey() throws Exception {
        Nanopub nanopub = nanopubWithPubinfo((np, sig) -> statements(
                vf.createStatement(np, NPX.HAS_SIGNATURE_ELEMENT, sig),
                vf.createStatement(sig, NPX.HAS_PUBLIC_KEY, anyIri)));

        MalformedCryptoElementException ex = assertThrows(MalformedCryptoElementException.class,
                () -> LegacySignatureUtils.getSignatureElement(nanopub));
        assertTrue(ex.getMessage().startsWith("Literal expected as public key: "), ex.getMessage());
    }

    @Test
    void getSignatureElementRefusesANonUriSigner() throws Exception {
        Nanopub nanopub = nanopubWithPubinfo((np, sig) -> statements(
                vf.createStatement(np, NPX.HAS_SIGNATURE_ELEMENT, sig),
                vf.createStatement(sig, NPX.SIGNED_BY, vf.createLiteral("not a URI"))));

        MalformedCryptoElementException ex = assertThrows(MalformedCryptoElementException.class,
                () -> LegacySignatureUtils.getSignatureElement(nanopub));
        assertTrue(ex.getMessage().startsWith("URI expected as signer: "), ex.getMessage());
    }

    @Test
    void getSignatureElementRefusesAMissingSignature() throws Exception {
        Nanopub nanopub = nanopubWithPubinfo((np, sig) -> statements(
                vf.createStatement(np, NPX.HAS_SIGNATURE_ELEMENT, sig),
                vf.createStatement(sig, NPX.HAS_PUBLIC_KEY, vf.createLiteral("a key"))));

        MalformedCryptoElementException ex = assertThrows(MalformedCryptoElementException.class,
                () -> LegacySignatureUtils.getSignatureElement(nanopub));
        assertEquals("Signature element without signature", ex.getMessage());
    }

    @Test
    void getSignatureElementRefusesAMissingPublicKey() throws Exception {
        Nanopub nanopub = nanopubWithPubinfo((np, sig) -> statements(
                vf.createStatement(np, NPX.HAS_SIGNATURE_ELEMENT, sig),
                vf.createStatement(sig, NPX.HAS_SIGNATURE, vf.createLiteral("AAAA"))));

        MalformedCryptoElementException ex = assertThrows(MalformedCryptoElementException.class,
                () -> LegacySignatureUtils.getSignatureElement(nanopub));
        assertEquals("Signature element without public key", ex.getMessage());
    }

    @Test
    void getSignatureElementKeepsOtherStatementsAboutTheSignatureElement() throws Exception {
        Nanopub nanopub = nanopubWithPubinfo((np, sig) -> statements(
                vf.createStatement(np, NPX.HAS_SIGNATURE_ELEMENT, sig),
                vf.createStatement(sig, NPX.HAS_SIGNATURE, vf.createLiteral("AAAA")),
                vf.createStatement(sig, NPX.HAS_PUBLIC_KEY, vf.createLiteral("a key")),
                // not part of the signature vocabulary, so it is signed over like any other statement
                vf.createStatement(sig, RDFS.LABEL, vf.createLiteral("the signature"))));

        NanopubSignatureElement element = LegacySignatureUtils.getSignatureElement(nanopub);

        assertNotNull(element);
        assertTrue(element.getTargetStatements().stream()
                .anyMatch(st -> st.getPredicate().equals(RDFS.LABEL)
                                && st.getObject().stringValue().equals("the signature")));
    }

}
