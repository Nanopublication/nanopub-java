package org.nanopub.extra.security;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.nanopub.utils.TestUtils.vf;

class ArtifactCodeUtilsTest {

    private static final String ARTIFACT_CODE = "RA1234";

    @Test
    void removesTheArtifactCodeFromAString() {
        assertEquals("https://example.org/np", ArtifactCodeUtils.removeArtifactCode("https://example.org/npRA1234", ARTIFACT_CODE));
        assertEquals("https://example.org/npthing", ArtifactCodeUtils.removeArtifactCode("https://example.org/npRA1234/thing", ARTIFACT_CODE));
        assertEquals("https://example.org/npthing", ArtifactCodeUtils.removeArtifactCode("https://example.org/npRA1234#thing", ARTIFACT_CODE));
    }

    @Test
    void leavesStringsWithoutTheArtifactCodeAlone() {
        assertEquals("https://example.org/np", ArtifactCodeUtils.removeArtifactCode("https://example.org/np", ARTIFACT_CODE));
    }

    @Test
    void removesTheArtifactCodeFromIris() {
        assertEquals("https://example.org/np/thing",
                ArtifactCodeUtils.removeArtifactCode(vf.createIRI("https://example.org/np/RA1234/thing"), ARTIFACT_CODE).stringValue());
    }

    @Test
    void leavesLiteralsAlone() {
        assertEquals("RA1234 stays here",
                ArtifactCodeUtils.removeArtifactCode(vf.createLiteral("RA1234 stays here"), ARTIFACT_CODE).stringValue());
    }

    @Test
    void removesTheArtifactCodeFromEveryPositionOfAStatement() {
        Statement statement = vf.createStatement(
                vf.createIRI("https://example.org/np/RA1234/subject"),
                vf.createIRI("https://example.org/np/RA1234/predicate"),
                vf.createIRI("https://example.org/np/RA1234/object"),
                vf.createIRI("https://example.org/np/RA1234/graph"));

        Statement stripped = ArtifactCodeUtils.removeArtifactCode(statement, ARTIFACT_CODE);

        assertEquals("https://example.org/np/subject", stripped.getSubject().stringValue());
        assertEquals("https://example.org/np/predicate", stripped.getPredicate().stringValue());
        assertEquals("https://example.org/np/object", stripped.getObject().stringValue());
        assertEquals("https://example.org/np/graph", stripped.getContext().stringValue());
    }

    @Test
    void removesTheArtifactCodeFromEveryStatementOfAList() {
        List<Statement> statements = List.of(
                vf.createStatement(vf.createIRI("https://example.org/np/RA1234/one"), RDFS.LABEL,
                        vf.createLiteral("one"), vf.createIRI("https://example.org/np/RA1234/graph")),
                vf.createStatement(vf.createIRI("https://example.org/np/RA1234/two"), RDFS.LABEL,
                        vf.createLiteral("two"), vf.createIRI("https://example.org/np/RA1234/graph")));

        List<Statement> stripped = ArtifactCodeUtils.removeArtifactCode(statements, ARTIFACT_CODE);

        assertEquals(2, stripped.size());
        assertEquals("https://example.org/np/one", stripped.get(0).getSubject().stringValue());
        assertEquals("https://example.org/np/two", stripped.get(1).getSubject().stringValue());
    }

}
