package org.nanopub.extra.server;

import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
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

}
