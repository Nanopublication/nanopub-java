package org.nanopub.extra.services;

import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.parser.sparql.SPARQLParser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Checks SPARQL query strings for syntax errors, and describes what is wrong in terms the author of
 * the query can act on.
 * <p>
 * The parser used here is the one {@link QueryTemplate} runs, so a query that passes this check is a
 * query that can later be executed.
 */
public class SparqlSyntax {

    private SparqlSyntax() {
    }  // no instances allowed

    private static final Pattern positionPattern = Pattern.compile("line (\\d+), column (\\d+)");

    /**
     * Checks whether the given string is a syntactically valid SPARQL query.
     *
     * @param sparql the query string to check
     * @return true if the query parses
     */
    public static boolean isValid(String sparql) {
        return getSyntaxError(sparql) == null;
    }

    /**
     * Describes what keeps the given string from being a valid SPARQL query.
     * <p>
     * Where the parser points at a non-ASCII character, that character is named. Queries are more often
     * broken by a character picked up on the way through a word processor or a web page than by a
     * hand-written syntax error, and the parser reports such a character as a bare number, which leaves
     * the author none the wiser: a no-break space looks exactly like the space it replaced.
     *
     * @param sparql the query string to check
     * @return a description of the syntax error, or null if the query parses (or is null)
     */
    public static String getSyntaxError(String sparql) {
        if (sparql == null) return null;
        try {
            new SPARQLParser().parseQuery(sparql, null);
            return null;
        } catch (MalformedQueryException ex) {
            return describe(sparql, ex.getMessage());
        }
    }

    private static String describe(String sparql, String parserMessage) {
        String description = "This is not valid SPARQL. The SPARQL parser reports: " + summarize(parserMessage);
        String character = describeCharacterAt(sparql, parserMessage);
        if (character != null) {
            description += " " + character;
        }
        return description;
    }

    /**
     * Reduces the parser's report to its first line, which says what was encountered and where. What
     * follows is the list of everything the parser would have accepted instead, which runs to dozens of
     * lines and does not survive being quoted in an error message.
     */
    private static String summarize(String parserMessage) {
        if (parserMessage == null) return "(no details given)";
        String firstLine = parserMessage.split("\\R", 2)[0].replaceAll("\\s+", " ").trim();
        return firstLine.replaceAll("[,.]+$", "") + ".";
    }

    /**
     * Names the character the parser points at, if it is not an ASCII one. For ASCII characters the
     * parser's own report already shows what is there.
     */
    private static String describeCharacterAt(String sparql, String parserMessage) {
        if (parserMessage == null) return null;
        Matcher m = positionPattern.matcher(parserMessage);
        if (!m.find()) return null;
        int line = Integer.parseInt(m.group(1));
        int column = Integer.parseInt(m.group(2));
        String[] lines = sparql.split("\\R", -1);
        if (line < 1 || line > lines.length || column < 1 || column > lines[line - 1].length()) return null;
        int codePoint = lines[line - 1].codePointAt(column - 1);
        if (codePoint < 128) return null;
        String name = Character.getName(codePoint);
        return "The character in that position is " + String.format("U+%04X", codePoint) +
                (name == null ? "" : " (" + name + ")") + ", which SPARQL doesn't allow there. " +
                "Characters like this one tend to slip in when a query is copied from a word processor " +
                "or a web page, and replacing them with their plain equivalents makes the query valid again.";
    }

}
