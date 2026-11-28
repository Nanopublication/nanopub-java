package org.nanopub.templates;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Literals;
import org.eclipse.rdf4j.model.vocabulary.XSD;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The text form of a literal where a value can be either an IRI or a literal, as for a value
 * placeholder: a quoted string in Turtle syntax, optionally followed by a language tag or a
 * datatype IRI, such as {@code "text"}, {@code "text"@en} or
 * {@code "5"^^<http://www.w3.org/2001/XMLSchema#integer>}.
 */
public final class LiteralSerialization {

    private LiteralSerialization() {
    }

    private static final ValueFactory vf = SimpleValueFactory.getInstance();

    // The part after the quoted string: a language tag, or a datatype IRI. The quoted string
    // itself is scanned rather than matched (see scanQuotedString): expressed as a regex it
    // costs one stack frame per character, so a literal of a few thousand characters would
    // overflow the stack.
    private static final Pattern LANGTAG_SUFFIX = Pattern.compile("^@([0-9a-zA-Z-]{2,})$");
    private static final Pattern DATATYPE_SUFFIX = Pattern.compile("^\\^\\^<([^ ><\"^]+)>$");

    // The characters a literal string escapes, each with the letter that follows the
    // backslash for it: Turtle's short string escapes.
    private static final Map<Character, Character> ESCAPE_LETTERS = Map.of(
            '\\', '\\',
            '"', '"',
            '\n', 'n',
            '\r', 'r',
            '\t', 't',
            '\b', 'b',
            '\f', 'f');

    // The letters that may follow a backslash, each with the character it stands for: the
    // inverse of the above, plus the single quote, which Turtle allows to be escaped but
    // which never needs it here.
    private static final Map<Character, Character> ESCAPED_CHARS = Map.of(
            '\\', '\\',
            '"', '"',
            '\'', '\'',
            'n', '\n',
            'r', '\r',
            't', '\t',
            'b', '\b',
            'f', '\f');

    /**
     * Checks whether a text is the text form of a literal.
     *
     * @param text the text to check
     * @return true if {@link #parse(String)} accepts the text
     */
    public static boolean isValid(String text) {
        if (text == null) return false;
        int end = scanQuotedString(text);
        if (end < 0) return false;
        String suffix = text.substring(end);
        return suffix.isEmpty()
                || LANGTAG_SUFFIX.matcher(suffix).matches()
                || DATATYPE_SUFFIX.matcher(suffix).matches();
    }

    /**
     * Writes a literal in its text form.
     *
     * @param literal the literal
     * @return the text form
     */
    public static String serialize(Literal literal) {
        String quoted = "\"" + escape(literal.stringValue()) + "\"";
        if (literal.getLanguage().isPresent()) {
            return quoted + "@" + Literals.normalizeLanguageTag(literal.getLanguage().get());
        } else if (literal.getDatatype().equals(XSD.STRING)) {
            return quoted;
        } else {
            return quoted + "^^<" + literal.getDatatype() + ">";
        }
    }

    /**
     * Reads a literal from its text form.
     *
     * @param text the text form
     * @return the literal
     * @throws IllegalArgumentException if the text is not the text form of a literal
     */
    public static Literal parse(String text) {
        int end = text == null ? -1 : scanQuotedString(text);
        if (end >= 0) {
            String value = unescape(text.substring(1, end - 1));
            String suffix = text.substring(end);
            if (suffix.isEmpty()) return vf.createLiteral(value);
            Matcher langtag = LANGTAG_SUFFIX.matcher(suffix);
            if (langtag.matches()) return vf.createLiteral(value, langtag.group(1));
            Matcher datatype = DATATYPE_SUFFIX.matcher(suffix);
            if (datatype.matches()) return vf.createLiteral(value, vf.createIRI(datatype.group(1)));
        }
        throw new InvalidRawValueException(Violation.Kind.MALFORMED_LITERAL, "Not a valid literal serialization: " + text);
    }

    /**
     * Escapes a string the way Turtle does: backslashes, quotes and the control characters
     * that have a short escape.
     *
     * @param unescaped the string
     * @return the escaped string
     */
    public static String escape(String unescaped) {
        StringBuilder escaped = new StringBuilder(unescaped.length());
        for (int i = 0; i < unescaped.length(); i++) {
            char c = unescaped.charAt(i);
            Character escapeLetter = ESCAPE_LETTERS.get(c);
            if (escapeLetter == null) {
                escaped.append(c);
            } else {
                escaped.append('\\').append(escapeLetter.charValue());
            }
        }
        return escaped.toString();
    }

    /**
     * Reverses {@link #escape(String)}. A backslash before a character that has no escape is
     * kept as it is.
     *
     * @param escaped the escaped string
     * @return the string
     */
    public static String unescape(String escaped) {
        StringBuilder unescaped = new StringBuilder(escaped.length());
        for (int i = 0; i < escaped.length(); i++) {
            char c = escaped.charAt(i);
            Character escapedChar = null;
            if (c == '\\' && i + 1 < escaped.length()) escapedChar = ESCAPED_CHARS.get(escaped.charAt(i + 1));
            if (escapedChar == null) {
                unescaped.append(c);
            } else {
                unescaped.append(escapedChar.charValue());
                i++;
            }
        }
        return unescaped.toString();
    }

    // Scans a leading quoted string, in which a backslash starts one of Turtle's string
    // escapes, and returns the index just past its closing quote, or -1 if the text does not
    // start with a well-formed quoted string.
    private static int scanQuotedString(String s) {
        if (s.isEmpty() || s.charAt(0) != '"') return -1;
        int i = 1;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == '\\') {
                if (i + 1 >= s.length()) return -1;
                if (!ESCAPED_CHARS.containsKey(s.charAt(i + 1))) return -1;
                i += 2;
            } else if (c == '"') {
                return i + 1;
            } else {
                i++;
            }
        }
        return -1;
    }

}
