package org.nanopub.extra.server;

import org.junit.jupiter.api.Test;
import org.nanopub.Nanopub;
import org.nanopub.NanopubCreator;
import org.nanopub.utils.TestUtils;
import org.nanopub.vocabulary.KPXL_GRLC;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.nanopub.utils.TestUtils.anyIri;

class PublishNanopubTest {

    private static Nanopub grlcQuery(String sparql) throws Exception {
        NanopubCreator creator = TestUtils.getNanopubCreator();
        creator.addAssertionStatement(anyIri, KPXL_GRLC.SPARQL, TestUtils.vf.createLiteral(sparql));
        creator.addProvenanceStatement(creator.getAssertionUri(), anyIri, anyIri);
        creator.addPubinfoStatement(anyIri, anyIri);
        return creator.finalizeTrustyNanopub();
    }

    @Test
    void refusesToPublishANanopubWithInvalidSparql() throws Exception {
        // no server is contacted: the refusal happens before the registry is looked up
        Nanopub np = grlcQuery("SELECT ?x WHERE { ?x ?y ?z }");

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> new PublishNanopub().publishNanopub(np, "https://example.org/unreachable/"));

        assertTrue(ex.getMessage().contains("Can't publish nanopublication with invalid SPARQL"), ex.getMessage());
        assertTrue(ex.getMessage().contains("U+00A0 (NO-BREAK SPACE)"), ex.getMessage());
    }

}
