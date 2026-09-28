package org.nanopub.extra.services;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SparqlSyntaxTest {

    private static final String VALID = "SELECT ?x WHERE { ?x ?y ?z }";

    @Test
    void acceptsAValidQuery() {
        assertTrue(SparqlSyntax.isValid(VALID));
        assertNull(SparqlSyntax.getSyntaxError(VALID));
    }

    @Test
    void acceptsAQueryWithPlaceholders() {
        assertTrue(SparqlSyntax.isValid("SELECT ?x WHERE { ?x ?y ?_placeholder }"));
    }

    @Test
    void rejectsAnEmptyQuery() {
        assertFalse(SparqlSyntax.isValid(""));
        assertTrue(SparqlSyntax.getSyntaxError("").startsWith("This is not valid SPARQL."));
    }

    @Test
    void ignoresANullQuery() {
        assertTrue(SparqlSyntax.isValid(null));
        assertNull(SparqlSyntax.getSyntaxError(null));
    }

    @Test
    void namesANoBreakSpace() {
        String error = SparqlSyntax.getSyntaxError("SELECT ?x\nWHERE {\u00A0?x ?y ?z }");

        assertNotNull(error);
        assertTrue(error.contains("U+00A0 (NO-BREAK SPACE)"), error);
        assertTrue(error.contains("line 2, column 8"), error);
        assertTrue(error.contains("word processor"), error);
    }

    @Test
    void namesATypographicQuote() {
        String error = SparqlSyntax.getSyntaxError("SELECT ?x WHERE { ?x ?y “hello” }");

        assertNotNull(error);
        assertTrue(error.contains("U+201C (LEFT DOUBLE QUOTATION MARK)"), error);
    }

    @Test
    void doesNotNameAnAsciiCharacter() {
        // the parser's own report already shows what is there
        String error = SparqlSyntax.getSyntaxError("SELECT ?x WHERE { ?x ?y }");

        assertNotNull(error);
        assertFalse(error.contains("The character in that position"), error);
        assertTrue(error.contains("line 1, column 25"), error);
    }

    @Test
    void keepsTheParserReportToASingleLine() {
        // the parser follows its complaint with a list of everything it would have accepted instead,
        // which runs to dozens of lines
        String error = SparqlSyntax.getSyntaxError("SELECT ?x WHERE { ?x ?y }");

        assertFalse(error.contains("\n"), error);
        assertFalse(error.contains("Was expecting"), error);
    }

}
