package org.nanopub.extra.services;

import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;
import org.nanopub.NanopubUtils;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class QueryCallTest {

    private final MockedStatic<NanopubUtils> mockStatic = mockStatic(NanopubUtils.class);
    private CloseableHttpClient mockHttpClient;

    private static String[] queryApiInstancesOld;

    @BeforeAll
    static void beforeAll() {
        // Store the original queryApiInstances for later restoration
        queryApiInstancesOld = QueryCall.queryApiInstances;
    }

    @BeforeEach
    void setUp() {
        mockHttpClient = mock(CloseableHttpClient.class);
        mockStatic.when(NanopubUtils::getHttpClient).thenReturn(mockHttpClient);
    }


    @AfterEach
    void tearDown() throws NoSuchFieldException, IllegalAccessException, IOException {
        mockStatic.close();

        // Reset the static field 'checkedApiInstances' to null after each test using reflection
        Field field = QueryCall.class.getDeclaredField("checkedApiInstances");
        field.setAccessible(true);
        field.set(null, null);

        mockHttpClient.close();
    }

    @AfterAll
    static void afterAll() {
        // Restore the original queryApiInstances
        QueryCall.queryApiInstances = queryApiInstancesOld;
    }

    @Test
    void getApiInstancesWithNotAccessibleInstances() throws IOException {
        StatusLine mockStatusLine = mock(StatusLine.class);
        when(mockStatusLine.getStatusCode()).thenReturn(300);

        when(mockHttpClient.execute(any(HttpGet.class))).thenAnswer(invocation -> {
            CloseableHttpResponse response = mock(CloseableHttpResponse.class);
            when(response.getStatusLine()).thenReturn(mockStatusLine);
            when(response.getEntity()).thenReturn(mock(HttpEntity.class));
            return response;
        });

        QueryCall.queryApiInstances = new String[]{
                "https://mocked.instance1.com/",
                "https://mocked.instance2.com/",
                "https://mocked.instance3.com/"
        };
        assertThrows(RuntimeException.class, QueryCall::getApiInstances);
    }

    @Test
    void getApiInstancesWithOnlyOneInstance() throws IOException {
        StatusLine mockStatusLine = mock(StatusLine.class);
        when(mockStatusLine.getStatusCode()).thenReturn(200);

        when(mockHttpClient.execute(any(HttpGet.class))).thenAnswer(invocation -> {
            CloseableHttpResponse response = mock(CloseableHttpResponse.class);
            when(response.getStatusLine()).thenReturn(mockStatusLine);
            when(response.getEntity()).thenReturn(mock(HttpEntity.class));
            return response;
        });

        QueryCall.queryApiInstances = new String[]{"https://mocked.instance1.com/"};
        assertThrows(RuntimeException.class, QueryCall::getApiInstances);
    }

    @Test
    void getApiInstancesWithValidInstances() throws IOException {
        StatusLine mockStatusLine = mock(StatusLine.class);
        when(mockStatusLine.getStatusCode()).thenReturn(200);

        when(mockHttpClient.execute(any(HttpGet.class))).thenAnswer(invocation -> {
            CloseableHttpResponse response = mock(CloseableHttpResponse.class);
            when(response.getStatusLine()).thenReturn(mockStatusLine);
            when(response.getEntity()).thenReturn(mock(HttpEntity.class));
            return response;
        });

        QueryCall.queryApiInstances = new String[]{"https://mocked.instance1.com/", "https://mocked.instance2.com/", "https://mocked.instance3.com/"};
        List<String> apiInstances = QueryCall.getApiInstances();
        assertEquals(apiInstances, List.of(QueryCall.queryApiInstances));
    }

}