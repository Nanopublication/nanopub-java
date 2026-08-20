package org.nanopub.extra.index;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.DC;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.junit.jupiter.api.Test;
import org.nanopub.Nanopub;
import org.nanopub.NanopubAlreadyFinalizedException;
import org.nanopub.trusty.TrustyNanopubUtils;
import org.nanopub.utils.TestUtils;
import org.nanopub.vocabulary.NPX;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.nanopub.utils.TestUtils.vf;

class SimpleIndexCreatorTest {

    /**
     * Collects the indexes that the creator hands back, so the test can look at them.
     */
    private static class CollectingIndexCreator extends SimpleIndexCreator {

        final List<NanopubIndex> incomplete = new ArrayList<>();
        final List<NanopubIndex> complete = new ArrayList<>();

        CollectingIndexCreator(boolean makeTrusty) {
            super(makeTrusty);
            setBaseUri("https://example.org/index/");
        }

        CollectingIndexCreator(IRI previousIndexUri, boolean makeTrusty) {
            super(previousIndexUri, makeTrusty);
            setBaseUri("https://example.org/index/");
        }

        @Override
        public void handleIncompleteIndex(NanopubIndex npi) {
            incomplete.add(npi);
        }

        @Override
        public void handleCompleteIndex(NanopubIndex npi) {
            complete.add(npi);
        }

    }

    private static IRI element(int i) {
        return vf.createIRI("https://example.org/elements/np" + i);
    }

    @Test
    void buildsAnIndexOverTheAddedElements() throws Exception {
        CollectingIndexCreator creator = new CollectingIndexCreator(false);
        creator.addElement(element(1));
        creator.addElement(element(2));

        creator.finalizeNanopub();

        assertEquals(1, creator.complete.size());
        assertTrue(creator.incomplete.isEmpty());
        NanopubIndex index = creator.complete.getFirst();
        assertEquals(java.util.Set.of(element(1), element(2)), index.getElements());
        assertFalse(index.isIncomplete());
        assertEquals(index.getUri(), creator.getCompleteIndexUri());
    }

    @Test
    void addsAnElementFromItsNanopub() throws Exception {
        Nanopub np = TestUtils.createNanopub("https://example.org/np1#");
        CollectingIndexCreator creator = new CollectingIndexCreator(false);

        creator.addElement(np);
        creator.finalizeNanopub();

        assertEquals(java.util.Set.of(np.getUri()), creator.complete.getFirst().getElements());
    }

    @Test
    void addsSubIndexes() throws Exception {
        CollectingIndexCreator subCreator = new CollectingIndexCreator(false);
        subCreator.addElement(element(1));
        subCreator.finalizeNanopub();
        NanopubIndex subIndex = subCreator.complete.getFirst();

        CollectingIndexCreator creator = new CollectingIndexCreator(false);
        creator.addSubIndex(subIndex);
        creator.addSubIndex(vf.createIRI("https://example.org/index/other"));
        creator.finalizeNanopub();

        assertEquals(2, creator.complete.getFirst().getSubIndexes().size());
        assertTrue(creator.complete.getFirst().getSubIndexes().contains(subIndex.getUri()));
    }

    @Test
    void recordsTheSupersededIndex() throws Exception {
        CollectingIndexCreator superseded = new CollectingIndexCreator(false);
        superseded.addElement(element(1));
        superseded.finalizeNanopub();

        CollectingIndexCreator creator = new CollectingIndexCreator(false);
        creator.addElement(element(2));
        creator.setSupersededIndex(superseded.complete.getFirst());
        creator.finalizeNanopub();

        assertTrue(creator.complete.getFirst().getPubinfo().stream()
                .anyMatch(st -> st.getPredicate().equals(NPX.SUPERSEDES)));
    }

    @Test
    void recordsTheSupersededIndexByUri() throws Exception {
        IRI supersededUri = vf.createIRI("https://example.org/index/old");
        CollectingIndexCreator creator = new CollectingIndexCreator(false);
        creator.addElement(element(1));
        creator.setSupersededIndex(supersededUri);
        creator.finalizeNanopub();

        assertTrue(creator.complete.getFirst().getPubinfo().stream()
                .anyMatch(st -> st.getPredicate().equals(NPX.SUPERSEDES)
                                && st.getObject().equals(supersededUri)));
    }

    @Test
    void splitsIntoSeveralNanopubsWhenTheIndexGrowsTooLarge() throws Exception {
        CollectingIndexCreator creator = new CollectingIndexCreator(false);
        for (int i = 0; i <= NanopubIndex.MAX_SIZE; i++) {
            creator.addElement(element(i));
        }

        creator.finalizeNanopub();

        assertEquals(1, creator.incomplete.size());
        assertTrue(creator.incomplete.getFirst().isIncomplete());
        assertEquals(NanopubIndex.MAX_SIZE, creator.incomplete.getFirst().getElements().size());
        assertEquals(1, creator.complete.size());
        assertEquals(1, creator.complete.getFirst().getElements().size());
    }

