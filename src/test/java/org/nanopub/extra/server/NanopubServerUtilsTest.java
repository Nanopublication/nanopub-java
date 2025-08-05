package org.nanopub.extra.server;

import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.jupiter.api.Test;
import org.nanopub.MalformedNanopubException;
import org.nanopub.Nanopub;
import org.nanopub.NanopubCreator;
import org.nanopub.utils.TestUtils;
import org.nanopub.vocabulary.NPX;

import static org.junit.jupiter.api.Assertions.*;

class NanopubServerUtilsTest {

    @Test
    void constructor() {
        assertThrows(RuntimeException.class, NanopubServerUtils::new);
    }

    @Test
    void isProtectedNanopubTrue() throws MalformedNanopubException {
        NanopubCreator creator = TestUtils.getNanopubCreator();
        creator.addAssertionStatement(TestUtils.anyIri, TestUtils.anyIri, TestUtils.anyIri);
        creator.addProvenanceStatement(TestUtils.anyIri, TestUtils.anyIri);
        creator.addPubinfoStatement(RDF.TYPE, NPX.PROTECTED_NANOPUB);
        Nanopub nanopub = creator.finalizeNanopub();
        assertTrue(NanopubServerUtils.isProtectedNanopub(nanopub));
    }

    @Test
    void isProtectedNanopubFalse() throws MalformedNanopubException {
        Nanopub nanopub = TestUtils.createNanopub();
        assertFalse(NanopubServerUtils.isProtectedNanopub(nanopub));
    }

}