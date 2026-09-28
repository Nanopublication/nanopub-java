package org.nanopub.extra.security;

import net.trustyuri.rdf.RdfFileContent;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.nanopub.NanopubProfile;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.nanopub.utils.TestUtils.vf;

class TransformContextTest {

    private static final IRI SIGNER = vf.createIRI("https://orcid.org/0000-0000-0000-0001");
    private static final IRI TEMP = vf.createIRI("http://purl.org/nanopub/temp/1234/");
    private static final IRI TRUSTY = vf.createIRI("https://w3id.org/np/RAO30EliKt55zd1CjWpKBE9q3KeJfoy9q0Q5x-XaSNxRk");

    private static KeyPair anyKey() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(1024);
        return generator.generateKeyPair();
    }

    private static TransformContext context(boolean resolveCrossRefs, boolean prefixBased) throws Exception {
        return new TransformContext(SignatureAlgorithm.RSA, anyKey(), SIGNER, resolveCrossRefs, prefixBased, false);
    }

    @Test
    void keepsWhatItWasGiven() throws Exception {
        KeyPair key = anyKey();

        TransformContext context = new TransformContext(SignatureAlgorithm.RSA, key, SIGNER, false, false, true);

        assertEquals(SignatureAlgorithm.RSA, context.getSignatureAlgorithm());
        assertEquals(key, context.getKey());
        assertEquals(SIGNER, context.getSigner());
        assertTrue(context.isIgnoreSignedEnabled());
    }

    @Test
    void withoutCrossReferenceResolutionThereAreNoMaps() throws Exception {
        TransformContext context = context(false, false);

        assertNull(context.getTempRefMap());
        assertNull(context.getTempPrefixMap());
        assertFalse(context.isIgnoreSignedEnabled());
    }

    @Test
    void crossReferenceResolutionAddsAReferenceMap() throws Exception {
        TransformContext context = context(true, false);

        assertNotNull(context.getTempRefMap());
        assertNull(context.getTempPrefixMap());
    }

    @Test
    void prefixBasedResolutionAddsBothMaps() throws Exception {
        TransformContext context = context(false, true);

        assertNotNull(context.getTempRefMap());
        assertNotNull(context.getTempPrefixMap());
    }

    @Test
    void resolveCrossRefsPassesTheContentThroughUnchangedWithoutAMap() throws Exception {
        TransformContext context = context(false, false);
        RdfFileContent content = new RdfFileContent(RDFFormat.TRIG);

        assertSame(content, context.resolveCrossRefs(content));
    }

    @Test
    void resolveCrossRefsRewritesTheContentWithAMap() throws Exception {
        TransformContext context = context(true, false);
        RdfFileContent content = new RdfFileContent(RDFFormat.TRIG);
        content.startRDF();
        content.endRDF();

        assertNotSame(content, context.resolveCrossRefs(content));
    }

    @Test
    void mergeTransformMapIgnoresANullMap() throws Exception {
        TransformContext context = context(true, false);

        assertDoesNotThrow(() -> context.mergeTransformMap(null));
        assertTrue(context.getTempRefMap().isEmpty());
    }

    @Test
    void mergeTransformMapDoesNothingWithoutMaps() throws Exception {
        TransformContext context = context(false, false);

        assertDoesNotThrow(() -> context.mergeTransformMap(Map.of(TEMP, TRUSTY)));
        assertNull(context.getTempRefMap());
    }

    @Test
    void mergeTransformMapChainsTheReferences() throws Exception {
        TransformContext context = context(true, false);
        IRI intermediate = vf.createIRI("https://w3id.org/np/ARTIFACTCODE-PLACEHOLDER/");
        context.getTempRefMap().put(TEMP, intermediate);

        Map<Resource, IRI> map = new HashMap<>();
        map.put(intermediate, TRUSTY);
        context.mergeTransformMap(map);

        assertEquals(TRUSTY, context.getTempRefMap().get(TEMP));
        assertEquals(1, context.getTempRefMap().size());
    }

    @Test
    void mergeTransformMapAddsUnrelatedReferences() throws Exception {
        TransformContext context = context(true, false);

        Map<Resource, IRI> map = new HashMap<>();
        map.put(TEMP, TRUSTY);
        context.mergeTransformMap(map);

        assertEquals(TRUSTY, context.getTempRefMap().get(TEMP));
    }

    @Test
    void mergeTransformMapKeepsOnlyTrustyTargetsInThePrefixMap() throws Exception {
        TransformContext context = context(false, true);

        Map<Resource, IRI> map = new HashMap<>();
        map.put(TEMP, TRUSTY);
        map.put(vf.createIRI("https://example.org/other/"), vf.createIRI("https://example.org/plain/"));
        map.put(vf.createBNode(), TRUSTY);
        context.mergeTransformMap(map);

        assertEquals(TRUSTY.stringValue(), context.getTempPrefixMap().get(TEMP.stringValue()));
        assertEquals(1, context.getTempPrefixMap().size());
    }

    @Test
    void makeDefaultTakesTheSignerAndKeyFromTheProfile() throws Exception {
        KeyPair key = anyKey();

        try (MockedConstruction<NanopubProfile> profile = mockConstruction(NanopubProfile.class,
                (mock, ctx) -> when(mock.getOrcidId()).thenReturn(SIGNER.stringValue()));
             MockedStatic<SignNanopub> signNanopub = mockStatic(SignNanopub.class)) {
            signNanopub.when(() -> SignNanopub.loadKey(anyString(), any(SignatureAlgorithm.class))).thenReturn(key);

            TransformContext context = TransformContext.makeDefault();

            assertEquals(SignatureAlgorithm.RSA, context.getSignatureAlgorithm());
            assertEquals(SIGNER, context.getSigner());
            assertEquals(key, context.getKey());
        }
    }

    @Test
    void makeDefaultWithoutAnOrcidInTheProfile() throws Exception {
        KeyPair key = anyKey();

        try (MockedConstruction<NanopubProfile> profile = mockConstruction(NanopubProfile.class,
                (mock, ctx) -> when(mock.getOrcidId()).thenReturn(null));
             MockedStatic<SignNanopub> signNanopub = mockStatic(SignNanopub.class)) {
            signNanopub.when(() -> SignNanopub.loadKey(anyString(), any(SignatureAlgorithm.class))).thenReturn(key);

            assertNull(TransformContext.makeDefault().getSigner());
        }
    }

    @Test
    void makeDefaultReportsAnUnreadableKey() {
        try (MockedConstruction<NanopubProfile> profile = mockConstruction(NanopubProfile.class,
                (mock, ctx) -> when(mock.getOrcidId()).thenReturn(SIGNER.stringValue()));
             MockedStatic<SignNanopub> signNanopub = mockStatic(SignNanopub.class)) {
            signNanopub.when(() -> SignNanopub.loadKey(anyString(), any(SignatureAlgorithm.class)))
                    .thenThrow(new java.io.IOException("no such key"));

            RuntimeException ex = assertThrows(RuntimeException.class, TransformContext::makeDefault);
            assertTrue(ex.getMessage().startsWith("Could not load key "), ex.getMessage());
        }
    }

}
