package org.nanopub.extra.index;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.DC;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.jupiter.api.Test;
import org.nanopub.*;
import org.nanopub.utils.TestUtils;
import org.nanopub.vocabulary.NPX;

import java.util.Set;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.nanopub.utils.TestUtils.anyIri;
import static org.nanopub.utils.TestUtils.vf;

class NanopubIndexImplTest {

    private static final IRI ELEMENT = vf.createIRI("https://example.org/element1");
    private static final IRI SUB_INDEX = vf.createIRI("https://example.org/subIndex1");
    private static final IRI APPENDED = vf.createIRI("https://example.org/appended");

    /**
     * Builds an index nanopub, letting the caller add whatever the test needs on top of the
     * type statement that makes it an index in the first place.
     */
    private static Nanopub indexNanopub(Consumer<NanopubCreator> content) throws Exception {
        NanopubCreator creator = TestUtils.getNanopubCreator("https://example.org/index1#");
        creator.addProvenanceStatement(anyIri, anyIri);
        creator.addPubinfoStatement(RDF.TYPE, NPX.NANOPUB_INDEX);
        content.accept(creator);
        return creator.finalizeNanopub();
    }

    private static void quietly(ThrowingConsumer action, NanopubCreator creator) {
        try {
            action.accept(creator);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private interface ThrowingConsumer {
        void accept(NanopubCreator creator) throws Exception;
    }

    private static Consumer<NanopubCreator> content(ThrowingConsumer action) {
        return creator -> quietly(action, creator);
    }

    // ------------------------------------------------------------ well-formed

    @Test
    void readsElementsSubIndexesAndTheAppendedIndex() throws Exception {
        Nanopub np = indexNanopub(content(creator -> {
            creator.addAssertionStatement(creator.getNanopubUri(), NPX.INCLUDES_ELEMENT, ELEMENT);
            creator.addAssertionStatement(creator.getNanopubUri(), NPX.INCLUDES_SUBINDEX, SUB_INDEX);
            creator.addAssertionStatement(creator.getNanopubUri(), NPX.APPENDS_INDEX, APPENDED);
            // statements about something else are ignored
            creator.addAssertionStatement(anyIri, NPX.INCLUDES_ELEMENT, vf.createIRI("https://example.org/other"));
        }));

        NanopubIndex index = new NanopubIndexImpl(np);

        assertEquals(Set.of(ELEMENT), index.getElements());
        assertEquals(Set.of(SUB_INDEX), index.getSubIndexes());
        assertEquals(APPENDED, index.getAppendedIndex());
        assertFalse(index.isIncomplete());
    }

    @Test
    void recognisesAnIncompleteIndex() throws Exception {
        Nanopub np = indexNanopub(content(creator -> {
            creator.addAssertionStatement(creator.getNanopubUri(), NPX.INCLUDES_ELEMENT, ELEMENT);
            creator.addPubinfoStatement(RDF.TYPE, NPX.INCOMPLETE_INDEX);
        }));

        assertTrue(new NanopubIndexImpl(np).isIncomplete());
    }

    @Test
    void delegatesEverythingElseToTheUnderlyingNanopub() throws Exception {
        Nanopub np = indexNanopub(content(creator ->
                creator.addAssertionStatement(creator.getNanopubUri(), NPX.INCLUDES_ELEMENT, ELEMENT)));

        NanopubIndexImpl index = new NanopubIndexImpl(np);

        assertEquals(np.getUri(), index.getUri());
        assertEquals(np.getHeadUri(), index.getHeadUri());
        assertEquals(np.getHead(), index.getHead());
        assertEquals(np.getAssertionUri(), index.getAssertionUri());
        assertEquals(np.getAssertion(), index.getAssertion());
        assertEquals(np.getProvenanceUri(), index.getProvenanceUri());
        assertEquals(np.getProvenance(), index.getProvenance());
        assertEquals(np.getPubinfoUri(), index.getPubinfoUri());
        assertEquals(np.getPubinfo(), index.getPubinfo());
        assertEquals(np.getGraphUris(), index.getGraphUris());
        assertEquals(np.getCreationTime(), index.getCreationTime());
        assertEquals(np.getAuthors(), index.getAuthors());
        assertEquals(np.getCreators(), index.getCreators());
        assertEquals(np.getTripleCount(), index.getTripleCount());
        assertEquals(np.getByteCount(), index.getByteCount());
    }

    @Test
    void exposesTheNamespacesOfTheUnderlyingNanopub() throws Exception {
        Nanopub np = indexNanopub(content(creator -> {
            creator.addAssertionStatement(creator.getNanopubUri(), NPX.INCLUDES_ELEMENT, ELEMENT);
            creator.addNamespace("ex", "https://example.org/");
        }));

        NanopubIndexImpl index = new NanopubIndexImpl(np);

        assertTrue(index.getNsPrefixes().contains("ex"));
        assertEquals("https://example.org/", index.getNamespace("ex"));
    }

    @Test
    void hasNoNamespacesWhenTheUnderlyingNanopubHasNone() throws Exception {
        Nanopub withNs = indexNanopub(content(creator ->
                creator.addAssertionStatement(creator.getNanopubUri(), NPX.INCLUDES_ELEMENT, ELEMENT)));
        // a plain Nanopub, i.e. one that does not carry prefix mappings
        Nanopub plain = org.mockito.Mockito.mock(Nanopub.class);
        org.mockito.Mockito.when(plain.getUri()).thenReturn(withNs.getUri());
        org.mockito.Mockito.when(plain.getAssertion()).thenReturn(withNs.getAssertion());
        org.mockito.Mockito.when(plain.getPubinfo()).thenReturn(withNs.getPubinfo());

        NanopubIndexImpl index = new NanopubIndexImpl(plain);

        assertTrue(index.getNsPrefixes().isEmpty());
        assertNull(index.getNamespace("ex"));
    }

    @Test
    void readsTheNameAndDescription() throws Exception {
        Nanopub np = indexNanopub(content(creator -> {
            creator.addAssertionStatement(creator.getNanopubUri(), NPX.INCLUDES_ELEMENT, ELEMENT);
            creator.addPubinfoStatement(DC.TITLE, vf.createLiteral("My index"));
            creator.addPubinfoStatement(DC.DESCRIPTION, vf.createLiteral("What it holds"));
        }));

        NanopubIndex index = new NanopubIndexImpl(np);

        assertEquals("My index", index.getName());
        assertEquals("What it holds", index.getDescription());
    }

    @Test
    void hasNoNameOrDescriptionWhenNoneIsGiven() throws Exception {
        Nanopub np = indexNanopub(content(creator -> {
            creator.addAssertionStatement(creator.getNanopubUri(), NPX.INCLUDES_ELEMENT, ELEMENT);
            // about something else, and of the wrong object type
            creator.addPubinfoStatement(vf.createStatement(anyIri, DC.TITLE, vf.createLiteral("not the index")));
            creator.addPubinfoStatement(DC.TITLE, anyIri);
            creator.addPubinfoStatement(DCTERMS.TITLE, vf.createLiteral("dcterms is not read here"));
        }));

        NanopubIndex index = new NanopubIndexImpl(np);

        assertNull(index.getName());
        assertNull(index.getDescription());
    }

    // -------------------------------------------------------------- malformed

    @Test
    void refusesANanopubThatIsNotAnIndex() throws Exception {
        Nanopub np = TestUtils.createNanopub();

        MalformedNanopubException ex = assertThrows(MalformedNanopubException.class, () -> new NanopubIndexImpl(np));
        assertEquals("Nanopub is not a nanopub index", ex.getMessage());
    }

    @Test
    void refusesMultipleAppendsStatements() throws Exception {
        Nanopub np = indexNanopub(content(creator -> {
            creator.addAssertionStatement(creator.getNanopubUri(), NPX.APPENDS_INDEX, APPENDED);
            creator.addAssertionStatement(creator.getNanopubUri(), NPX.APPENDS_INDEX,
                    vf.createIRI("https://example.org/appended2"));
        }));

        MalformedNanopubException ex = assertThrows(MalformedNanopubException.class, () -> new NanopubIndexImpl(np));
        assertEquals("Multiple appends-statements found for index", ex.getMessage());
    }

    @Test
    void refusesANonUriAppendedIndex() throws Exception {
        Nanopub np = indexNanopub(content(creator -> creator.addAssertionStatement(
                creator.getNanopubUri(), NPX.APPENDS_INDEX, vf.createLiteral("not a URI"))));

        MalformedNanopubException ex = assertThrows(MalformedNanopubException.class, () -> new NanopubIndexImpl(np));
        assertEquals("URI expected for object of appends-statement", ex.getMessage());
    }

    @Test
    void refusesANonUriElement() throws Exception {
        Nanopub np = indexNanopub(content(creator -> creator.addAssertionStatement(
                creator.getNanopubUri(), NPX.INCLUDES_ELEMENT, vf.createLiteral("not a URI"))));

        MalformedNanopubException ex = assertThrows(MalformedNanopubException.class, () -> new NanopubIndexImpl(np));
        assertEquals("Element has to be a URI", ex.getMessage());
    }

    @Test
    void refusesANonUriSubIndex() throws Exception {
        Nanopub np = indexNanopub(content(creator -> creator.addAssertionStatement(
                creator.getNanopubUri(), NPX.INCLUDES_SUBINDEX, vf.createLiteral("not a URI"))));

        MalformedNanopubException ex = assertThrows(MalformedNanopubException.class, () -> new NanopubIndexImpl(np));
        assertEquals("Sub-index has to be a URI", ex.getMessage());
    }

    @Test
    void refusesAnIndexThatIsTooLarge() throws Exception {
        Nanopub np = indexNanopub(content(creator -> {
            for (int i = 0; i <= NanopubIndex.MAX_SIZE; i++) {
                creator.addAssertionStatement(creator.getNanopubUri(), NPX.INCLUDES_ELEMENT,
                        vf.createIRI("https://example.org/element" + i));
            }
        }));

        MalformedNanopubException ex = assertThrows(MalformedNanopubException.class, () -> new NanopubIndexImpl(np));
        assertEquals("Nanopub index exceeds maximum size", ex.getMessage());
    }

}
