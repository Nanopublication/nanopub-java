package org.nanopub.trusty;

import org.junit.jupiter.api.Test;
import org.nanopub.Nanopub;
import org.nanopub.NanopubCreator;
import org.nanopub.utils.TestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.nanopub.utils.TestUtils.anyIri;

class TrustyNanopubPatternTest {

    private final TrustyNanopubPattern pattern = new TrustyNanopubPattern();

    private static Nanopub trustyNanopub() throws Exception {
        NanopubCreator creator = TestUtils.getNanopubCreator();
        creator.addAssertionStatement(anyIri, anyIri, anyIri);
        creator.addProvenanceStatement(anyIri, anyIri);
        creator.addPubinfoStatement(anyIri, anyIri);
        return creator.finalizeTrustyNanopub();
    }

    @Test
    void getName() {
        assertEquals("Trusty nanopublication", pattern.getName());
    }

    @Test
    void getPatternInfoUrl() throws Exception {
        assertEquals("http://trustyuri.net/", pattern.getPatternInfoUrl().toString());
    }

    @Test
    void appliesToNanopubsWithATrustyUri() throws Exception {
        assertTrue(pattern.appliesTo(trustyNanopub()));
    }

    @Test
    void doesNotApplyToNanopubsWithoutATrustyUri() throws Exception {
        assertFalse(pattern.appliesTo(TestUtils.createNanopub()));
    }

    @Test
    void isCorrectlyUsedByATrustyNanopub() throws Exception {
        Nanopub nanopub = trustyNanopub();

        assertTrue(pattern.isCorrectlyUsedBy(nanopub));
        assertEquals("This nanopublication has a valid Trusty URI.", pattern.getDescriptionFor(nanopub));
    }

    @Test
    void isNotCorrectlyUsedByAPlainNanopub() throws Exception {
        Nanopub nanopub = TestUtils.createNanopub();

        assertFalse(pattern.isCorrectlyUsedBy(nanopub));
        assertEquals("The Trusty URI of this nanopublication is not valid.", pattern.getDescriptionFor(nanopub));
    }

}
