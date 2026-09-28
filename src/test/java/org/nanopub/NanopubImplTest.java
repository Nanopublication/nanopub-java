package org.nanopub;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.impl.MapBindingSet;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.junit.jupiter.api.AfterAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.MockedStatic;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import org.nanopub.testsuite.NanopubTestSuite;
import org.nanopub.testsuite.TestSuiteCategory;
import org.nanopub.testsuite.TestSuiteEntry;
import org.nanopub.trusty.TrustyNanopubUtils;
import org.nanopub.utils.TestUtils;
import org.nanopub.vocabulary.NP;
import org.nanopub.vocabulary.PAV;

import net.trustyuri.ArtifactCode;
import net.trustyuri.TrustyUriUtils;

class NanopubImplTest {

    private static CloseableHttpClient mockHttpClient;

    @BeforeAll
    static void setUp() throws IOException {
        mockHttpClient = mock();
        when(mockHttpClient.execute(any(HttpGet.class))).thenAnswer(invocation -> {
            HttpGet request = invocation.getArgument(0);
            URI requestUri = request.getURI();
            String npId = TrustyUriUtils.getArtifactCode(requestUri.getPath());
            CloseableHttpResponse mockResponse = mock(CloseableHttpResponse.class);
            HttpEntity mockEntity = mock(HttpEntity.class);
            when(mockResponse.getEntity()).thenReturn(mockEntity);
            when(mockResponse.getEntity()).thenReturn(mockEntity);
            TestSuiteEntry entry = NanopubTestSuite.getLatest().getByArtifactCode(npId).getFirst();
            when(mockEntity.getContent()).thenReturn(new FileInputStream(entry.toFile()));
            when(mockResponse.getStatusLine()).thenReturn(mock(StatusLine.class));
            when(mockResponse.getStatusLine().getStatusCode()).thenReturn(200);
            when(mockResponse.getFirstHeader("Content-Type")).thenReturn(mock(Header.class));
            when(mockResponse.getFirstHeader("Content-Type").getValue()).thenReturn("application/trig");
            return mockResponse;
        });
    }

    @AfterAll
    static void tearDown() {
        if (mockHttpClient != null) {
            try {
                mockHttpClient.close();
            } catch (IOException e) {
                throw new RuntimeException("Failed to close HttpClient", e);
            }
        }
    }

    @Test
    void equalsTextBlockWithSameLineSeparator() throws URISyntaxException, MalformedNanopubException, IOException {
        try (MockedStatic<NanopubUtils> nanopubUtilsMock = mockStatic(NanopubUtils.class, CALLS_REAL_METHODS)) {
            nanopubUtilsMock.when(NanopubUtils::getHttpClient).thenReturn(mockHttpClient);

            final ArtifactCode artifactCode = ArtifactCode.of("RA6T-YLqLnYd5XfnqR9PaGUjCzudvHdYjcG4GvOc7fdpA");
            NanopubImpl nanopub1 = new NanopubImpl(new URI("https://w3id.org/np/" + artifactCode).toURL());
            NanopubImpl nanopub2 = new NanopubImpl(NanopubTestSuite.getLatest()
                    .getByArtifactCode("RA6T-YLqLnYd5XfnqR9PaGUjCzudvHdYjcG4GvOc7fdpA", TestSuiteCategory.VALID)
                    .orElseThrow(() -> new IllegalStateException("Nanopub with artifact code " + artifactCode + " not found in test suite"))
                    .toFile(), RDFFormat.TRIG);

            assertTrue(TrustyNanopubUtils.isValidTrustyNanopub(nanopub1));
            assertTrue(TrustyNanopubUtils.isValidTrustyNanopub(nanopub2));
            assertEquals(nanopub1, nanopub2);
        }
    }

    @Test
    void equalsTextBlockWithDifferentLineSeparator() throws IOException, MalformedNanopubException, URISyntaxException {
        try (MockedStatic<NanopubUtils> nanopubUtilsMock = mockStatic(NanopubUtils.class, CALLS_REAL_METHODS)) {
            nanopubUtilsMock.when(NanopubUtils::getHttpClient).thenReturn(mockHttpClient);

            final ArtifactCode artifactCode = ArtifactCode.of("RA6T-YLqLnYd5XfnqR9PaGUjCzudvHdYjcG4GvOc7fdpA");
            NanopubImpl nanopub1 = new NanopubImpl(new URI("https://w3id.org/np/" + artifactCode).toURL());
            NanopubImpl nanopub2 = new NanopubImpl(NanopubTestSuite.getLatest()
                    .getByArtifactCode(artifactCode.toString(), TestSuiteCategory.INVALID)
                    .orElseThrow(() -> new IllegalStateException("Nanopub with artifact code " + artifactCode + " not found in test suite"))
                    .toFile(), RDFFormat.TRIG);

            assertTrue(TrustyNanopubUtils.isValidTrustyNanopub(nanopub1));
            assertFalse(TrustyNanopubUtils.isValidTrustyNanopub(nanopub2));
            assertNotEquals(nanopub1, nanopub2);
        }
    }

