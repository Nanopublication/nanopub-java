package org.nanopub.extra.server;

import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicHeader;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.nanopub.Nanopub;
import org.nanopub.NanopubCreator;
import org.nanopub.NanopubUtils;
import org.nanopub.utils.TestUtils;
import org.nanopub.vocabulary.KPXL_GRLC;
import org.nanopub.vocabulary.NPX;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.nanopub.utils.TestUtils.anyIri;

class PublishNanopubTest {

    private static final String LOCAL_REGISTRY_URL = "https://local-registry.example/";
    private static final String PUBLIC_REGISTRY_URL = "https://public-registry.example/";

    private MockedStatic<NanopubUtils> mockStatic;
    private CloseableHttpClient mockHttpClient;

    @BeforeEach
    void setUp() throws Exception {
        clearRegistryInfoCache();
        mockHttpClient = mock(CloseableHttpClient.class);
        mockStatic = mockStatic(NanopubUtils.class, CALLS_REAL_METHODS);
        mockStatic.when(NanopubUtils::getHttpClient).thenReturn(mockHttpClient);
    }

    @AfterEach
    void tearDown() throws Exception {
        mockStatic.close();
        clearRegistryInfoCache();
    }

    /**
     * ServerIterator keeps loaded registry infos in a static map, which would otherwise leak
     * between tests that use the same registry URL with different flags.
     */
    private void clearRegistryInfoCache() throws Exception {
        Field f = ServerIterator.class.getDeclaredField("serverInfos");
        f.setAccessible(true);
        ((Map<?, ?>) f.get(null)).clear();
    }

    private void stubRegistry(boolean isLocalInstance) throws Exception {
        String json = "{\"status\":\"ready\",\"isLocalInstance\":" + isLocalInstance + "}";
        CloseableHttpResponse getResponse = mock(CloseableHttpResponse.class);
        HttpEntity getEntity = mock(HttpEntity.class);
        when(getEntity.getContent()).thenReturn(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
        when(getResponse.getEntity()).thenReturn(getEntity);
        when(mockHttpClient.execute(any(HttpGet.class))).thenReturn(getResponse);

        CloseableHttpResponse postResponse = mock(CloseableHttpResponse.class);
        StatusLine statusLine = mock(StatusLine.class);
        when(statusLine.getStatusCode()).thenReturn(201);
        when(postResponse.getStatusLine()).thenReturn(statusLine);
        when(mockHttpClient.execute(any(HttpPost.class))).thenReturn(postResponse);
    }

    private static Nanopub grlcQuery(String sparql) throws Exception {
        NanopubCreator creator = TestUtils.getNanopubCreator();
        creator.addAssertionStatement(anyIri, KPXL_GRLC.SPARQL, TestUtils.vf.createLiteral(sparql));
        creator.addProvenanceStatement(creator.getAssertionUri(), anyIri, anyIri);
        creator.addPubinfoStatement(anyIri, anyIri);
        return creator.finalizeTrustyNanopub();
    }

    private static Nanopub protectedNanopub() throws Exception {
        NanopubCreator creator = TestUtils.getNanopubCreator();
        creator.addAssertionStatement(anyIri, anyIri, anyIri);
        creator.addProvenanceStatement(creator.getAssertionUri(), anyIri, anyIri);
        creator.addPubinfoStatement(RDF.TYPE, NPX.PROTECTED_NANOPUB);
        return creator.finalizeTrustyNanopub();
    }

    private static Nanopub plainNanopub() throws Exception {
        NanopubCreator creator = TestUtils.getNanopubCreator();
        creator.addAssertionStatement(anyIri, anyIri, anyIri);
        creator.addProvenanceStatement(creator.getAssertionUri(), anyIri, anyIri);
        creator.addPubinfoStatement(anyIri, anyIri);
        return creator.finalizeTrustyNanopub();
    }

    @Test
    void refusesToPublishANanopubWithInvalidSparql() throws Exception {
        // no server is contacted: the refusal happens before the registry is looked up
        Nanopub np = grlcQuery("SELECT ?x WHERE { ?x ?y ?z }");

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> new PublishNanopub().publishNanopub(np, "https://example.org/unreachable/"));

        assertTrue(ex.getMessage().contains("Can't publish nanopublication with invalid SPARQL"), ex.getMessage());
        assertTrue(ex.getMessage().contains("U+00A0 (NO-BREAK SPACE)"), ex.getMessage());
    }

