package org.nanopub.trusty;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.rio.RDFHandler;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.nanopub.Nanopub;
import org.nanopub.utils.TestUtils;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.nanopub.utils.TestUtils.vf;

class TempUriReplacerTest {

    private static final String TEMP_NP_URI = TempUriReplacer.tempUri + "1234/";

    private static Nanopub nanopubWithUri(String uri) {
        Nanopub nanopub = mock(Nanopub.class);
        when(nanopub.getUri()).thenReturn(vf.createIRI(uri));
        return nanopub;
    }

    @Test
    void hasTempUri() {
        assertTrue(TempUriReplacer.hasTempUri(nanopubWithUri(TEMP_NP_URI)));
        assertFalse(TempUriReplacer.hasTempUri(nanopubWithUri("https://example.org/np1#")));
    }

    @Test
    void replacesTheTempUriInEveryPositionOfAStatement() {
        RDFHandler nested = mock(RDFHandler.class);
        Map<Resource, IRI> transformMap = new HashMap<>();
        TempUriReplacer replacer = new TempUriReplacer(nanopubWithUri(TEMP_NP_URI), nested, transformMap);

        replacer.handleStatement(vf.createStatement(
                vf.createIRI(TEMP_NP_URI + "subject"),
                vf.createIRI(TEMP_NP_URI + "predicate"),
                vf.createIRI(TEMP_NP_URI + "object"),
                vf.createIRI(TEMP_NP_URI + "graph")));

        ArgumentCaptor<Statement> captured = ArgumentCaptor.forClass(Statement.class);
        verify(nested).handleStatement(captured.capture());
        Statement statement = captured.getValue();
        assertEquals(TempUriReplacer.normUri + "subject", statement.getSubject().stringValue());
        assertEquals(TempUriReplacer.normUri + "predicate", statement.getPredicate().stringValue());
        assertEquals(TempUriReplacer.normUri + "object", statement.getObject().stringValue());
        assertEquals(TempUriReplacer.normUri + "graph", statement.getContext().stringValue());
        assertEquals(4, transformMap.size());
    }

    @Test
    void leavesValuesOutsideTheTempUriAlone() {
        RDFHandler nested = mock(RDFHandler.class);
        TempUriReplacer replacer = new TempUriReplacer(nanopubWithUri(TEMP_NP_URI), nested, new HashMap<>());

        replacer.handleStatement(vf.createStatement(
                vf.createIRI("https://example.org/subject"),
                RDFS.LABEL,
                vf.createLiteral("a label"),
                vf.createIRI("https://example.org/graph")));

        ArgumentCaptor<Statement> captured = ArgumentCaptor.forClass(Statement.class);
        verify(nested).handleStatement(captured.capture());
        Statement statement = captured.getValue();
        assertEquals("https://example.org/subject", statement.getSubject().stringValue());
        assertEquals("a label", statement.getObject().stringValue());
    }

    @Test
    void worksWithoutATransformMap() {
        RDFHandler nested = mock(RDFHandler.class);
        TempUriReplacer replacer = new TempUriReplacer(nanopubWithUri(TEMP_NP_URI), nested, null);

        assertDoesNotThrow(() -> replacer.handleStatement(vf.createStatement(
                vf.createIRI(TEMP_NP_URI + "subject"), RDFS.LABEL,
                vf.createLiteral("x"), vf.createIRI(TEMP_NP_URI + "graph"))));
        verify(nested).handleStatement(any());
    }

    @Test
    void replacesTheTempUriInNamespaces() {
        RDFHandler nested = mock(RDFHandler.class);
        TempUriReplacer replacer = new TempUriReplacer(nanopubWithUri(TEMP_NP_URI), nested, new HashMap<>());

        replacer.handleNamespace("this", TEMP_NP_URI);
        replacer.handleNamespace("ex", "https://example.org/");

        verify(nested).handleNamespace("this", TempUriReplacer.normUri);
        verify(nested).handleNamespace("ex", "https://example.org/");
    }

    @Test
    void passesTheOtherEventsOnToTheNestedHandler() {
        RDFHandler nested = mock(RDFHandler.class);
        TempUriReplacer replacer = new TempUriReplacer(nanopubWithUri(TEMP_NP_URI), nested, new HashMap<>());

        replacer.startRDF();
        replacer.handleComment("a comment");
        replacer.endRDF();

        verify(nested).startRDF();
        verify(nested).handleComment("a comment");
        verify(nested).endRDF();
    }

    @Test
    void usesTheDocumentedUriPrefixes() {
        assertEquals("http://purl.org/nanopub/temp/", TempUriReplacer.tempUri);
        assertEquals("https://w3id.org/np/ARTIFACTCODE-PLACEHOLDER/", TempUriReplacer.normUri);
    }

    @Test
    void replacerIsUsedForTempNanopubs() throws Exception {
        // sanity check that the class is wired into the normal creation path
        assertTrue(TempUriReplacer.hasTempUri(nanopubWithUri(
                org.nanopub.NanopubUtils.createTempNanopubIri().stringValue())));
        assertNotNull(TestUtils.createNanopub());
    }

}