    // ---------------------------------------------------------------- helpers
    private static final ValueFactory vf = SimpleValueFactory.getInstance();

    private static final String NP_URI = "https://example.org/np1#";
    private static final IRI NP_ID = vf.createIRI(NP_URI);
    // all four graph URIs are of the same length, which keeps the byte count stable
    // when a statement is moved from one graph to another
    private static final IRI HEAD = vf.createIRI(NP_URI + "g1");
    private static final IRI ASSERTION = vf.createIRI(NP_URI + "g2");
    private static final IRI PROVENANCE = vf.createIRI(NP_URI + "g3");
    private static final IRI PUBINFO = vf.createIRI(NP_URI + "g4");

    private static final String NANOPUB_TRIG = """
            @prefix this: <https://example.org/np1#> .
            @prefix np: <http://www.nanopub.org/nschema#> .
            @prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
            this:g1 {
                this: a np:Nanopublication ;
                    np:hasAssertion this:g2 ;
                    np:hasProvenance this:g3 ;
                    np:hasPublicationInfo this:g4 .
            }
            this:g2 { this:g2 rdfs:label "assertion" . }
            this:g3 { this:g2 rdfs:label "provenance" . }
            this:g4 { this: rdfs:label "pubinfo" . }
            """;

    private static List<Statement> headStatements() {
        List<Statement> statements = new ArrayList<>();
        statements.add(vf.createStatement(NP_ID, RDF.TYPE, NP.NANOPUBLICATION, HEAD));
        statements.add(vf.createStatement(NP_ID, NP.HAS_ASSERTION, ASSERTION, HEAD));
        statements.add(vf.createStatement(NP_ID, NP.HAS_PROVENANCE, PROVENANCE, HEAD));
        statements.add(vf.createStatement(NP_ID, NP.HAS_PUBINFO, PUBINFO, HEAD));
        return statements;
    }

    private static List<Statement> validStatements() {
        List<Statement> statements = headStatements();
        statements.add(vf.createStatement(ASSERTION, RDFS.LABEL, vf.createLiteral("a"), ASSERTION));
        statements.add(vf.createStatement(ASSERTION, RDFS.LABEL, vf.createLiteral("p"), PROVENANCE));
        statements.add(vf.createStatement(NP_ID, RDFS.LABEL, vf.createLiteral("i"), PUBINFO));
        return statements;
    }

    private static String malformedNanopubMessage(Collection<Statement> statements) {
        return assertThrows(MalformedNanopubException.class, () -> new NanopubImpl(statements)).getMessage();
    }

    // ------------------------------------------------- construction and parsing
    @Test
    void constructFromStatements() throws MalformedNanopubException {
        NanopubImpl nanopub = new NanopubImpl(validStatements());

        assertEquals(NP_ID, nanopub.getUri());
        assertEquals(HEAD, nanopub.getHeadUri());
        assertEquals(ASSERTION, nanopub.getAssertionUri());
        assertEquals(PROVENANCE, nanopub.getProvenanceUri());
        assertEquals(PUBINFO, nanopub.getPubinfoUri());
        assertEquals(Set.of(HEAD, ASSERTION, PROVENANCE, PUBINFO), nanopub.getGraphUris());
        assertEquals(4, nanopub.getHead().size());
        assertEquals(1, nanopub.getAssertion().size());
        assertEquals(1, nanopub.getProvenance().size());
        assertEquals(1, nanopub.getPubinfo().size());
        assertEquals(7, nanopub.getTripleCount());
        assertTrue(nanopub.getByteCount() > 0);
        assertFalse(nanopub.isValidAndTrusty());
        assertTrue(nanopub.getNsPrefixes().isEmpty());
        assertTrue(nanopub.getNs().isEmpty());
        assertNull(nanopub.getNamespace("ex"));
    }

    @Test
    void constructFromStatementsAndNamespacePairs() throws MalformedNanopubException {
        NanopubImpl nanopub = new NanopubImpl(validStatements(), List.of(Pair.of("ex", "https://example.org/")));

        assertEquals(List.of("ex"), nanopub.getNsPrefixes());
        assertEquals("https://example.org/", nanopub.getNamespace("ex"));
    }

    @Test
    void constructFromString() throws MalformedNanopubException {
        NanopubImpl nanopub = new NanopubImpl(NANOPUB_TRIG, RDFFormat.TRIG);

        assertEquals(NP_ID, nanopub.getUri());
        assertTrue(nanopub.getNsPrefixes().contains("np"));
        assertEquals("http://www.nanopub.org/nschema#", nanopub.getNamespace("np"));
    }