    @Test
    void protectedNanopubIsPublishedToLocalInstance() throws Exception {
        stubRegistry(true);

        String url = new PublishNanopub().publishNanopub(protectedNanopub(), LOCAL_REGISTRY_URL);

        assertNotNull(url);
        assertTrue(url.startsWith(LOCAL_REGISTRY_URL + "np/"));
        verify(mockHttpClient).execute(any(HttpPost.class));
    }

    @Test
    void protectedNanopubIsNotPublishedToPublicRegistry() throws Exception {
        stubRegistry(false);
        PublishNanopub publisher = new PublishNanopub();
        Nanopub nanopub = protectedNanopub();

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> publisher.publishNanopub(nanopub, PUBLIC_REGISTRY_URL));

        assertTrue(ex.getMessage().contains("Can't publish protected nanopublication"), ex.getMessage());
        assertTrue(ex.getMessage().contains("local instance"), ex.getMessage());
        verify(mockHttpClient, never()).execute(any(HttpPost.class));
    }

    @Test
    void unprotectedNanopubIsPublishedToPublicRegistry() throws Exception {
        stubRegistry(false);

        String url = new PublishNanopub().publishNanopub(plainNanopub(), PUBLIC_REGISTRY_URL);

        assertNotNull(url);
        assertTrue(url.startsWith(PUBLIC_REGISTRY_URL + "np/"));
        verify(mockHttpClient).execute(any(HttpPost.class));
    }

    /**
     * Records the backoff instead of spending it, so the retry tests stay fast.
     */
    private static class RecordingPublisher extends PublishNanopub {

        final List<Long> waits = new ArrayList<>();

        @Override
        void waitBeforeRetry(long millis) {
            waits.add(millis);
        }

    }

    /**
     * Stubs the registry-info GET and then answers the POSTs with the given statuses in order.
     * A status may carry a Retry-After value (seconds), or null for none.
     */
    private void stubPostStatuses(PostStub... stubs) throws Exception {
        String json = "{\"status\":\"ready\",\"isLocalInstance\":false}";
        CloseableHttpResponse getResponse = mock(CloseableHttpResponse.class);
        HttpEntity getEntity = mock(HttpEntity.class);
        when(getEntity.getContent()).thenReturn(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
        when(getResponse.getEntity()).thenReturn(getEntity);
        when(mockHttpClient.execute(any(HttpGet.class))).thenReturn(getResponse);

        CloseableHttpResponse[] responses = new CloseableHttpResponse[stubs.length];
        for (int i = 0; i < stubs.length; i++) {
            CloseableHttpResponse postResponse = mock(CloseableHttpResponse.class);
            StatusLine statusLine = mock(StatusLine.class);
            when(statusLine.getStatusCode()).thenReturn(stubs[i].code());
            when(statusLine.getReasonPhrase()).thenReturn(stubs[i].reason());
            when(postResponse.getStatusLine()).thenReturn(statusLine);
            if (stubs[i].retryAfter() != null) {
                when(postResponse.getFirstHeader("Retry-After"))
                        .thenReturn(new BasicHeader("Retry-After", stubs[i].retryAfter()));
            }
            responses[i] = postResponse;
        }
        if (responses.length == 1) {
            when(mockHttpClient.execute(any(HttpPost.class))).thenReturn(responses[0]);
        } else {
            CloseableHttpResponse[] rest = new CloseableHttpResponse[responses.length - 1];
            System.arraycopy(responses, 1, rest, 0, rest.length);
            when(mockHttpClient.execute(any(HttpPost.class))).thenReturn(responses[0], rest);
        }
    }

    private record PostStub(int code, String reason, String retryAfter) {
    }

    @Test
    void aRateLimitedRegistryIsRetriedRatherThanAbandoned() throws Exception {
        // 10 requests/s per IP is what the public registries allow, so a bulk publish meets 429
        // and 503 as a matter of course; giving up on the registry would end the batch.
        stubPostStatuses(new PostStub(429, "Too Many Requests", null),
                new PostStub(503, "Service Unavailable", null),
                new PostStub(201, "Created", null));
        RecordingPublisher publisher = new RecordingPublisher();

        String url = publisher.publishNanopub(plainNanopub(), PUBLIC_REGISTRY_URL);

        assertNotNull(url);
        assertTrue(url.startsWith(PUBLIC_REGISTRY_URL + "np/"));
        verify(mockHttpClient, times(3)).execute(any(HttpPost.class));
        assertEquals(List.of(1000L, 2000L), publisher.waits);
    }

    @Test
    void retryAfterHeaderSetsTheWait() throws Exception {
        stubPostStatuses(new PostStub(503, "Service Unavailable", "7"),
                new PostStub(201, "Created", null));
        RecordingPublisher publisher = new RecordingPublisher();

        assertNotNull(publisher.publishNanopub(plainNanopub(), PUBLIC_REGISTRY_URL));

        assertEquals(List.of(7000L), publisher.waits);
    }

    @Test
    void anAbsurdRetryAfterIsCapped() throws Exception {
        stubPostStatuses(new PostStub(429, "Too Many Requests", "86400"),
                new PostStub(201, "Created", null));
        RecordingPublisher publisher = new RecordingPublisher();

        assertNotNull(publisher.publishNanopub(plainNanopub(), PUBLIC_REGISTRY_URL));

        assertEquals(List.of(30000L), publisher.waits);
    }

    @Test
    void aRejectedNanopubIsNotRetriedAndNotOfferedElsewhere() throws Exception {
        // Every registry applies the same content rules (triple and byte limits, a creation time
        // that is not in the future, covered types), so a 400 is final.
        stubPostStatuses(new PostStub(400, "Error processing nanopub: Nanopublication not supported", null));
        RecordingPublisher publisher = new RecordingPublisher();
        Nanopub nanopub = plainNanopub();

        NanopubRejectedException ex = assertThrows(NanopubRejectedException.class,
                () -> publisher.publishNanopub(nanopub, PUBLIC_REGISTRY_URL));

        assertEquals(400, ex.getStatusCode());
        assertEquals(PUBLIC_REGISTRY_URL, ex.getRegistryUrl());
        assertEquals(nanopub.getUri(), ex.getNanopubUri());
        assertTrue(ex.getMessage().contains("Nanopublication not supported"), ex.getMessage());
        verify(mockHttpClient, times(1)).execute(any(HttpPost.class));
        assertTrue(publisher.waits.isEmpty());
    }

    @Test
    void anEndpointLevelErrorIsNotTakenForARejectionOfTheNanopub() throws Exception {
        // A 404 or 405 says something about this endpoint, not about the nanopublication, so it
        // must keep falling through to the next registry rather than ending the publication.
        stubPostStatuses(new PostStub(404, "Not Found", null));
        RecordingPublisher publisher = new RecordingPublisher();
        Nanopub nanopub = plainNanopub();

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> publisher.publishNanopub(nanopub, PUBLIC_REGISTRY_URL));

        assertFalse(ex instanceof NanopubRejectedException, ex.getClass().getName());
        assertTrue(ex.getMessage().contains("404"), ex.getMessage());
        verify(mockHttpClient, times(1)).execute(any(HttpPost.class));
        assertTrue(publisher.waits.isEmpty(), "an endpoint-level status should not be retried");
    }

    @Test
    void aRegistryThatKeepsRefusingIsGivenUpOnWithAnInformativeMessage() throws Exception {
        stubPostStatuses(new PostStub(503, "Service Unavailable", null),
                new PostStub(503, "Service Unavailable", null),
                new PostStub(503, "Service Unavailable", null),
                new PostStub(503, "Service Unavailable", null));
        RecordingPublisher publisher = new RecordingPublisher();
        Nanopub nanopub = plainNanopub();

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> publisher.publishNanopub(nanopub, PUBLIC_REGISTRY_URL));

        // The message used to report the serverUrl parameter, which is null on the command-line
        // path; it has to name the registry that was actually contacted and what it said.
        assertTrue(ex.getMessage().contains(PUBLIC_REGISTRY_URL), ex.getMessage());
        assertTrue(ex.getMessage().contains("503"), ex.getMessage());
        assertTrue(ex.getMessage().contains(nanopub.getUri().stringValue()), ex.getMessage());
        assertFalse(ex.getMessage().contains("null"), ex.getMessage());
        verify(mockHttpClient, times(4)).execute(any(HttpPost.class));
    }

}
