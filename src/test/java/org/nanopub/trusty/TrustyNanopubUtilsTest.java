package org.nanopub.trusty;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.Test;
import org.nanopub.Nanopub;
import org.nanopub.NanopubCreator;
import org.nanopub.utils.TestUtils;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.nanopub.utils.TestUtils.anyIri;
import static org.nanopub.utils.TestUtils.vf;

class TrustyNanopubUtilsTest {

    private static Nanopub trustyNanopub() throws Exception {
        NanopubCreator creator = TestUtils.getNanopubCreator();
        creator.addAssertionStatement(anyIri, anyIri, anyIri);
        creator.addProvenanceStatement(anyIri, anyIri);
        creator.addPubinfoStatement(anyIri, anyIri);
        return creator.finalizeTrustyNanopub();
    }

    @Test
    void declaresTheSerializedTrustyNanopubFormat() {
        assertEquals("Serialized Trusty Nanopub", TrustyNanopubUtils.STNP_FORMAT.getName());
        assertEquals("stnp", TrustyNanopubUtils.STNP_FORMAT.getDefaultFileExtension());
        assertFalse(TrustyNanopubUtils.STNP_FORMAT.supportsNamespaces());
    }

    @Test
    void writeNanopubEmitsTheNanopubWithItsStandardNamespaces() throws Exception {
        Nanopub nanopub = trustyNanopub();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        TrustyNanopubUtils.writeNanopub(nanopub, out, RDFFormat.TRIG);

        String trig = out.toString(StandardCharsets.UTF_8);
        assertTrue(trig.contains(nanopub.getUri().toString()), trig);
        assertTrue(trig.contains("@prefix this:"), trig);
        assertTrue(trig.contains("@prefix sub:"), trig);
        assertTrue(trig.contains("@prefix np:"), trig);
        // the bnode character is part of a valid local name, so no separate "node" prefix is needed
        assertFalse(trig.contains("@prefix node:"), trig);
    }

    @Test
    void isValidTrustyNanopubAcceptsATrustyNanopub() throws Exception {
        assertTrue(TrustyNanopubUtils.isValidTrustyNanopub(trustyNanopub()));
    }

    @Test
    void isValidTrustyNanopubRejectsANanopubWithoutAnArtifactCode() throws Exception {
        assertFalse(TrustyNanopubUtils.isValidTrustyNanopub(TestUtils.createNanopub()));
    }

    @Test
    void isValidTrustyNanopubRejectsGraphUrisOutsideTheNanopubUri() {
        // NanopubImpl would refuse such a nanopub, but the check accepts any Nanopub implementation
        IRI npUri = vf.createIRI("https://example.org/np1#");
        Nanopub nanopub = mock(Nanopub.class);
        when(nanopub.getUri()).thenReturn(npUri);
        when(nanopub.getGraphUris()).thenReturn(Set.of(vf.createIRI("https://elsewhere.example.com/graph")));

        assertFalse(TrustyNanopubUtils.isValidTrustyNanopub(nanopub));
    }

    @Test
    void isValidTrustyNanopubRejectsATamperedNanopub() throws Exception {
        Nanopub trusty = trustyNanopub();
        // same trusty URI, but one extra statement, so the content no longer hashes to it
        Nanopub tampered = mock(Nanopub.class);
        when(tampered.getUri()).thenReturn(trusty.getUri());
        when(tampered.getGraphUris()).thenReturn(trusty.getGraphUris());
        when(tampered.getHead()).thenReturn(trusty.getHead());
        when(tampered.getAssertion()).thenReturn(trusty.getAssertion());
        when(tampered.getProvenance()).thenReturn(trusty.getProvenance());
        when(tampered.getPubinfo()).thenReturn(Set.of());

        assertFalse(TrustyNanopubUtils.isValidTrustyNanopub(tampered));
    }

    @Test
    void getTrustyDigestStringForATrustyNanopub() throws Exception {
        assertNotNull(TrustyNanopubUtils.getTrustyDigestString(trustyNanopub()));
    }

    @Test
    void getTrustyDigestStringWithoutAnArtifactCodeIsNull() throws Exception {
        assertNull(TrustyNanopubUtils.getTrustyDigestString(TestUtils.createNanopub()));
    }

}