    @Test
    void constructFromStringWrapsIoExceptionsOfTheParser() throws IOException {
        RDFParser parser = mock(RDFParser.class);
        doThrow(new IOException("cannot read")).when(parser).parse(any(Reader.class));

        try (MockedStatic<NanopubUtils> nanopubUtilsMock = mockStatic(NanopubUtils.class, CALLS_REAL_METHODS)) {
            nanopubUtilsMock.when(() -> NanopubUtils.getParser(RDFFormat.TRIG)).thenReturn(parser);

            RuntimeException ex = assertThrows(RuntimeException.class, () -> new NanopubImpl(NANOPUB_TRIG, RDFFormat.TRIG));
            assertEquals("Unexpected IOException", ex.getMessage());
            assertInstanceOf(IOException.class, ex.getCause());
        }
    }

    @Test
    void ensureLoaded() {
        assertDoesNotThrow(NanopubImpl::ensureLoaded);
    }

    @Test
    void tryToLoadParserFactoryWithUnknownClass() throws Exception {
        Method method = NanopubImpl.class.getDeclaredMethod("tryToLoadParserFactory", String.class);
        method.setAccessible(true);

        InvocationTargetException ex = assertThrows(InvocationTargetException.class,
                () -> method.invoke(null, "org.nanopub.NoSuchParserFactory"));
        assertInstanceOf(RuntimeException.class, ex.getCause());
        assertInstanceOf(ClassNotFoundException.class, ex.getCause().getCause());
    }

    @Test
    void tryToLoadWriterFactoryWithUnknownClass() throws Exception {
        Method method = NanopubImpl.class.getDeclaredMethod("tryToLoadWriterFactory", String.class);
        method.setAccessible(true);

        InvocationTargetException ex = assertThrows(InvocationTargetException.class,
                () -> method.invoke(null, "org.nanopub.NoSuchWriterFactory"));
        assertInstanceOf(RuntimeException.class, ex.getCause());
        assertInstanceOf(ClassNotFoundException.class, ex.getCause().getCause());
    }

    // ------------------------------------------------------------ malformedness
    @Test
    void rejectsEmptyContent() {
        assertEquals("No content received for nanopub", malformedNanopubMessage(List.of()));
    }

    @Test
    void rejectsStatementsWithoutNanopubUri() {
        List<Statement> statements = List.of(vf.createStatement(ASSERTION, RDFS.LABEL, vf.createLiteral("a"), ASSERTION));

        assertEquals("No nanopub URI found", malformedNanopubMessage(statements));
    }

    @Test
    void rejectsStatementsWithoutContext() {
        List<Statement> statements = List.of(vf.createStatement(NP_ID, RDF.TYPE, NP.NANOPUBLICATION));

        assertEquals("Null value for context URI found.", malformedNanopubMessage(statements));
    }

    @Test
    void rejectsTwoNanopubUris() {
        List<Statement> statements = validStatements();
        statements.add(vf.createStatement(vf.createIRI(NP_URI + "other"), RDF.TYPE, NP.NANOPUBLICATION, HEAD));

        assertEquals("Two nanopub URIs found", malformedNanopubMessage(statements));
    }

    @Test
    void rejectsTwoAssertionUris() {
        List<Statement> statements = validStatements();
        statements.add(vf.createStatement(NP_ID, NP.HAS_ASSERTION, vf.createIRI(NP_URI + "g5"), HEAD));

        assertTrue(malformedNanopubMessage(statements).startsWith("Two assertion URIs found: "));
    }

    @Test
    void rejectsTwoProvenanceUris() {
        List<Statement> statements = validStatements();
        statements.add(vf.createStatement(NP_ID, NP.HAS_PROVENANCE, vf.createIRI(NP_URI + "g5"), HEAD));

        assertTrue(malformedNanopubMessage(statements).startsWith("Two provenance URIs found: "));
    }

    @Test
    void rejectsTwoPubinfoUris() {
        List<Statement> statements = validStatements();
        statements.add(vf.createStatement(NP_ID, NP.HAS_PUBINFO, vf.createIRI(NP_URI + "g5"), HEAD));

        assertTrue(malformedNanopubMessage(statements).startsWith("Two publication info URIs found: "));
    }

    @Test
    void rejectsMissingAssertionUri() {
        List<Statement> statements = new ArrayList<>();
        statements.add(vf.createStatement(NP_ID, RDF.TYPE, NP.NANOPUBLICATION, HEAD));

        assertEquals("No assertion URI found for " + NP_URI, malformedNanopubMessage(statements));
    }

    @Test
    void rejectsMissingProvenanceUri() {
        List<Statement> statements = new ArrayList<>();
        statements.add(vf.createStatement(NP_ID, RDF.TYPE, NP.NANOPUBLICATION, HEAD));
        statements.add(vf.createStatement(NP_ID, NP.HAS_ASSERTION, ASSERTION, HEAD));

        assertEquals("No provenance URI found for " + NP_URI, malformedNanopubMessage(statements));
    }

    @Test
    void rejectsMissingPubinfoUri() {
        List<Statement> statements = new ArrayList<>();
        statements.add(vf.createStatement(NP_ID, RDF.TYPE, NP.NANOPUBLICATION, HEAD));
        statements.add(vf.createStatement(NP_ID, NP.HAS_ASSERTION, ASSERTION, HEAD));
        statements.add(vf.createStatement(NP_ID, NP.HAS_PROVENANCE, PROVENANCE, HEAD));

        assertEquals("No publication info URI found for " + NP_URI, malformedNanopubMessage(statements));
    }

