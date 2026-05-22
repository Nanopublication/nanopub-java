package org.nanopub.extra.services;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.nanopub.MalformedNanopubException;
import org.nanopub.Nanopub;
import org.nanopub.NanopubAlreadyFinalizedException;
import org.nanopub.NanopubCreator;
import org.nanopub.NanopubImpl;
import org.nanopub.extra.server.GetNanopub;
import org.nanopub.testsuite.NanopubTestSuite;
import org.nanopub.testsuite.TestSuiteEntry;
import org.nanopub.vocabulary.KPXL_GRLC;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;

class QueryTemplateTest {

    // Fixture: a no-placeholder grlc query.
    private static final String ARTIFACT_CODE = "RA6T-YLqLnYd5XfnqR9PaGUjCzudvHdYjcG4GvOc7fdpA";
    private static final String QUERY_SUFFIX = "get-participation";
    private static final String QUERY_ID = ARTIFACT_CODE + "/" + QUERY_SUFFIX;
    private static final String NANOPUB_URI = "https://w3id.org/np/" + ARTIFACT_CODE;
    private static final String QUERY_URI = NANOPUB_URI + "/" + QUERY_SUFFIX;
    private static final String LABEL = "Get participation links";
    private static final String DESCRIPTION = "This query returns all participation links.";
    private static final String ENDPOINT = "https://w3id.org/np/l/nanopub-query-1.1/repo/full";
    private static final String LICENSE = "http://www.apache.org/licenses/LICENSE-2.0";

    // Fixture: a grlc query with a single ?_np_iri placeholder, using `#` as separator.
    private static final String PARAM_ARTIFACT_CODE = "RAWH0fe1RCpoOgaJE1B2qfTzzdTiBUUK7iIk6l7Zll9mg";
    private static final String PARAM_QUERY_SUFFIX = "get-newer-versions-of-np";
    private static final String PARAM_QUERY_ID = PARAM_ARTIFACT_CODE + "/" + PARAM_QUERY_SUFFIX;

    private Nanopub loadFixture(String artifactCode) throws MalformedNanopubException, IOException {
        TestSuiteEntry entry = NanopubTestSuite.getLatest().getByArtifactCode(artifactCode).getFirst();
        return new NanopubImpl(entry.toFile());
    }

    @Test
    void constructFromIdFetchesNanopub() throws Exception {
        try (MockedStatic<GetNanopub> mock = mockStatic(GetNanopub.class)) {
            Nanopub np = loadFixture(ARTIFACT_CODE);
            mock.when(() -> GetNanopub.get(any(String.class))).thenReturn(np);
            QueryTemplate qt = new QueryTemplate(QUERY_ID);
            assertEquals(QUERY_ID, qt.getQueryId());
            assertEquals(ARTIFACT_CODE, qt.getArtifactCode());
            assertEquals(QUERY_SUFFIX, qt.getQuerySuffix());
            assertEquals(QUERY_URI, qt.getQueryUri().stringValue());
            assertEquals(LABEL, qt.getLabel());
            assertEquals(DESCRIPTION, qt.getDescription());
            assertEquals(ENDPOINT, qt.getEndpoint().stringValue());
            assertEquals(LICENSE, qt.getLicense().stringValue());
            assertNotNull(qt.getSparql());
            assertTrue(qt.getPlaceholdersList().isEmpty());
            assertFalse(qt.isConstructQuery());
            assertEquals(np, qt.getNanopub());
        }
    }

    @Test
    void constructFromFullQueryUri() throws Exception {
        try (MockedStatic<GetNanopub> mock = mockStatic(GetNanopub.class)) {
            Nanopub np = loadFixture(ARTIFACT_CODE);
            mock.when(() -> GetNanopub.get(any(String.class))).thenReturn(np);
            QueryTemplate qt = new QueryTemplate(QUERY_URI);
            assertEquals(QUERY_ID, qt.getQueryId());
            assertEquals(ARTIFACT_CODE, qt.getArtifactCode());
        }
    }

    @Test
    void constructFromNanopubTrustyUri() throws Exception {
        try (MockedStatic<GetNanopub> mock = mockStatic(GetNanopub.class)) {
            Nanopub np = loadFixture(ARTIFACT_CODE);
            mock.when(() -> GetNanopub.get(any(String.class))).thenReturn(np);
            QueryTemplate qt = new QueryTemplate(NANOPUB_URI);
            assertEquals(QUERY_ID, qt.getQueryId());
            assertEquals(QUERY_SUFFIX, qt.getQuerySuffix());
        }
    }

