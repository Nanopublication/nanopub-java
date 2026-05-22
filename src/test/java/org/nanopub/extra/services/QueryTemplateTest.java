package org.nanopub.extra.services;

import org.eclipse.rdf4j.model.IRI;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.nanopub.MalformedNanopubException;
import org.nanopub.Nanopub;
import org.nanopub.NanopubImpl;
import org.nanopub.extra.server.GetNanopub;
import org.nanopub.testsuite.NanopubTestSuite;
import org.nanopub.testsuite.TestSuiteEntry;

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

}