    @Test
    void rejectsNanopubUriUsedAsGraphUri() {
        List<Statement> statements = new ArrayList<>();
        statements.add(vf.createStatement(NP_ID, RDF.TYPE, NP.NANOPUBLICATION, HEAD));
        statements.add(vf.createStatement(NP_ID, NP.HAS_ASSERTION, ASSERTION, HEAD));
        statements.add(vf.createStatement(NP_ID, NP.HAS_PROVENANCE, PROVENANCE, HEAD));
        statements.add(vf.createStatement(NP_ID, NP.HAS_PUBINFO, NP_ID, HEAD));

        assertEquals("Nanopub URI cannot be identical to one of the graph URIs: " + NP_URI,
                malformedNanopubMessage(statements));
    }

    @Test
    void rejectsDisconnectedGraphs() {
        List<Statement> statements = validStatements();
        IRI disconnected = vf.createIRI(NP_URI + "g9");
        statements.add(vf.createStatement(NP_ID, RDFS.LABEL, vf.createLiteral("x"), disconnected));

        assertEquals("Disconnected graph: " + disconnected, malformedNanopubMessage(statements));
    }

    @Test
    void rejectsMalformedUris() {
        String malformed = NP_URI + "with a space";
        List<Statement> statements = validStatements();
        statements.add(vf.createStatement(ASSERTION, RDFS.SEEALSO, vf.createIRI(malformed), ASSERTION));

        assertEquals("Malformed URI: " + malformed, malformedNanopubMessage(statements));
    }

    @Test
    void rejectsProvenanceThatDoesNotReferToTheAssertion() {
        List<Statement> statements = headStatements();
        statements.add(vf.createStatement(ASSERTION, RDFS.LABEL, vf.createLiteral("a"), ASSERTION));
        statements.add(vf.createStatement(NP_ID, RDFS.LABEL, vf.createLiteral("p"), PROVENANCE));
        statements.add(vf.createStatement(NP_ID, RDFS.LABEL, vf.createLiteral("i"), PUBINFO));

        assertEquals("Provenance does not refer to assertion: " + PROVENANCE, malformedNanopubMessage(statements));
    }

    @Test
    void acceptsProvenanceThatRefersToTheAssertionAsObject() throws MalformedNanopubException {
        List<Statement> statements = headStatements();
        statements.add(vf.createStatement(ASSERTION, RDFS.LABEL, vf.createLiteral("a"), ASSERTION));
        statements.add(vf.createStatement(NP_ID, RDFS.SEEALSO, ASSERTION, PROVENANCE));
        statements.add(vf.createStatement(NP_ID, RDFS.LABEL, vf.createLiteral("i"), PUBINFO));

        assertEquals(NP_ID, new NanopubImpl(statements).getUri());
    }

    @Test
    void rejectsPubinfoThatDoesNotReferToTheNanopub() {
        List<Statement> statements = headStatements();
        statements.add(vf.createStatement(ASSERTION, RDFS.LABEL, vf.createLiteral("a"), ASSERTION));
        statements.add(vf.createStatement(ASSERTION, RDFS.LABEL, vf.createLiteral("p"), PROVENANCE));
        statements.add(vf.createStatement(ASSERTION, RDFS.LABEL, vf.createLiteral("i"), PUBINFO));

        assertEquals("Publication info does not refer to nanopublication URI: " + PUBINFO,
                malformedNanopubMessage(statements));
    }

    // ------------------------------------------------------------ file loading
    private static File writeNanopubFile(File directory, String fileName) throws IOException {
        File file = new File(directory, fileName);
        Files.writeString(file.toPath(), NANOPUB_TRIG, StandardCharsets.UTF_8);
        return file;
    }

    @Test
    void constructFromFileWithExplicitFormat(@TempDir File tempDir) throws Exception {
        NanopubImpl nanopub = new NanopubImpl(writeNanopubFile(tempDir, "np.trig"), RDFFormat.TRIG);

        assertEquals(NP_ID, nanopub.getUri());
    }

    @Test
    void constructFromFileGuessingTheFormatFromTheFileName(@TempDir File tempDir) throws Exception {
        NanopubImpl nanopub = new NanopubImpl(writeNanopubFile(tempDir, "np.trig"));

        assertEquals(NP_ID, nanopub.getUri());
    }

    @Test
    void constructFromFileFallingBackToTrigForContextLessFormats(@TempDir File tempDir) throws Exception {
        // Turtle is what the file name suggests, but it cannot carry graphs, so TriG is used instead
        NanopubImpl nanopub = new NanopubImpl(writeNanopubFile(tempDir, "np.ttl"));

        assertEquals(NP_ID, nanopub.getUri());
    }

