package org.nanopub;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.junit.jupiter.api.Test;
import org.nanopub.utils.TestUtils;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.nanopub.utils.TestUtils.anyIri;
import static org.nanopub.utils.TestUtils.vf;

class NanopubEqualityTest {

    private static Nanopub nanopub(String uri, String label, String created) throws Exception {
        NanopubCreator creator = TestUtils.getNanopubCreator(uri);
        creator.addAssertionStatement(anyIri, RDFS.LABEL, vf.createLiteral(label));
        creator.addProvenanceStatement(anyIri, anyIri);
        creator.addPubinfoStatement(anyIri, anyIri);
        if (created != null) {
            creator.addPubinfoStatement(DCTERMS.CREATED, vf.createLiteral(created, XSD.DATETIME));
        }
        return creator.finalizeNanopub();
    }

    @Test
    void nanopubsWithDifferentTempIrisAreEqual() throws Exception {
        Nanopub a = nanopub("http://purl.org/nanopub/temp/1111/", "same", null);
        Nanopub b = nanopub("http://purl.org/nanopub/temp/2222/", "same", null);

        assertTrue(NanopubEquality.unsignedNanopubsAreEqual(a, b));
    }

    @Test
    void nanopubsWithDifferentTimestampsAreEqual() throws Exception {
        Nanopub a = nanopub("http://purl.org/nanopub/temp/1111/", "same", "2024-01-02T03:04:05Z");
        Nanopub b = nanopub("http://purl.org/nanopub/temp/1111/", "same", "2025-06-07T08:09:10Z");

        assertTrue(NanopubEquality.unsignedNanopubsAreEqual(a, b));
    }

    @Test
    void nanopubsWithDifferentContentAreNotEqual() throws Exception {
        Nanopub a = nanopub("http://purl.org/nanopub/temp/1111/", "one", null);
        Nanopub b = nanopub("http://purl.org/nanopub/temp/1111/", "another", null);

        assertFalse(NanopubEquality.unsignedNanopubsAreEqual(a, b));
    }

    @Test
    void rejectsNullArguments() throws Exception {
        Nanopub nanopub = TestUtils.createNanopub();

        assertThrows(NullPointerException.class, () -> NanopubEquality.unsignedNanopubsAreEqual(null, nanopub));
        assertThrows(NullPointerException.class, () -> NanopubEquality.unsignedNanopubsAreEqual(nanopub, null));
    }

    @Test
    void treatsAMissingContextLikeTheNanopubsOwnIri() {
        // NanopubImpl never yields statements without a context, but the comparison accepts any
        // Nanopub implementation, so a null context has to be handled rather than throwing.
        IRI npUri = vf.createIRI("https://example.org/np1#");
        Statement withoutContext = vf.createStatement(anyIri, RDFS.LABEL, vf.createLiteral("x"));

        assertTrue(NanopubEquality.unsignedNanopubsAreEqual(
                nanopubReturning(npUri, withoutContext), nanopubReturning(npUri, withoutContext)));
    }

    private static Nanopub nanopubReturning(IRI uri, Statement headStatement) {
        Nanopub nanopub = mock(Nanopub.class);
        when(nanopub.getUri()).thenReturn(uri);
        when(nanopub.getHead()).thenReturn(Set.of(headStatement));
        when(nanopub.getAssertion()).thenReturn(Set.of());
        when(nanopub.getProvenance()).thenReturn(Set.of());
        when(nanopub.getPubinfo()).thenReturn(Set.of());
        return nanopub;
    }

}
