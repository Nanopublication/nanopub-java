package org.nanopub.extra.services;

import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.jupiter.api.Test;
import org.nanopub.Nanopub;
import org.nanopub.NanopubCreator;
import org.nanopub.NanopubPatterns;
import org.nanopub.utils.TestUtils;
import org.nanopub.vocabulary.KPXL_GRLC;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.nanopub.utils.TestUtils.anyIri;

class GrlcQueryPatternTest {

    private static final GrlcQueryPattern pattern = new GrlcQueryPattern();

    private static Nanopub grlcQuery(String sparql) throws Exception {
        NanopubCreator creator = TestUtils.getNanopubCreator();
        creator.addAssertionStatement(anyIri, RDF.TYPE, KPXL_GRLC.GRLC_QUERY);
        if (sparql != null) {
            creator.addAssertionStatement(anyIri, KPXL_GRLC.SPARQL, TestUtils.vf.createLiteral(sparql));
        }
        creator.addProvenanceStatement(creator.getAssertionUri(), anyIri, anyIri);
        creator.addPubinfoStatement(anyIri, anyIri);
        return creator.finalizeNanopub();
    }

    @Test
    void appliesToANanopubWithASparqlLiteral() throws Exception {
        assertTrue(pattern.appliesTo(grlcQuery("SELECT ?x WHERE { ?x ?y ?z }")));
    }

    @Test
    void doesNotApplyToANanopubWithoutASparqlLiteral() throws Exception {
        assertFalse(pattern.appliesTo(grlcQuery(null)));
        assertFalse(pattern.appliesTo(TestUtils.createNanopub()));
    }

    @Test
    void acceptsAQueryThatParses() throws Exception {
        Nanopub np = grlcQuery("SELECT ?x WHERE { ?x ?y ?z }");

        assertTrue(pattern.isCorrectlyUsedBy(np));
        assertEquals("grlc query with valid SPARQL", pattern.getDescriptionFor(np));
    }

    @Test
    void rejectsAQueryThatDoesNotParse() throws Exception {
        Nanopub np = grlcQuery("SELECT ?x WHERE { ?x ?y }");

        assertFalse(pattern.isCorrectlyUsedBy(np));
        assertTrue(pattern.getDescriptionFor(np).contains("This is not valid SPARQL."), pattern.getDescriptionFor(np));
    }

    @Test
    void namesAnInvisibleCharacterInTheDescription() throws Exception {
        Nanopub np = grlcQuery("SELECT ?x WHERE {\u00A0?x ?y ?z }");

        assertFalse(pattern.isCorrectlyUsedBy(np));
        assertTrue(pattern.getDescriptionFor(np).contains("U+00A0 (NO-BREAK SPACE)"), pattern.getDescriptionFor(np));
    }

    @Test
    void hasANameAndAnInfoUrl() throws Exception {
        assertEquals("grlc query nanopublication", pattern.getName());
        assertNotNull(pattern.getPatternInfoUrl());
    }

    @Test
    void isRegistered() {
        assertTrue(NanopubPatterns.getPatterns().stream().anyMatch(p -> p instanceof GrlcQueryPattern));
    }

}