    @Test
    void constructFromFileUsingTheFormatOfTheMimeType(@TempDir File tempDir) throws Exception {
        File file = writeNanopubFile(tempDir, "np.unknown");

        try (MockedStatic<Rio> rioMock = mockStatic(Rio.class, CALLS_REAL_METHODS)) {
            rioMock.when(() -> Rio.getParserFormatForMIMEType(anyString())).thenReturn(Optional.of(RDFFormat.TRIG));

            assertEquals(NP_ID, new NanopubImpl(file).getUri());
        }
    }

    @Test
    void constructFromFileIgnoringAContextLessMimeType(@TempDir File tempDir) throws Exception {
        File file = writeNanopubFile(tempDir, "np.trig");

        try (MockedStatic<Rio> rioMock = mockStatic(Rio.class, CALLS_REAL_METHODS)) {
            // Turtle cannot carry graphs, so the format is taken from the file name instead
            rioMock.when(() -> Rio.getParserFormatForMIMEType(anyString())).thenReturn(Optional.of(RDFFormat.TURTLE));

            assertEquals(NP_ID, new NanopubImpl(file).getUri());
        }
    }

    // ------------------------------------------------------------- URL loading
    private static CloseableHttpResponse httpResponse(int statusCode, String reasonPhrase, String contentType) throws IOException {
        CloseableHttpResponse response = mock(CloseableHttpResponse.class);
        StatusLine statusLine = mock(StatusLine.class);
        when(statusLine.getStatusCode()).thenReturn(statusCode);
        when(statusLine.getReasonPhrase()).thenReturn(reasonPhrase);
        when(response.getStatusLine()).thenReturn(statusLine);
        HttpEntity entity = mock(HttpEntity.class);
        when(entity.getContent()).thenReturn(new ByteArrayInputStream(NANOPUB_TRIG.getBytes(StandardCharsets.UTF_8)));
        when(response.getEntity()).thenReturn(entity);
        if (contentType != null) {
            Header header = mock(Header.class);
            when(header.getValue()).thenReturn(contentType);
            when(response.getFirstHeader("Content-Type")).thenReturn(header);
        }
        return response;
    }

    private static CloseableHttpClient httpClientReturning(CloseableHttpResponse response) throws IOException {
        CloseableHttpClient client = mock(CloseableHttpClient.class);
        when(client.execute(any(HttpGet.class))).thenReturn(response);
        return client;
    }

    /**
     * Runs the given action with {@link NanopubUtils#getHttpClient()} answering
     * with the given response.
     */
    private static void withHttpResponse(CloseableHttpResponse response, ThrowingRunnable action) throws Throwable {
        // the client has to be stubbed before the static mock is set up, or the nested
        // stubbing is reported as an unfinished one
        CloseableHttpClient client = httpClientReturning(response);
        try (MockedStatic<NanopubUtils> nanopubUtilsMock = mockStatic(NanopubUtils.class, CALLS_REAL_METHODS)) {
            nanopubUtilsMock.when(NanopubUtils::getHttpClient).thenReturn(client);
            action.run();
        }
    }

    private interface ThrowingRunnable {

        void run() throws Throwable;
    }

    @Test
    void constructFromUrlWithExplicitFormat() throws Throwable {
        withHttpResponse(httpResponse(200, "OK", "application/trig"), () -> {
            URL url = new URI("https://example.org/np1").toURL();
            assertEquals(NP_ID, new NanopubImpl(url, RDFFormat.TRIG).getUri());
        });
    }

    @Test
    void constructFromUrlUsingTheContentTypeHeader() throws Throwable {
        withHttpResponse(httpResponse(200, "OK", "application/trig"), () -> {
            URL url = new URI("https://example.org/np1").toURL();
            assertEquals(NP_ID, new NanopubImpl(url).getUri());
        });
    }

    @Test
    void constructFromUrlWithoutContentTypeHeader() throws Throwable {
        withHttpResponse(httpResponse(200, "OK", null), () -> {
            URL url = new URI("https://example.org/np1.trig").toURL();
            assertEquals(NP_ID, new NanopubImpl(url).getUri());
        });
    }

    @Test
    void constructFromUrlWithUnknownContentType() throws Throwable {
        withHttpResponse(httpResponse(200, "OK", "application/octet-stream"), () -> {
            URL url = new URI("https://example.org/np1.trig").toURL();
            assertEquals(NP_ID, new NanopubImpl(url).getUri());
        });
    }

    @Test
    void constructFromUrlWithContextLessContentType() throws Throwable {
        withHttpResponse(httpResponse(200, "OK", "text/turtle"), () -> {
            URL url = new URI("https://example.org/np1.trig").toURL();
            assertEquals(NP_ID, new NanopubImpl(url).getUri());
        });
    }

    @Test
    void constructFromUrlFallingBackToTrigForContextLessFormats() throws Throwable {
        withHttpResponse(httpResponse(200, "OK", null), () -> {
            URL url = new URI("https://example.org/np1.ttl").toURL();
            assertEquals(NP_ID, new NanopubImpl(url).getUri());
        });
    }