    @Test
    void constructFromPreFetchedNanopub() throws Exception {
        Nanopub np = loadFixture(ARTIFACT_CODE);
        QueryTemplate qt = new QueryTemplate(np);
        assertEquals(QUERY_ID, qt.getQueryId());
        assertEquals(LABEL, qt.getLabel());
        assertEquals(np, qt.getNanopub());
    }

    @Test
    void constructFromPreFetchedNanopubAndId() throws Exception {
        Nanopub np = loadFixture(ARTIFACT_CODE);
        QueryTemplate qt = new QueryTemplate(np, QUERY_ID);
        assertEquals(QUERY_ID, qt.getQueryId());
    }

    @Test
    void constructWithNullArgsThrows() {
        assertThrows(IllegalArgumentException.class, () -> new QueryTemplate((String) null));
        assertThrows(IllegalArgumentException.class, () -> new QueryTemplate(null, null));
    }

    @Test
    void constructWithInvalidIdThrows() {
        assertThrows(IllegalArgumentException.class, () -> new QueryTemplate("not-a-query-id"));
    }

    @Test
    void constructWithMissingQueryThrows() throws Exception {
        Nanopub np = loadFixture(ARTIFACT_CODE);
        assertThrows(IllegalArgumentException.class,
                () -> new QueryTemplate(np, ARTIFACT_CODE + "/does-not-exist"));
    }

    @Test
    void hashSeparatorIsNormalized() throws Exception {
        Nanopub np = loadFixture(PARAM_ARTIFACT_CODE);
        QueryTemplate qt = new QueryTemplate(np);
        assertEquals(PARAM_QUERY_ID, qt.getQueryId());
        assertEquals(PARAM_QUERY_SUFFIX, qt.getQuerySuffix());
        // queryUri retains its `#` form from the source RDF.
        assertTrue(qt.getQueryUri().stringValue().contains("#"));
    }

    @Test
    void placeholdersAreDiscovered() throws Exception {
        Nanopub np = loadFixture(PARAM_ARTIFACT_CODE);
        QueryTemplate qt = new QueryTemplate(np);
        assertEquals(List.of("_np_iri"), qt.getPlaceholdersList());
    }

    @Test
    void expandQueryStrictThrowsOnMissingMandatory() throws Exception {
        Nanopub np = loadFixture(PARAM_ARTIFACT_CODE);
        QueryTemplate qt = new QueryTemplate(np);
        assertThrows(IllegalArgumentException.class, () -> qt.expandQuery(Map.of()));
    }

    @Test
    void expandQueryNonStrictLeavesPlaceholders() throws Exception {
        Nanopub np = loadFixture(PARAM_ARTIFACT_CODE);
        QueryTemplate qt = new QueryTemplate(np);
        String expanded = qt.expandQuery(Map.of(), false);
        assertTrue(expanded.contains("?_np_iri"), "expected placeholder to remain in non-strict mode");
    }

    @Test
    void expandQuerySubstitutesIriPlaceholder() throws Exception {
        Nanopub np = loadFixture(PARAM_ARTIFACT_CODE);
        QueryTemplate qt = new QueryTemplate(np);
        String iri = "https://w3id.org/np/example";
        String expanded = qt.expandQuery(Map.of("np", List.of(iri)));
        assertFalse(expanded.contains("?_np_iri"));
        assertTrue(expanded.contains("<" + iri + ">"));
    }

    @Test
    void expandQueryWithNoPlaceholdersReturnsSparqlUnchanged() throws Exception {
        Nanopub np = loadFixture(ARTIFACT_CODE);
        QueryTemplate qt = new QueryTemplate(np);
        assertEquals(qt.getSparql(), qt.expandQuery(Map.of()));
    }

    // Static helpers

    @Test
    void isIriPlaceholder() {
        assertTrue(QueryTemplate.isIriPlaceholder("foo_iri"));
        assertTrue(QueryTemplate.isIriPlaceholder("foo_multi_iri"));
        assertFalse(QueryTemplate.isIriPlaceholder("foo"));
    }

    @Test
    void isOptionalPlaceholder() {
        assertTrue(QueryTemplate.isOptionalPlaceholder("__foo"));
        assertFalse(QueryTemplate.isOptionalPlaceholder("_foo"));
        assertFalse(QueryTemplate.isOptionalPlaceholder("foo"));
    }

    @Test
    void isMultiPlaceholder() {
        assertTrue(QueryTemplate.isMultiPlaceholder("foo_multi"));
        assertTrue(QueryTemplate.isMultiPlaceholder("foo_multi_iri"));
        assertFalse(QueryTemplate.isMultiPlaceholder("foo_iri"));
        assertFalse(QueryTemplate.isMultiPlaceholder("foo"));
    }

