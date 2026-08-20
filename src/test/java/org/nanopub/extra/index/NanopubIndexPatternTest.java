package org.nanopub.extra.index;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.nanopub.MalformedNanopubException;
import org.nanopub.Nanopub;
import org.nanopub.NanopubAlreadyFinalizedException;
import org.nanopub.NanopubCreator;
import org.nanopub.utils.TestUtils;
import org.nanopub.vocabulary.NPX;
import org.eclipse.rdf4j.model.vocabulary.RDF;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.*;

class NanopubIndexPatternTest {

    @Test
    void getNameDoesNotReturnNull() {
        NanopubIndexPattern pattern = new NanopubIndexPattern();
        assertNotNull(pattern.getName());
    }

    @Test
    void appliesToReturnsTrueForValidIndexNanopub() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        Nanopub nanopub = TestUtils.createNanopub();
        try (var mockedIndexUtils = Mockito.mockStatic(IndexUtils.class)) {
            mockedIndexUtils.when(() -> IndexUtils.isIndex(nanopub)).thenReturn(true);
            NanopubIndexPattern pattern = new NanopubIndexPattern();
            assertTrue(pattern.appliesTo(nanopub));
        }
    }

    @Test
    void appliesToReturnsFalseForNonIndexNanopub() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        Nanopub nanopub = TestUtils.createNanopub();
        try (var mockedIndexUtils = Mockito.mockStatic(IndexUtils.class)) {
            mockedIndexUtils.when(() -> IndexUtils.isIndex(nanopub)).thenReturn(false);
            NanopubIndexPattern pattern = new NanopubIndexPattern();
            assertFalse(pattern.appliesTo(nanopub));
        }
    }

    @Test
    void getPatternInfoUrlReturnsValidUrl() throws MalformedURLException, URISyntaxException {
        NanopubIndexPattern pattern = new NanopubIndexPattern();
        URL url = pattern.getPatternInfoUrl();
        assertNotNull(url);
    }

    @Test
    void getName() {
        assertEquals("Nanopublication index", new NanopubIndexPattern().getName());
    }

    private static Nanopub indexNanopub() throws Exception {
        NanopubCreator creator = TestUtils.getNanopubCreator("https://example.org/index1#");
        creator.addAssertionStatement(creator.getNanopubUri(), NPX.INCLUDES_ELEMENT,
                TestUtils.vf.createIRI("https://example.org/element1"));
        creator.addProvenanceStatement(TestUtils.anyIri, TestUtils.anyIri);
        creator.addPubinfoStatement(RDF.TYPE, NPX.NANOPUB_INDEX);
        return creator.finalizeNanopub();
    }

    @Test
    void isCorrectlyUsedByAWellFormedIndex() throws Exception {
        Nanopub index = indexNanopub();
        NanopubIndexPattern pattern = new NanopubIndexPattern();

        assertTrue(pattern.appliesTo(index));
        assertTrue(pattern.isCorrectlyUsedBy(index));
        assertEquals("This is a valid nanopublication index.", pattern.getDescriptionFor(index));
    }

    @Test
    void isNotCorrectlyUsedByAMalformedIndex() throws Exception {
        NanopubCreator creator = TestUtils.getNanopubCreator("https://example.org/index1#");
        creator.addAssertionStatement(creator.getNanopubUri(), NPX.INCLUDES_ELEMENT,
                TestUtils.vf.createLiteral("not a URI"));
        creator.addProvenanceStatement(TestUtils.anyIri, TestUtils.anyIri);
        creator.addPubinfoStatement(RDF.TYPE, NPX.NANOPUB_INDEX);
        Nanopub malformed = creator.finalizeNanopub();

        NanopubIndexPattern pattern = new NanopubIndexPattern();

        assertTrue(pattern.appliesTo(malformed));
        assertFalse(pattern.isCorrectlyUsedBy(malformed));
        assertEquals("Element has to be a URI", pattern.getDescriptionFor(malformed));
    }

    @Test
    void isNotCorrectlyUsedByANanopubThatIsNotAnIndex() throws Exception {
        Nanopub plain = TestUtils.createNanopub();
        NanopubIndexPattern pattern = new NanopubIndexPattern();

        assertFalse(pattern.appliesTo(plain));
        assertFalse(pattern.isCorrectlyUsedBy(plain));
        assertEquals("Nanopub is not a nanopub index", pattern.getDescriptionFor(plain));
    }

}