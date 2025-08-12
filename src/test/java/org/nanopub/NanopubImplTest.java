package org.nanopub;

import net.trustyuri.TrustyUriUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.nanopub.trusty.TrustyNanopubUtils;
import org.nanopub.utils.MockFileService;
import org.nanopub.utils.MockFileServiceExtension;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockFileServiceExtension.class)
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
            when(mockEntity.getContent()).thenReturn(new FileInputStream(MockFileService.getValidAndSignedNanopubFromId(npId)));
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
    void equalsTextBlockWithSameLineSeparator() throws Exception {
        try (MockedStatic<NanopubUtils> nanopubUtilsMock = mockStatic(NanopubUtils.class, CALLS_REAL_METHODS)) {
            nanopubUtilsMock.when(NanopubUtils::getHttpClient).thenReturn(mockHttpClient);

            NanopubImpl nanopub1 = new NanopubImpl(new URL("https://w3id.org/np/RA6T-YLqLnYd5XfnqR9PaGUjCzudvHdYjcG4GvOc7fdpA"));
            NanopubImpl nanopub2 = new NanopubImpl(new File(Objects.requireNonNull(this.getClass().getResource("/testsuite/valid/signed/RA6T-YLqLnYd5XfnqR9PaGUjCzudvHdYjcG4GvOc7fdpA.trig")).getPath()), RDFFormat.TRIG);

            assertTrue(TrustyNanopubUtils.isValidTrustyNanopub(nanopub1));
            assertTrue(TrustyNanopubUtils.isValidTrustyNanopub(nanopub2));
            assertEquals(nanopub1, nanopub2);
        }
    }

    @Test
    void equalsTextBlockWithDifferentLineSeparator() throws IOException, MalformedNanopubException {
        try (MockedStatic<NanopubUtils> nanopubUtilsMock = mockStatic(NanopubUtils.class, CALLS_REAL_METHODS)) {
            nanopubUtilsMock.when(NanopubUtils::getHttpClient).thenReturn(mockHttpClient);

            NanopubImpl nanopub1 = new NanopubImpl(new URL("https://w3id.org/np/RA6T-YLqLnYd5XfnqR9PaGUjCzudvHdYjcG4GvOc7fdpA"));
            NanopubImpl nanopub2 = new NanopubImpl(new File(Objects.requireNonNull(this.getClass().getResource("/testsuite/valid/signed/RA6T-YLqLnYd5XfnqR9PaGUjCzudvHdYjcG4GvOc7fdpA-all-LF.trig")).getPath()), RDFFormat.TRIG);

            assertTrue(TrustyNanopubUtils.isValidTrustyNanopub(nanopub1));
            assertFalse(TrustyNanopubUtils.isValidTrustyNanopub(nanopub2));
            assertNotEquals(nanopub1, nanopub2);
        }
    }

}