    @Test
    void declaresANamespaceForRepeatedElementPrefixes() throws Exception {
        CollectingIndexCreator creator = new CollectingIndexCreator(false);
        creator.addElement(element(1));
        creator.addElement(element(2));
        creator.addElement(vf.createIRI("https://elsewhere.example.com/np1"));

        creator.finalizeNanopub();

        // the shared prefix of the two elements gets a namespace, the single one does not
        org.nanopub.NanopubWithNs withNs = (org.nanopub.NanopubWithNs) creator.complete.getFirst();
        assertEquals("https://example.org/elements/", withNs.getNamespace("ns1"));
        assertNull(withNs.getNamespace("ns2"));
    }

    @Test
    void makesATrustyIndex() throws Exception {
        CollectingIndexCreator creator = new CollectingIndexCreator(true);
        creator.addElement(element(1));

        creator.finalizeNanopub();

        assertTrue(TrustyNanopubUtils.isValidTrustyNanopub(creator.complete.getFirst()));
    }

    @Test
    void continuesFromAPreviousIndex() throws Exception {
        IRI previous = vf.createIRI("https://example.org/index/previous");
        CollectingIndexCreator creator = new CollectingIndexCreator(previous, false);
        creator.addElement(element(1));

        creator.finalizeNanopub();

        assertEquals(previous, creator.complete.getFirst().getAppendedIndex());
    }

    @Test
    void refusesToBeUsedAfterFinalizing() throws Exception {
        CollectingIndexCreator creator = new CollectingIndexCreator(false);
        creator.addElement(element(1));
        creator.finalizeNanopub();

        assertThrows(NanopubAlreadyFinalizedException.class, () -> creator.addElement(element(2)));
        assertThrows(NanopubAlreadyFinalizedException.class, () -> creator.addSubIndex(element(2)));
        assertThrows(NanopubAlreadyFinalizedException.class, () -> creator.setSupersededIndex(element(2)));
        assertThrows(NanopubAlreadyFinalizedException.class, creator::finalizeNanopub);
    }

    @Test
    void cannotFinalizeAnIndexWithoutAnyEntries() {
        // an index with neither elements nor sub-indexes has an empty assertion graph,
        // which is not a well-formed nanopub
        CollectingIndexCreator creator = new CollectingIndexCreator(false);

        RuntimeException ex = assertThrows(RuntimeException.class, creator::finalizeNanopub);
        assertInstanceOf(org.nanopub.MalformedNanopubException.class, ex.getCause());
    }

    // ------------------------------------------------ the descriptive metadata

    @Test
    void writesTheDescriptiveMetadata() throws Exception {
        IRI license = vf.createIRI("https://creativecommons.org/licenses/by/4.0/");
        IRI seeAlso = vf.createIRI("https://example.org/more");
        CollectingIndexCreator creator = new CollectingIndexCreator(false);
        creator.setTitle("My index");
        creator.setDescription("What it holds");
        creator.setLicense(license);
        creator.addCreator("https://orcid.org/0000-0000-0000-0001");
        creator.addCreator("0000-0000-0000-0002");
        creator.addSeeAlsoUri(seeAlso);
        creator.addElement(element(1));

        creator.finalizeNanopub();

        NanopubIndex index = creator.complete.getFirst();
        assertEquals("My index", index.getName());
        assertEquals("What it holds", index.getDescription());
        assertTrue(index.getPubinfo().stream().anyMatch(st -> st.getPredicate().equals(DC.TITLE)));
        assertTrue(index.getPubinfo().stream().anyMatch(st -> st.getPredicate().equals(DC.DESCRIPTION)));
        assertTrue(index.getPubinfo().stream().anyMatch(st -> st.getPredicate().equals(DCTERMS.LICENSE)
                                                              && st.getObject().equals(license)));
        assertTrue(index.getPubinfo().stream().anyMatch(st -> st.getPredicate().equals(RDFS.SEEALSO)
                                                              && st.getObject().equals(seeAlso)));
        assertEquals(2, index.getCreators().size());
    }

    @Test
    void refusesACreatorThatIsNeitherAUriNorAnOrcid() throws Exception {
        CollectingIndexCreator creator = new CollectingIndexCreator(false);
        creator.addCreator("Jane Doe");
        creator.addElement(element(1));

        RuntimeException ex = assertThrows(RuntimeException.class, creator::finalizeNanopub);
        assertTrue(ex.getMessage().contains("Author has to be URI or ORCID: Jane Doe")
                   || ex.getCause() != null && ex.getCause().getMessage().contains("Author has to be URI or ORCID: Jane Doe"),
                ex.toString());
    }

    @Test
    void theDefaultConstructorMakesATrustyIndexWithoutABaseUri() {
        SimpleIndexCreator creator = new SimpleIndexCreator() {
            @Override
            public void handleIncompleteIndex(NanopubIndex npi) {
            }

            @Override
            public void handleCompleteIndex(NanopubIndex npi) {
            }
        };

        assertNull(creator.getBaseUri());
    }

    @Test
    void theBaseUriCanBeSetAfterwards() {
        SimpleIndexCreator creator = new CollectingIndexCreator(false);

        creator.setBaseUri("https://example.org/other/");

        assertEquals("https://example.org/other/", creator.getBaseUri());
    }

}