    @Test
    @SuppressWarnings("deprecation")
    void constructFromUrlThatCannotBeTurnedIntoARequest() throws Throwable {
        // the curly braces are fine for java.net.URL but not for the URI that the HTTP client builds
        URL url = new URL("https://example.org/np{1}");

        withHttpResponse(httpResponse(200, "OK", "application/trig"), () -> {
            IOException ex = assertThrows(IOException.class, () -> new NanopubImpl(url));
            assertEquals("invalid URL: " + url, ex.getMessage());
        });
    }

    @Test
    void constructFromUrlThatIsNotFound() throws Throwable {
        withHttpResponse(httpResponse(404, "Not Found", "application/trig"), () -> {
            URL url = new URI("https://example.org/np1").toURL();
            FileNotFoundException ex = assertThrows(FileNotFoundException.class, () -> new NanopubImpl(url));
            assertEquals("Not Found", ex.getMessage());
        });
    }

    @Test
    void constructFromUrlThatIsGone() throws Throwable {
        withHttpResponse(httpResponse(410, "Gone", "application/trig"), () -> {
            URL url = new URI("https://example.org/np1").toURL();
            assertThrows(FileNotFoundException.class, () -> new NanopubImpl(url));
        });
    }

    @Test
    void constructFromUrlWithInformationalStatus() throws Throwable {
        withHttpResponse(httpResponse(100, "Continue", "application/trig"), () -> {
            URL url = new URI("https://example.org/np1").toURL();
            IOException ex = assertThrows(IOException.class, () -> new NanopubImpl(url));
            assertEquals("HTTP error 100: Continue", ex.getMessage());
        });
    }

    @Test
    void constructFromUrlWithServerError() throws Throwable {
        withHttpResponse(httpResponse(500, "Internal Server Error", "application/trig"), () -> {
            URL url = new URI("https://example.org/np1").toURL();
            IOException ex = assertThrows(IOException.class, () -> new NanopubImpl(url));
            assertEquals("HTTP error 500: Internal Server Error", ex.getMessage());
        });
    }

    // ------------------------------------------------------ repository loading
    private static Repository repositoryReturning(List<Statement> statements) {
        List<BindingSet> bindingSets = new ArrayList<>();
        for (Statement st : statements) {
            MapBindingSet bs = new MapBindingSet();
            bs.addBinding("G", st.getContext());
            bs.addBinding("S", st.getSubject());
            bs.addBinding("P", st.getPredicate());
            bs.addBinding("O", st.getObject());
            bindingSets.add(bs);
        }
        Iterator<BindingSet> iterator = bindingSets.iterator();
        TupleQueryResult result = mock(TupleQueryResult.class);
        when(result.hasNext()).thenAnswer(invocation -> iterator.hasNext());
        when(result.next()).thenAnswer(invocation -> iterator.next());

        TupleQuery tupleQuery = mock(TupleQuery.class);
        when(tupleQuery.evaluate()).thenReturn(result);
        RepositoryConnection connection = mock(RepositoryConnection.class);
        when(connection.prepareTupleQuery(any(QueryLanguage.class), anyString())).thenReturn(tupleQuery);
        Repository repository = mock(Repository.class);
        when(repository.getConnection()).thenReturn(connection);
        return repository;
    }

    @Test
    void constructFromRepository() throws MalformedNanopubException {
        NanopubImpl nanopub = new NanopubImpl(repositoryReturning(validStatements()), NP_ID);

        assertEquals(NP_ID, nanopub.getUri());
        assertTrue(nanopub.getNsPrefixes().isEmpty());
    }

    @Test
    void constructFromRepositoryWithNamespaces() throws MalformedNanopubException {
        NanopubImpl nanopub = new NanopubImpl(repositoryReturning(validStatements()), NP_ID,
                List.of("ex"), Map.of("ex", "https://example.org/"));

        assertEquals(NP_ID, nanopub.getUri());
        assertEquals(List.of("ex"), nanopub.getNsPrefixes());
        assertEquals("https://example.org/", nanopub.getNamespace("ex"));
    }

    @Test
    void constructFromRepositoryWithFailingQuery() {
        RepositoryConnection connection = mock(RepositoryConnection.class);
        when(connection.prepareTupleQuery(any(QueryLanguage.class), anyString()))
                .thenThrow(new MalformedQueryException("cannot parse this query"));
        Repository repository = mock(Repository.class);
        when(repository.getConnection()).thenReturn(connection);

        MalformedNanopubException ex = assertThrows(MalformedNanopubException.class,
                () -> new NanopubImpl(repository, NP_ID));
        assertEquals("No content received for nanopub", ex.getMessage());
    }

