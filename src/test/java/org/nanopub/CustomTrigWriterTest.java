package org.nanopub;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CustomTrigWriterTest {

    private static final ValueFactory vf = SimpleValueFactory.getInstance();
    private static final IRI GRAPH = vf.createIRI("https://example.org/graph");

    /**
     * Writes a single statement about {@code subject} and returns the resulting TriG document.
     */
    private static String write(Map<String, String> namespaces, IRI subject, org.eclipse.rdf4j.model.Value object) {
        StringWriter out = new StringWriter();
        CustomTrigWriter writer = new CustomTrigWriter(out);
        writer.startRDF();
        namespaces.forEach(writer::handleNamespace);
        writer.handleStatement(vf.createStatement(subject, RDFS.LABEL, object, GRAPH));
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
    void constructorsAcceptStreamsAndWriters() {
        assertNotNull(new CustomTrigWriter(new ByteArrayOutputStream()));
        assertNotNull(new CustomTrigWriter(new StringWriter()));
        assertNotNull(new CustomTrigWriter(new ByteArrayOutputStream(), new HashSet<>()));
        assertNotNull(new CustomTrigWriter(new StringWriter(), new HashSet<>()));
        assertNotNull(new CustomTrigWriter(new HashSet<>()));
    }

    @Test
    void writesToAnOutputStream() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        CustomTrigWriter writer = new CustomTrigWriter(out);
        writer.startRDF();
        writer.handleNamespace("ex", "https://example.org/");
        writer.handleStatement(vf.createStatement(vf.createIRI("https://example.org/thing"), RDFS.LABEL,
                vf.createLiteral("a label"), GRAPH));
        writer.endRDF();

        assertTrue(out.toString(StandardCharsets.UTF_8).contains("ex:thing"));
    }

    @Test
    void abbreviatesUrisWithAKnownPrefix() {
        String out = write(namespaces("ex", "https://example.org/"),
                vf.createIRI("https://example.org/thing"), vf.createLiteral("x"));

        assertTrue(out.contains("ex:thing"), out);
    }

    @Test
    void abbreviatesAUriThatIsExactlyTheNamespace() {
        String out = write(namespaces("ex", "https://example.org/thing"),
                vf.createIRI("https://example.org/thing"), vf.createLiteral("x"));

        // the namespace declaration mentions the full URI, so only the body is of interest here
        String body = withoutPrefixDeclarations(out);
        assertTrue(body.contains("ex:"), body);
        assertFalse(body.contains("<https://example.org/thing>"), body);
    }

    private static String withoutPrefixDeclarations(String trig) {
        return trig.lines().filter(line -> !line.startsWith("@prefix")).reduce("", (a, b) -> a + b + "\n");
    }

    @Test
    void writesTheFullUriWhenNoPrefixMatches() {
        String out = write(namespaces("ex", "https://example.org/"),
                vf.createIRI("https://other.example.com/thing"), vf.createLiteral("x"));

        assertTrue(out.contains("<https://other.example.com/thing>"), out);
    }

    @Test
    void writesTheFullUriWhenItEndsWithADot() {
        String out = write(namespaces("ex", "https://example.org/"),
                vf.createIRI("https://example.org/thing."), vf.createLiteral("x"));

        assertTrue(out.contains("<https://example.org/thing.>"), out);
    }

    @Test
    void splitsAtTheLastDot() {
        String out = write(namespaces("ex", "https://example.org/np.RA1234."),
                vf.createIRI("https://example.org/np.RA1234.thing"), vf.createLiteral("x"));

        assertTrue(out.contains("ex:thing"), out);
    }

    @Test
    void splitsAtTheLastColon() {
        String out = write(namespaces("ex", "https://example.org/id:"),
                vf.createIRI("https://example.org/id:thing"), vf.createLiteral("x"));

        assertTrue(out.contains("ex:thing"), out);
    }

    @Test
    void doesNotSplitAtATrailingColon() {
        String out = write(namespaces("ex", "https://example.org/"),
                vf.createIRI("https://example.org/thing:"), vf.createLiteral("x"));

        assertTrue(out.contains("ex:thing:"), out);
    }

    @Test
    void doesNotSplitAtATrailingUnderscore() {
        String out = write(namespaces("ex", "https://example.org/"),
                vf.createIRI("https://example.org/thing_"), vf.createLiteral("x"));

        assertTrue(out.contains("ex:thing_"), out);
    }

    @Test
    void keepsTheOriginalSplitWhenTheColonNamespaceIsUnknown() {
        String out = write(namespaces("ex", "https://example.org/"),
                vf.createIRI("https://example.org/a:b"), vf.createLiteral("x"));

        assertTrue(out.contains("ex:a:b"), out);
    }

    @Test
    void splitsAtTheLastUnderscore() {
        String out = write(namespaces("ex", "https://example.org/id_"),
                vf.createIRI("https://example.org/id_thing"), vf.createLiteral("x"));

        assertTrue(out.contains("ex:thing"), out);
    }

    @Test
    void splitsBeforeAHashSign() {
        String out = write(namespaces("ex", "https://example.org/np1"),
                vf.createIRI("https://example.org/np1#thing"), vf.createLiteral("x"));

        assertTrue(out.contains("ex:\\#thing"), out);
    }

    @Test
    void keepsThePostHashPrefixWhenThereIsOne() {
        String out = write(namespaces("pre", "https://example.org/np1", "post", "https://example.org/np1#"),
                vf.createIRI("https://example.org/np1#thing"), vf.createLiteral("x"));

        assertTrue(out.contains("post:thing"), out);
    }

    @Test
    void collectsTheUsedPrefixes() {
        Set<String> usedPrefixes = new HashSet<>();
        CustomTrigWriter writer = new CustomTrigWriter(usedPrefixes);
        writer.startRDF();
        writer.handleNamespace("ex", "https://example.org/");
        writer.handleNamespace("unused", "https://unused.example.org/");
        writer.handleStatement(vf.createStatement(vf.createIRI("https://example.org/thing"), RDFS.LABEL,
                vf.createLiteral("x"), GRAPH));
        writer.endRDF();

        assertTrue(usedPrefixes.contains("ex"));
        assertFalse(usedPrefixes.contains("unused"));
    }

    @Test
    void writesAPlainStringLiteralWithoutADatatype() {
        String out = write(namespaces(), vf.createIRI("https://example.org/thing"), vf.createLiteral("a label"));

        assertTrue(out.contains("\"a label\""), out);
        assertFalse(out.contains("^^"), out);
    }

    @Test
    void writesMultiLineLiteralsAsLongStrings() {
        String out = write(namespaces(), vf.createIRI("https://example.org/thing"),
                vf.createLiteral("first\nsecond"));

        assertTrue(out.contains("\"\"\""), out);
    }

    @Test
    void writesLiteralsWithCarriageReturnsAsLongStrings() {
        String out = write(namespaces(), vf.createIRI("https://example.org/thing"),
                vf.createLiteral("first\rsecond"));

        assertTrue(out.contains("\"\"\""), out);
    }

    @Test
    void writesLiteralsWithTabsAsLongStrings() {
        String out = write(namespaces(), vf.createIRI("https://example.org/thing"),
                vf.createLiteral("first\tsecond"));

        assertTrue(out.contains("\"\"\""), out);
    }

    @Test
    void writesTheLanguageOfALanguageLiteral() {
        String out = write(namespaces(), vf.createIRI("https://example.org/thing"),
                vf.createLiteral("een label", "nl"));

        assertTrue(out.contains("\"een label\"@nl"), out);
    }

    @Test
    void writesTheDatatypeOfATypedLiteral() {
        String out = write(namespaces(), vf.createIRI("https://example.org/thing"),
                vf.createLiteral("42", XSD.INTEGER));

        assertTrue(out.contains("^^"), out);
        assertTrue(out.contains("integer"), out);
    }

    @Test
    void doesNotNormaliseLiterals() {
        // the writer deliberately keeps the lexical form as it is, rather than pretty-printing it
        Statement statement = vf.createStatement(vf.createIRI("https://example.org/thing"), RDFS.LABEL,
                vf.createLiteral("007", XSD.INTEGER), GRAPH);
        StringWriter out = new StringWriter();
        CustomTrigWriter writer = new CustomTrigWriter(out);
        writer.startRDF();
        writer.handleStatement(statement);
        writer.endRDF();

        assertTrue(out.toString().contains("\"007\""), out.toString());
    }

}