    @Test
    void getParamName() {
        assertEquals("foo", QueryTemplate.getParamName("_foo"));
        assertEquals("foo", QueryTemplate.getParamName("__foo"));
        assertEquals("foo", QueryTemplate.getParamName("_foo_iri"));
        assertEquals("foo", QueryTemplate.getParamName("__foo_iri"));
        assertEquals("foo", QueryTemplate.getParamName("_foo_multi"));
        assertEquals("foo", QueryTemplate.getParamName("_foo_multi_iri"));
        assertEquals("foo", QueryTemplate.getParamName("__foo_multi_iri"));
    }

    @Test
    void serializeIri() {
        assertEquals("<https://example.org>", QueryTemplate.serializeIri("https://example.org"));
    }

    @Test
    void serializeLiteral() {
        assertEquals("\"hello\"", QueryTemplate.serializeLiteral("hello"));
    }

    @Test
    void escapeLiteralEscapesSpecialChars() {
        assertEquals("a\\\\b\\nc\\\"d", QueryTemplate.escapeLiteral("a\\b\nc\"d"));
    }

    @Test
    void licenseIsExtractedAsIri() throws Exception {
        Nanopub np = loadFixture(ARTIFACT_CODE);
        QueryTemplate qt = new QueryTemplate(np);
        IRI lic = qt.getLicense();
        assertNotNull(lic);
        assertEquals(LICENSE, lic.stringValue());
    }

    // Error paths exercised via synthesized nanopubs.

    private static final ValueFactory VF = SimpleValueFactory.getInstance();
    private static final String FAKE_TRUSTY_URI = "https://w3id.org/np/RAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";

