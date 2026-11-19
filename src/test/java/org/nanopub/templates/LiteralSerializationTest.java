package org.nanopub.templates;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Serialization and parsing of the literal form used by the publish form's value fields
 * (nt:ValuePlaceholder). The long-literal cases guard the stack overflow that a
 * regex-based implementation caused for SVG-sized values (nanodash#634).
 */
class LiteralSerializationTest {

    private static String longSvgLiteral() {
        StringBuilder sb = new StringBuilder("\"<svg xmlns=\\\"http://www.w3.org/2000/svg\\\" viewBox=\\\"0 0 1029 478\\\">");
        for (int i = 0; i < 400; i++) {
            sb.append("<path d=\\\"M0.074,-0L0.074,-0.657L0.285,-0.657Z\\\" style=\\\"fill:rgb(63,63,63);\\\"/>");
        }
        return sb.append("</svg>\"").toString();
    }

    @Test
    void plainLiteralRoundTrips() {
        Literal l = LiteralSerialization.parse("\"hello\"");
        assertEquals("hello", l.stringValue());
        assertTrue(LiteralSerialization.isValid("\"hello\""));
    }

    @Test
    void languageTaggedAndTypedLiteralsRoundTrip() {
        assertEquals("de", LiteralSerialization.parse("\"Haus\"@de").getLanguage().orElse(null));
        Literal typed = LiteralSerialization.parse("\"42\"^^<http://www.w3.org/2001/XMLSchema#integer>");
        assertEquals("42", typed.stringValue());
        assertEquals("http://www.w3.org/2001/XMLSchema#integer", typed.getDatatype().stringValue());
    }

    @Test
    void escapedQuotesAndBackslashesRoundTrip() {
        String value = "a \" b \\ c";
        String serialized = "\"" + LiteralSerialization.escape(value) + "\"";
        // The escaping must survive validation — an unescaped inner quote would end the
        // literal early and make the rest unparseable.
        assertTrue(LiteralSerialization.isValid(serialized), serialized);
        assertEquals(value, LiteralSerialization.parse(serialized).stringValue());
    }

    @Test
    void longSvgLiteralIsValidatedWithoutOverflowing() {
        String serialized = longSvgLiteral();
        assertTrue(serialized.length() > 20_000);
        assertTrue(LiteralSerialization.isValid(serialized));
        assertTrue(LiteralSerialization.parse(serialized).stringValue().startsWith("<svg "));
    }

    @Test
    void longSvgLiteralWithLanguageTagAndDatatypeIsValidated() {
        String body = longSvgLiteral();
        assertTrue(LiteralSerialization.isValid(body + "@en"));
        assertTrue(LiteralSerialization.isValid(body + "^^<http://www.w3.org/2001/XMLSchema#string>"));
    }

    @Test
    void malformedSerializationsAreRejected() {
        assertFalse(LiteralSerialization.isValid("no quotes"));
        assertFalse(LiteralSerialization.isValid("\"unterminated"));
        assertFalse(LiteralSerialization.isValid("\"inner \" quote\""));
        assertFalse(LiteralSerialization.isValid("\"text\" trailing"));
        assertFalse(LiteralSerialization.isValid("\"bad escape \\q\""));
        assertThrows(IllegalArgumentException.class, () -> LiteralSerialization.parse("no quotes"));
    }

    /**
     * A value field is a single line, which a browser strips line breaks from, so a literal
     * with several lines is shown with its newlines escaped and comes back unchanged
     * (nanodash#103).
     */
    @Test
    void multiLineLiteralIsSerializedOnOneLineAndRoundTrips() {
        Literal multiLine = SimpleValueFactory.getInstance().createLiteral("first line\nsecond line\r\nthird\tline", "en");
        String serialized = LiteralSerialization.serialize(multiLine);
        assertEquals("\"first line\\nsecond line\\r\\nthird\\tline\"@en", serialized);
        assertTrue(LiteralSerialization.isValid(serialized), serialized);
        assertEquals(multiLine, LiteralSerialization.parse(serialized));
    }

    /**
     * A newline typed as {@code \n} into a value field becomes a real newline in the literal.
     */
    @Test
    void typedNewlineEscapeBecomesANewline() {
        assertEquals("a\nb", LiteralSerialization.parse("\"a\\nb\"").stringValue());
    }

    /**
     * An escaped backslash before an {@code n} stays a backslash and a letter.
     */
    @Test
    void escapedBackslashBeforeNIsNotANewline() {
        assertEquals("C:\\new", LiteralSerialization.parse("\"C:\\\\new\"").stringValue());
        assertEquals("\"C:\\\\new\"", LiteralSerialization.serialize(SimpleValueFactory.getInstance().createLiteral("C:\\new")));
    }

    /**
     * Every short escape Turtle defines is accepted, the single quote included.
     */
    @Test
    void allTurtleShortEscapesAreAccepted() {
        String serialized = "\"\\t\\b\\n\\r\\f\\\"\\'\\\\\"";
        assertTrue(LiteralSerialization.isValid(serialized));
        assertEquals("\t\b\n\r\f\"'\\", LiteralSerialization.parse(serialized).stringValue());
    }

    /**
     * A literal whose text still holds a raw line break, as a prefilled page parameter may,
     * is accepted and keeps it.
     */
    @Test
    void rawNewlineInsideTheQuotesIsKept() {
        assertTrue(LiteralSerialization.isValid("\"a\nb\""));
        assertEquals("a\nb", LiteralSerialization.parse("\"a\nb\"").stringValue());
    }

}