    // ---------------------------------------------------------------- accessors
    @Test
    void getCreationTimeAuthorsAndCreators() throws MalformedNanopubException {
        IRI author = vf.createIRI("https://orcid.org/0000-0000-0000-0001");
        IRI creator = vf.createIRI("https://orcid.org/0000-0000-0000-0002");
        List<Statement> statements = validStatements();
        statements.add(vf.createStatement(NP_ID, PAV.AUTHORED_BY, author, PUBINFO));
        statements.add(vf.createStatement(NP_ID, DCTERMS.CREATOR, creator, PUBINFO));
        statements.add(vf.createStatement(NP_ID, DCTERMS.CREATED,
                vf.createLiteral("2024-01-02T03:04:05Z", XSD.DATETIME), PUBINFO));

        NanopubImpl nanopub = new NanopubImpl(statements);

        assertEquals(Set.of(author), nanopub.getAuthors());
        assertEquals(Set.of(creator), nanopub.getCreators());
        assertNotNull(nanopub.getCreationTime());
    }

    @Test
    void removeUnusedPrefixesIsIdempotent() throws MalformedNanopubException {
        NanopubImpl nanopub = new NanopubImpl(validStatements(),
                List.of("rdfs", "unused"),
                Map.of("rdfs", RDFS.NAMESPACE, "unused", "https://example.org/unused/"));

        nanopub.removeUnusedPrefixes();
        List<String> afterFirstRun = nanopub.getNsPrefixes();
        nanopub.removeUnusedPrefixes();

        assertEquals(List.of("rdfs"), afterFirstRun);
        assertEquals(afterFirstRun, nanopub.getNsPrefixes());
    }

    // ------------------------------------------------------- equals and hashCode
    /**
     * A nanopub built from {@link #validStatements()}, with the graph URIs
     * derived from the given suffixes so that individual fields can be varied
     * without disturbing the triple and byte counts.
     */
    private static NanopubImpl nanopub(String npUri, String headSuffix, String assertionSuffix,
            String provenanceSuffix, String pubinfoSuffix,
            List<String> nsPrefixes, Map<String, String> ns) throws MalformedNanopubException {
        IRI npId = vf.createIRI(npUri);
        IRI head = vf.createIRI(npUri + headSuffix);
        IRI assertion = vf.createIRI(npUri + assertionSuffix);
        IRI provenance = vf.createIRI(npUri + provenanceSuffix);
        IRI pubinfo = vf.createIRI(npUri + pubinfoSuffix);
        List<Statement> statements = new ArrayList<>();
        statements.add(vf.createStatement(npId, RDF.TYPE, NP.NANOPUBLICATION, head));
        statements.add(vf.createStatement(npId, NP.HAS_ASSERTION, assertion, head));
        statements.add(vf.createStatement(npId, NP.HAS_PROVENANCE, provenance, head));
        statements.add(vf.createStatement(npId, NP.HAS_PUBINFO, pubinfo, head));
        statements.add(vf.createStatement(assertion, RDFS.LABEL, vf.createLiteral("a"), assertion));
        statements.add(vf.createStatement(assertion, RDFS.LABEL, vf.createLiteral("p"), provenance));
        statements.add(vf.createStatement(npId, RDFS.LABEL, vf.createLiteral("i"), pubinfo));
        return new NanopubImpl(statements, nsPrefixes, ns);
    }

    private static NanopubImpl defaultNanopub() throws MalformedNanopubException {
        return nanopub(NP_URI, "g1", "g2", "g3", "g4", List.of(), Map.of());
    }

    /**
     * Adds one extra statement to a nanopub built like
     * {@link #defaultNanopub()}. The graph and the label are varied by the
     * caller; both are one character long, so the byte count stays the same.
     */
    private static NanopubImpl nanopubWithExtraStatement(String graphSuffix, String label) throws MalformedNanopubException {
        List<Statement> statements = validStatements();
        statements.add(vf.createStatement(NP_ID, RDFS.LABEL, vf.createLiteral(label), vf.createIRI(NP_URI + graphSuffix)));
        return new NanopubImpl(statements);
    }

    @Test
    void equalsAndHashCodeForEqualNanopubs() throws MalformedNanopubException {
        NanopubImpl nanopub = defaultNanopub();
        NanopubImpl same = defaultNanopub();

        assertEquals(nanopub, nanopub);
        assertEquals(nanopub, same);
        assertEquals(nanopub.hashCode(), same.hashCode());
    }

    @Test
    void equalsForOtherTypes() throws MalformedNanopubException {
        NanopubImpl nanopub = defaultNanopub();

        assertNotEquals(nanopub, "not a nanopub");
        assertNotEquals(null, nanopub);
    }

    @Test
    void notEqualWhenUnusedPrefixesWereRemoved() throws MalformedNanopubException {
        List<String> prefixes = List.of("rdfs");
        Map<String, String> ns = Map.of("rdfs", RDFS.NAMESPACE);
        NanopubImpl nanopub = nanopub(NP_URI, "g1", "g2", "g3", "g4", prefixes, ns);
        NanopubImpl other = nanopub(NP_URI, "g1", "g2", "g3", "g4", prefixes, ns);

        other.removeUnusedPrefixes();

        assertNotEquals(nanopub, other);
    }

    @Test
    void notEqualWhenTripleCountDiffers() throws MalformedNanopubException {
        assertNotEquals(defaultNanopub(), nanopubWithExtraStatement("g4", "x"));
    }

