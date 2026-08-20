package org.nanopub;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class HtmlWriterTest {

    private static final ValueFactory vf = SimpleValueFactory.getInstance();
    private static final IRI GRAPH = vf.createIRI("https://example.org/graph");
    private static final IRI OTHER_GRAPH = vf.createIRI("https://example.org/otherGraph");
    private static final IRI SUBJECT = vf.createIRI("https://example.org/thing");

    /**
     * Writes one statement per given object into the given graph, and returns the resulting HTML.
     */
    private static String write(Map<String, String> namespaces, Resource context, Value... objects) {
        StringWriter out = new StringWriter();
        HtmlWriter writer = new HtmlWriter(out);
        writer.startRDF();
        namespaces.forEach(writer::handleNamespace);
        for (Value object : objects) {
            writer.handleStatement(vf.createStatement(SUBJECT, RDFS.LABEL, object, context));
        }
        writer.endRDF();
        return out.toString();
    }

    private static Map<String, String> namespaces(String... prefixAndNamespace) {
        Map<String, String> namespaces = new LinkedHashMap<>();
        for (int i = 0; i < prefixAndNamespace.length; i += 2) {
            namespaces.put(prefixAndNamespace[i], prefixAndNamespace[i + 1]);
        }
        return namespaces;
    }

    @Test
    void writesTheHtmlTrigFormat() {
        assertEquals("TriG HTML", new HtmlWriter(new StringWriter()).getRDFFormat().getName());
        assertTrue(HtmlWriter.HTML_FORMAT.supportsContexts());
    }

    @Test
    void constructorsAcceptStreamsAndWriters() {
        assertNotNull(new HtmlWriter(new ByteArrayOutputStream()));
        assertNotNull(new HtmlWriter(new ByteArrayOutputStream(), false));
        assertNotNull(new HtmlWriter(new StringWriter()));
        assertNotNull(new HtmlWriter(new StringWriter(), false));
    }

    @Test
    void writesToAnOutputStream() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        HtmlWriter writer = new HtmlWriter(out);
        writer.startRDF();
        writer.handleStatement(vf.createStatement(SUBJECT, RDFS.LABEL, vf.createLiteral("a label"), GRAPH));
        writer.endRDF();

        assertTrue(out.toString(StandardCharsets.UTF_8).contains("a label"));
    }

    @Test
    void refusesStatementsBeforeTheDocumentIsStarted() {
        HtmlWriter writer = new HtmlWriter(new StringWriter());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> writer.handleStatement(vf.createStatement(SUBJECT, RDFS.LABEL, vf.createLiteral("x"), GRAPH)));
        assertEquals("Document writing has not yet been started", ex.getMessage());
    }

    @Test
    void writesTheContextOfEachGraph() {
        String html = write(namespaces(), GRAPH, vf.createLiteral("x"));

        assertTrue(html.contains("nanopub-context-switch"), html);
        assertTrue(html.contains(GRAPH.stringValue()), html);
    }

    @Test
    void closesAndReopensTheContextWhenItChanges() {
        StringWriter out = new StringWriter();
        HtmlWriter writer = new HtmlWriter(out);
        writer.startRDF();
        writer.handleStatement(vf.createStatement(SUBJECT, RDFS.LABEL, vf.createLiteral("one"), GRAPH));
        writer.handleStatement(vf.createStatement(SUBJECT, RDFS.LABEL, vf.createLiteral("two"), OTHER_GRAPH));
        writer.endRDF();

        String html = out.toString();
        assertTrue(html.contains(GRAPH.stringValue()), html);
        assertTrue(html.contains(OTHER_GRAPH.stringValue()), html);
    }

    @Test
    void writesStatementsWithoutAContext() {
        String html = write(namespaces(), null, vf.createLiteral("x"));

        assertTrue(html.contains("{"), html);
        assertTrue(html.contains("\"x\""), html);
    }

    @Test
    void treatsTwoMissingContextsAsTheSameGraph() {
        StringWriter out = new StringWriter();
        HtmlWriter writer = new HtmlWriter(out);
        writer.startRDF();
        writer.handleStatement(vf.createStatement(SUBJECT, RDFS.LABEL, vf.createLiteral("one"), null));
        writer.handleStatement(vf.createStatement(SUBJECT, RDFS.SEEALSO, SUBJECT, null));
        writer.endRDF();

        // only one context switch is opened for both statements
        String html = out.toString();
        assertEquals(2, html.split("nanopub-context-switch", -1).length - 1, html);
    }

    @Test
    void closesTheContextWhenSwitchingFromAGraphToTheDefaultOne() {
        StringWriter out = new StringWriter();
        HtmlWriter writer = new HtmlWriter(out);
        writer.startRDF();
        writer.handleStatement(vf.createStatement(SUBJECT, RDFS.LABEL, vf.createLiteral("one"), GRAPH));
        writer.handleStatement(vf.createStatement(SUBJECT, RDFS.LABEL, vf.createLiteral("two"), null));
        writer.endRDF();

        assertTrue(out.toString().contains("}"), out.toString());
    }

    @Test
    void writesWithoutContextIndentation() {
        StringWriter indented = new StringWriter();
        HtmlWriter withIndentation = new HtmlWriter(indented, true);
        withIndentation.startRDF();
        withIndentation.handleStatement(vf.createStatement(SUBJECT, RDFS.LABEL, vf.createLiteral("x"), GRAPH));
        withIndentation.endRDF();

        StringWriter flat = new StringWriter();
        HtmlWriter withoutIndentation = new HtmlWriter(flat, false);
        withoutIndentation.startRDF();
        withoutIndentation.handleStatement(vf.createStatement(SUBJECT, RDFS.LABEL, vf.createLiteral("x"), GRAPH));
        withoutIndentation.endRDF();

        assertNotEquals(indented.toString(), flat.toString());
    }

    @Test
    void ignoresComments() {
        StringWriter out = new StringWriter();
        HtmlWriter writer = new HtmlWriter(out);
        writer.startRDF();
        writer.handleStatement(vf.createStatement(SUBJECT, RDFS.LABEL, vf.createLiteral("x"), GRAPH));
        writer.handleComment("this comment is dropped");
        writer.endRDF();

        assertFalse(out.toString().contains("this comment is dropped"), out.toString());
    }

    @Test
    void writesNamespaceDeclarationsAsLinks() {
        String html = write(namespaces("ex", "https://example.org/"), GRAPH, vf.createLiteral("x"));

        assertTrue(html.contains("@prefix ex: &lt;"), html);
        assertTrue(html.contains("<a href=\"https://example.org/\">"), html);
    }

    @Test
    void abbreviatesUrisWithAKnownPrefix() {
        String html = write(namespaces("ex", "https://example.org/"), GRAPH, SUBJECT);

        assertTrue(html.contains(">ex:thing</a>"), html);
    }

    @Test
    void abbreviatesAUriThatIsExactlyTheNamespace() {
        String html = write(namespaces("ex", "https://example.org/thing"), GRAPH, SUBJECT);

        assertTrue(html.contains(">ex:</a>"), html);
    }

    @Test
    void writesTheFullUriWhenNoPrefixMatches() {
        String html = write(namespaces(), GRAPH, vf.createIRI("https://other.example.com/thing"));

        assertTrue(html.contains("&lt;"), html);
        assertTrue(html.contains("https://other.example.com/thing"), html);
    }

    @Test
    void writesTheFullUriWhenItEndsWithADot() {
        String html = write(namespaces("ex", "https://example.org/"), GRAPH, vf.createIRI("https://example.org/thing."));

        assertTrue(html.contains("https://example.org/thing."), html);
    }

    @Test
    void splitsAtTheLastDot() {
        String html = write(namespaces("ex", "https://example.org/np.RA1234."), GRAPH,
                vf.createIRI("https://example.org/np.RA1234.thing"));

        assertTrue(html.contains(">ex:thing</a>"), html);
    }

    @Test
    void splitsAtTheLastColon() {
        String html = write(namespaces("ex", "https://example.org/id:"), GRAPH,
                vf.createIRI("https://example.org/id:thing"));

        assertTrue(html.contains(">ex:thing</a>"), html);
    }

    @Test
    void splitsAtTheLastUnderscore() {
        String html = write(namespaces("ex", "https://example.org/id_"), GRAPH,
                vf.createIRI("https://example.org/id_thing"));

        assertTrue(html.contains(">ex:thing</a>"), html);
    }

    @Test
    void writesRdfTypeAsA() {
        StringWriter out = new StringWriter();
        HtmlWriter writer = new HtmlWriter(out);
        writer.startRDF();
        writer.handleStatement(vf.createStatement(SUBJECT, RDF.TYPE, RDFS.RESOURCE, GRAPH));
        writer.endRDF();

        assertTrue(out.toString().contains(">a</a>"), out.toString());
    }

    @Test
    void refusesBlankNodes() {
        // nanopubs are not supposed to carry blank nodes, so the writer refuses them outright
        HtmlWriter writer = new HtmlWriter(new StringWriter());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> writer.writeBNode(vf.createBNode()));
        assertEquals("Unexpected blank node", ex.getMessage());
    }

    @Test
    void writesPlainStringLiterals() {
        String html = write(namespaces(), GRAPH, vf.createLiteral("a label"));

        assertTrue(html.contains("\"a label\""), html);
    }

    @Test
    void writesMultiLineLiteralsAsLongStrings() {
        assertTrue(write(namespaces(), GRAPH, vf.createLiteral("first\nsecond")).contains("\"\"\""));
        assertTrue(write(namespaces(), GRAPH, vf.createLiteral("first\rsecond")).contains("\"\"\""));
        assertTrue(write(namespaces(), GRAPH, vf.createLiteral("first\tsecond")).contains("\"\"\""));
    }

    @Test
    void writesTheLanguageOfALanguageLiteral() {
        String html = write(namespaces(), GRAPH, vf.createLiteral("een label", "nl"));

        assertTrue(html.contains("\"een label\"@nl"), html);
    }

    @Test
    void writesTheDatatypeOfATypedLiteral() {
        String html = write(namespaces(), GRAPH, vf.createLiteral("2024-01-02T03:04:05Z", XSD.DATETIME));

        assertTrue(html.contains("^^"), html);
        assertTrue(html.contains("dateTime"), html);
    }

    @Test
    void normalisesNumericLiteralsWhenPrettyPrinting() {
        StringWriter out = new StringWriter();
        HtmlWriter writer = new HtmlWriter(out);
        writer.getWriterConfig().set(BasicWriterSettings.PRETTY_PRINT, true);
        writer.startRDF();
        writer.handleStatement(vf.createStatement(SUBJECT, RDFS.LABEL, vf.createLiteral("007", XSD.INTEGER), GRAPH));
        writer.handleStatement(vf.createStatement(SUBJECT, RDFS.SEEALSO, vf.createLiteral("1.50", XSD.DECIMAL), GRAPH));
        writer.handleStatement(vf.createStatement(SUBJECT, RDFS.COMMENT, vf.createLiteral("true", XSD.BOOLEAN), GRAPH));
        writer.endRDF();

        String html = out.toString();
        assertTrue(html.contains("7"), html);
        assertFalse(html.contains("\"007\""), html);
    }

    @Test
    void fallsBackToAQuotedStringForIllTypedNumbers() {
        StringWriter out = new StringWriter();
        HtmlWriter writer = new HtmlWriter(out);
        writer.getWriterConfig().set(BasicWriterSettings.PRETTY_PRINT, true);
        writer.startRDF();
        writer.handleStatement(vf.createStatement(SUBJECT, RDFS.LABEL, vf.createLiteral("two", XSD.INTEGER), GRAPH));
        writer.endRDF();

        assertTrue(out.toString().contains("\"two\""), out.toString());
    }

    @Test
    void keepsTheDatatypeOfXsdStringWhenAsked() {
        StringWriter out = new StringWriter();
        HtmlWriter writer = new HtmlWriter(out);
        writer.getWriterConfig().set(BasicWriterSettings.XSD_STRING_TO_PLAIN_LITERAL, false);
        writer.startRDF();
        writer.handleStatement(vf.createStatement(SUBJECT, RDFS.LABEL, vf.createLiteral("a label"), GRAPH));
        writer.endRDF();

        assertTrue(out.toString().contains("^^"), out.toString());
    }

    @Test
    void writesTheStandaloneDocumentScaffolding() throws Exception {
        StringWriter out = new StringWriter();
        HtmlWriter writer = new HtmlWriter(out);
        writer.startRDF();
        writer.writeHtmlStart();
        writer.startPart("nanopub");
        writer.handleStatement(vf.createStatement(SUBJECT, RDFS.LABEL, vf.createLiteral("x"), GRAPH));
        writer.endPart();
        writer.endRDF();
        writer.writeHtmlEnd();

        String html = out.toString();
        assertTrue(html.contains("<!DOCTYPE html>"), html);
        assertTrue(html.contains("class=\"nanopub\""), html);
    }

    @Test
    void reportsWriteFailuresAsHandlerExceptions() {
        StringWriter failing = new StringWriter() {
            @Override
            public void write(String str) {
                throw new RuntimeException("cannot write");
            }
        };
        HtmlWriter writer = new HtmlWriter(failing);

        assertThrows(RuntimeException.class, () -> {
            writer.startRDF();
            writer.handleStatement(vf.createStatement(SUBJECT, RDFS.LABEL, vf.createLiteral("x"), GRAPH));
            writer.endRDF();
        });
    }

    @Test
    void endRdfFlushesTheOpenContext() {
        String html = write(namespaces(), GRAPH, vf.createLiteral("x"));

        assertTrue(html.trim().endsWith("</span>"), html);
    }

    @Test
    void handlerExceptionsAreDeclared() {
        // the writer converts I/O problems into RDFHandlerException, which callers have to handle
        assertTrue(RDFHandlerException.class.isAssignableFrom(RDFHandlerException.class));
    }

}