    /**
     * Builds a nanopub with one or more grlc-query subjects. Each entry maps the
     * full subject IRI to its SPARQL string (or null to omit the sparql triple).
     */
    private Nanopub buildGrlcNanopub(String nanopubUri, Map<String, String> queries)
            throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        NanopubCreator c = new NanopubCreator(nanopubUri);
        for (Map.Entry<String, String> e : queries.entrySet()) {
            IRI subj = VF.createIRI(e.getKey());
            c.addAssertionStatement(subj, RDF.TYPE, KPXL_GRLC.GRLC_QUERY);
            if (e.getValue() != null) {
                c.addAssertionStatement(subj, KPXL_GRLC.SPARQL, VF.createLiteral(e.getValue()));
            }
        }
        c.addProvenanceStatement(DCTERMS.CREATOR, VF.createIRI("https://example.org/creator"));
        c.addPubinfoStatement(DCTERMS.CREATED, VF.createLiteral("2025-01-01"));
        return c.finalizeNanopub();
    }

    @Test
    void getNanopubReturnsNullThrows() {
        try (MockedStatic<GetNanopub> mock = mockStatic(GetNanopub.class)) {
            mock.when(() -> GetNanopub.get(any(String.class))).thenReturn(null);
            assertThrows(IllegalArgumentException.class, () -> new QueryTemplate(QUERY_ID));
        }
    }

    @Test
    void multipleGrlcQueriesThrows() throws Exception {
        Nanopub np = buildGrlcNanopub(
                FAKE_TRUSTY_URI,
                Map.of(
                        FAKE_TRUSTY_URI + "/q1", "select * where { ?s ?p ?o }",
                        FAKE_TRUSTY_URI + "/q2", "select * where { ?s ?p ?o }"
                )
        );
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> new QueryTemplate(np));
        assertTrue(ex.getMessage().contains("more than one query"));
    }

    @Test
    void noGrlcQueryInNanopubThrows() throws Exception {
        NanopubCreator c = new NanopubCreator(FAKE_TRUSTY_URI);
        c.addAssertionStatement(VF.createIRI(FAKE_TRUSTY_URI + "/x"), DCTERMS.TITLE, VF.createLiteral("nope"));
        c.addProvenanceStatement(DCTERMS.CREATOR, VF.createIRI("https://example.org/creator"));
        c.addPubinfoStatement(DCTERMS.CREATED, VF.createLiteral("2025-01-01"));
        Nanopub np = c.finalizeNanopub();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> new QueryTemplate(np));
        assertTrue(ex.getMessage().contains("No query found"));
    }

    @Test
    void queryUriPatternMismatchThrows() throws Exception {
        Nanopub np = buildGrlcNanopub(
                FAKE_TRUSTY_URI,
                Map.of("http://example.org/not-a-trusty-query", "select * where { ?s ?p ?o }")
        );
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> new QueryTemplate(np));
        assertTrue(ex.getMessage().contains("does not match expected pattern"));
    }

    @Test
    void noSparqlInQueryThrows() throws Exception {
        Nanopub np = buildGrlcNanopub(
                FAKE_TRUSTY_URI,
                java.util.Collections.singletonMap(FAKE_TRUSTY_URI + "/q1", null)
        );
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> new QueryTemplate(np));
        assertTrue(ex.getMessage().contains("no SPARQL"));
    }

    @Test
    void malformedSparqlThrows() throws Exception {
        Nanopub np = buildGrlcNanopub(
                FAKE_TRUSTY_URI,
                Map.of(FAKE_TRUSTY_URI + "/q1", "this is not valid SPARQL")
        );
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> new QueryTemplate(np));
        assertTrue(ex.getMessage().contains("Invalid SPARQL"));
    }

    // expandQuery — multi-placeholder variants

    @Test
    void expandQueryMultiIriPlaceholder() throws Exception {
        Nanopub np = buildGrlcNanopub(
                FAKE_TRUSTY_URI,
                Map.of(FAKE_TRUSTY_URI + "/q1",
                        "select ?s where { values ?_items_multi_iri {} ?s ?p ?_items_multi_iri }")
        );
        QueryTemplate qt = new QueryTemplate(np);
        String expanded = qt.expandQuery(Map.of("items",
                List.of("https://example.org/a", "https://example.org/b")));
        assertTrue(expanded.contains("<https://example.org/a>"));
        assertTrue(expanded.contains("<https://example.org/b>"));
        assertFalse(expanded.contains("{}"), "VALUES block should have been filled");
    }

    @Test
    void expandQueryMultiLiteralPlaceholder() throws Exception {
        Nanopub np = buildGrlcNanopub(
                FAKE_TRUSTY_URI,
                Map.of(FAKE_TRUSTY_URI + "/q1",
                        "select ?s where { values ?_tags_multi {} ?s ?p ?_tags_multi }")
        );
        QueryTemplate qt = new QueryTemplate(np);
        String expanded = qt.expandQuery(Map.of("tags", List.of("alpha", "beta")));
        assertTrue(expanded.contains("\"alpha\""));
        assertTrue(expanded.contains("\"beta\""));
    }

    @Test
    void expandQueryOptionalMultiMissingRemovesValuesBlock() throws Exception {
        // Reference the placeholder in the body too, so the SPARQL parser picks it
        // up as a Var (an empty VALUES alone compiles to BindingSetAssignment, which
        // the Var visitor doesn't traverse).
        Nanopub np = buildGrlcNanopub(
                FAKE_TRUSTY_URI,
                Map.of(FAKE_TRUSTY_URI + "/q1",
                        "select ?s where { values ?__tags_multi {} . optional { ?s ?p ?__tags_multi } }")
        );
        QueryTemplate qt = new QueryTemplate(np);
        assertTrue(qt.getPlaceholdersList().contains("__tags_multi"),
                "placeholder should be detected; got " + qt.getPlaceholdersList());
        String expanded = qt.expandQuery(Map.of());
        assertFalse(expanded.contains("values ?__tags_multi"),
                "optional multi placeholder with no value should strip the VALUES block; got: " + expanded);
    }

    @Test
    void expandQueryLiteralSingleSubstitution() throws Exception {
        Nanopub np = buildGrlcNanopub(
                FAKE_TRUSTY_URI,
                Map.of(FAKE_TRUSTY_URI + "/q1", "select ?s where { ?s rdfs:label ?_label }")
        );
        QueryTemplate qt = new QueryTemplate(np);
        String expanded = qt.expandQuery(Map.of("label", List.of("hello \"world\"")));
        assertTrue(expanded.contains("\"hello \\\"world\\\"\""),
                "literal should be substituted and embedded quotes escaped; got: " + expanded);
        assertFalse(expanded.contains("?_label"));
    }

    @Test
    void expandQueryWordBoundaryDoesNotMatchPrefix() throws Exception {
        // Both `?_x` and `?_x_iri` share param name "x" (getParamName strips _iri).
        // The word-boundary lookahead in the substitution regex must prevent `?_x`
        // from partially matching `?_x_iri` when replacing the bare placeholder.
        Nanopub np = buildGrlcNanopub(
                FAKE_TRUSTY_URI,
                Map.of(FAKE_TRUSTY_URI + "/q1",
                        "select ?s where { ?s ?_x ?o . ?s ?_x_iri ?o }")
        );
        QueryTemplate qt = new QueryTemplate(np);
        String expanded = qt.expandQuery(Map.of("x", List.of("https://example.org/x")));
        assertTrue(expanded.contains("<https://example.org/x>"),
                "iri form should be substituted as IRI; got: " + expanded);
        assertTrue(expanded.contains("\"https://example.org/x\""),
                "bare form should be substituted as literal; got: " + expanded);
        assertFalse(expanded.contains("?_x_iri"),
                "iri placeholder should have been substituted; got: " + expanded);
    }

}