    @Test
    void notEqualWhenByteCountDiffers() throws MalformedNanopubException {
        assertNotEquals(nanopubWithExtraStatement("g4", "x"), nanopubWithExtraStatement("g4", "xx"));
    }

    @Test
    void notEqualWhenNanopubUriDiffers() throws MalformedNanopubException {
        assertNotEquals(defaultNanopub(), nanopub("https://example.org/np2#", "g1", "g2", "g3", "g4", List.of(), Map.of()));
    }

    @Test
    void notEqualWhenHeadUriDiffers() throws MalformedNanopubException {
        assertNotEquals(defaultNanopub(), nanopub(NP_URI, "h1", "g2", "g3", "g4", List.of(), Map.of()));
    }

    @Test
    void notEqualWhenAssertionUriDiffers() throws MalformedNanopubException {
        assertNotEquals(defaultNanopub(), nanopub(NP_URI, "g1", "h2", "g3", "g4", List.of(), Map.of()));
    }

    @Test
    void notEqualWhenProvenanceUriDiffers() throws MalformedNanopubException {
        assertNotEquals(defaultNanopub(), nanopub(NP_URI, "g1", "g2", "h3", "g4", List.of(), Map.of()));
    }

    @Test
    void notEqualWhenPubinfoUriDiffers() throws MalformedNanopubException {
        assertNotEquals(defaultNanopub(), nanopub(NP_URI, "g1", "g2", "g3", "h4", List.of(), Map.of()));
    }

    @Test
    void notEqualWhenHeadDiffers() throws MalformedNanopubException {
        // the extra statement sits in the head of the one and in the pubinfo of the other
        assertNotEquals(nanopubWithExtraStatement("g1", "x"), nanopubWithExtraStatement("g4", "x"));
    }

    @Test
    void notEqualWhenAssertionDiffers() throws MalformedNanopubException {
        assertNotEquals(nanopubWithExtraStatement("g2", "x"), nanopubWithExtraStatement("g3", "x"));
    }

    @Test
    void notEqualWhenProvenanceDiffers() throws MalformedNanopubException {
        assertNotEquals(nanopubWithExtraStatement("g3", "x"), nanopubWithExtraStatement("g4", "x"));
    }

    @Test
    void notEqualWhenPubinfoDiffers() throws MalformedNanopubException {
        assertNotEquals(nanopubWithExtraStatement("g4", "x"), nanopubWithExtraStatement("g4", "y"));
    }

    @Test
    void notEqualWhenOnlyTheStatementOrderDiffers() throws MalformedNanopubException {
        List<Statement> statements = validStatements();
        List<Statement> reordered = new ArrayList<>(statements);
        Collections.reverse(reordered);

        NanopubImpl nanopub = new NanopubImpl(statements);
        NanopubImpl other = new NanopubImpl(reordered);

        assertEquals(nanopub.getHead(), other.getHead());
        assertNotEquals(nanopub, other);
    }

    @Test
    void notEqualWhenNamespacePrefixesDiffer() throws MalformedNanopubException {
        NanopubImpl nanopub = nanopub(NP_URI, "g1", "g2", "g3", "g4", List.of("ex"), Map.of("ex", "https://example.org/"));
        NanopubImpl other = nanopub(NP_URI, "g1", "g2", "g3", "g4", List.of("other"), Map.of("other", "https://example.org/"));

        assertNotEquals(nanopub, other);
    }

    @Test
    void notEqualWhenNamespacesDiffer() throws MalformedNanopubException {
        NanopubImpl nanopub = nanopub(NP_URI, "g1", "g2", "g3", "g4", List.of("ex"), Map.of("ex", "https://example.org/one/"));
        NanopubImpl other = nanopub(NP_URI, "g1", "g2", "g3", "g4", List.of("ex"), Map.of("ex", "https://example.org/two/"));

        assertNotEquals(nanopub, other);
    }

    @Test
    void equalsAndHashCodeForTrustyNanopubs() throws Exception {
        NanopubCreator creator = new NanopubCreator(TestUtils.NANOPUB_URI);
        creator.addAssertionStatement(TestUtils.anyIri, TestUtils.anyIri, TestUtils.anyIri);
        creator.addProvenanceStatement(TestUtils.anyIri, TestUtils.anyIri);
        creator.addPubinfoStatement(TestUtils.anyIri, TestUtils.anyIri);
        Nanopub trusty = creator.finalizeTrustyNanopub();
        NanopubImpl nanopub = assertInstanceOf(NanopubImpl.class, trusty);
        NanopubImpl same = new NanopubImpl(NanopubUtils.writeToString(trusty, RDFFormat.TRIG), RDFFormat.TRIG);

        assertTrue(nanopub.isValidAndTrusty());
        assertTrue(same.isValidAndTrusty());
        assertEquals(nanopub, same);
        assertEquals(nanopub.hashCode(), same.hashCode());
        assertEquals(Objects.hash(nanopub.getUri()), nanopub.hashCode());
    }

}
