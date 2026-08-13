package org.nanopub.trusty;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.rio.RDFHandler;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.nanopub.utils.TestUtils.vf;

class CrossRefResolverTest {

    private static final IRI TEMP = vf.createIRI("http://purl.org/nanopub/temp/1234/");
    private static final IRI RESOLVED = vf.createIRI("https://w3id.org/np/RA1234/");

    private static Statement captureStatement(RDFHandler nested) {
        ArgumentCaptor<Statement> captured = ArgumentCaptor.forClass(Statement.class);
        verify(nested).handleStatement(captured.capture());
        return captured.getValue();
    }

    @Test
    void resolvesReferencesFromTheReferenceMap() {
        RDFHandler nested = mock(RDFHandler.class);
        CrossRefResolver resolver = new CrossRefResolver(Map.of(TEMP, RESOLVED), null, nested);

        resolver.handleStatement(vf.createStatement(TEMP, RDFS.SEEALSO, TEMP, TEMP));

        Statement statement = captureStatement(nested);
        assertEquals(RESOLVED, statement.getSubject());
        assertEquals(RESOLVED, statement.getObject());
        assertEquals(RESOLVED, statement.getContext());
    }

    @Test
    void leavesLiteralsAlone() {
        RDFHandler nested = mock(RDFHandler.class);
        CrossRefResolver resolver = new CrossRefResolver(Map.of(TEMP, RESOLVED), null, nested);

        resolver.handleStatement(vf.createStatement(TEMP, RDFS.LABEL, vf.createLiteral("a label"), TEMP));

        assertEquals("a label", captureStatement(nested).getObject().stringValue());
    }

    @Test
    void leavesUnknownResourcesAlone() {
        RDFHandler nested = mock(RDFHandler.class);
        CrossRefResolver resolver = new CrossRefResolver(Map.of(), null, nested);
        IRI unknown = vf.createIRI("https://example.org/unknown");

        resolver.handleStatement(vf.createStatement(unknown, RDFS.SEEALSO, unknown, unknown));

        assertEquals(unknown, captureStatement(nested).getSubject());
    }

    @Test
    void resolvesReferencesByPrefix() {
        RDFHandler nested = mock(RDFHandler.class);
        Map<String, String> prefixMap = Map.of("http://purl.org/nanopub/temp/1234/", "https://w3id.org/np/RA1234/");
        CrossRefResolver resolver = new CrossRefResolver(Map.of(), prefixMap, nested);

        resolver.handleStatement(vf.createStatement(
                vf.createIRI("http://purl.org/nanopub/temp/1234/thing"), RDFS.SEEALSO,
                vf.createIRI("http://purl.org/nanopub/temp/1234/other"),
                vf.createIRI("http://purl.org/nanopub/temp/1234/graph")));

        Statement statement = captureStatement(nested);
        assertEquals("https://w3id.org/np/RA1234/thing", statement.getSubject().stringValue());
        assertEquals("https://w3id.org/np/RA1234/other", statement.getObject().stringValue());
    }

    @Test
    void leavesResourcesWithAnUnmatchedPrefixAlone() {
        RDFHandler nested = mock(RDFHandler.class);
        Map<String, String> prefixMap = Map.of("http://purl.org/nanopub/temp/1234/", "https://w3id.org/np/RA1234/");
        CrossRefResolver resolver = new CrossRefResolver(Map.of(), prefixMap, nested);
        IRI other = vf.createIRI("https://example.org/thing");

        resolver.handleStatement(vf.createStatement(other, RDFS.SEEALSO, other, other));

        assertEquals(other, captureStatement(nested).getSubject());
    }

    @Test
    void prefersTheReferenceMapOverThePrefixMap() {
        RDFHandler nested = mock(RDFHandler.class);
        Map<String, String> prefixMap = Map.of("http://purl.org/nanopub/temp/1234/", "https://example.org/wrong/");
        CrossRefResolver resolver = new CrossRefResolver(Map.of(TEMP, RESOLVED), prefixMap, nested);

        resolver.handleStatement(vf.createStatement(TEMP, RDFS.SEEALSO, TEMP, TEMP));

        assertEquals(RESOLVED, captureStatement(nested).getSubject());
    }

    @Test
    void resolvesNamespaces() {
        RDFHandler nested = mock(RDFHandler.class);
        CrossRefResolver resolver = new CrossRefResolver(Map.of((Resource) TEMP, RESOLVED), null, nested);

        resolver.handleNamespace("this", TEMP.stringValue());

        verify(nested).handleNamespace("this", RESOLVED.stringValue());
    }

    @Test
    void leavesBlankNodesAloneEvenWithAPrefixMap() {
        RDFHandler nested = mock(RDFHandler.class);
        Map<String, String> prefixMap = Map.of("http://purl.org/nanopub/temp/1234/", "https://w3id.org/np/RA1234/");
        CrossRefResolver resolver = new CrossRefResolver(Map.of(), prefixMap, nested);
        Resource blankNode = vf.createBNode();

        resolver.handleStatement(vf.createStatement(blankNode, RDFS.SEEALSO, blankNode, TEMP));

        assertEquals(blankNode, captureStatement(nested).getSubject());
    }

    @Test
    void passesTheOtherEventsOnToTheNestedHandler() {
        RDFHandler nested = mock(RDFHandler.class);
        CrossRefResolver resolver = new CrossRefResolver(Map.of(), null, nested);

        resolver.startRDF();
        resolver.handleComment("a comment");
        resolver.endRDF();

        verify(nested).startRDF();
        verify(nested).handleComment("a comment");
        verify(nested).endRDF();
    }

}
