package org.nanopub.extra.services;

import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
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

    MockedStatic<NanopubUtils> mockStatic = mockStatic(NanopubUtils.class);

    @AfterEach
    void tearDown() throws NoSuchFieldException, IllegalAccessException {
        mockStatic.close();

        // Reset the static field 'checkedApiInstances' to null after each test using reflection
        Field field = QueryCall.class.getDeclaredField("checkedApiInstances");
        field.setAccessible(true);
        field.set(null, null);
    }

    @Test
    void getApiInstancesWithNotAccessibleInstances() throws IOException {
        CloseableHttpClient mockHttpClient = mock(CloseableHttpClient.class);
        CloseableHttpResponse mockResponse = mock(CloseableHttpResponse.class);

        when(mockResponse.getStatusLine()).thenReturn(mock(StatusLine.class));
        when(mockResponse.getStatusLine().getStatusCode()).thenReturn(300);
        when(mockResponse.getEntity()).thenReturn(mock(HttpEntity.class));
        when(mockHttpClient.execute(any(HttpGet.class))).thenReturn(mockResponse);

        mockStatic.when(NanopubUtils::getHttpClient).thenReturn(mockHttpClient);

        QueryCall.queryApiInstances = new String[]{"https://mocked.instance1.com/", "https://mocked.instance2.com/", "https://mocked.instance3.com/"};
        assertThrows(RuntimeException.class, QueryCall::getApiInstances);
    }

    @Test
    void getApiInstancesWithOnlyOneInstance() throws IOException {
        CloseableHttpClient mockHttpClient = mock(CloseableHttpClient.class);
        CloseableHttpResponse mockResponse = mock(CloseableHttpResponse.class);

        when(mockResponse.getStatusLine()).thenReturn(mock(StatusLine.class));
        when(mockResponse.getStatusLine().getStatusCode()).thenReturn(200);
        when(mockResponse.getEntity()).thenReturn(mock(HttpEntity.class));
        when(mockHttpClient.execute(any(HttpGet.class))).thenReturn(mockResponse);

        mockStatic.when(NanopubUtils::getHttpClient).thenReturn(mockHttpClient);

        QueryCall.queryApiInstances = new String[]{"https://mocked.instance1.com/"};
        assertThrows(RuntimeException.class, QueryCall::getApiInstances);
    }

    @Test
    void getApiInstancesWithValidInstances() throws IOException {
        CloseableHttpClient mockHttpClient = mock(CloseableHttpClient.class);
        CloseableHttpResponse mockResponse = mock(CloseableHttpResponse.class);

        when(mockResponse.getStatusLine()).thenReturn(mock(StatusLine.class));
        when(mockResponse.getStatusLine().getStatusCode()).thenReturn(200);
        when(mockResponse.getEntity()).thenReturn(mock(HttpEntity.class));
        when(mockHttpClient.execute(any(HttpGet.class))).thenReturn(mockResponse);

        mockStatic.when(NanopubUtils::getHttpClient).thenReturn(mockHttpClient);

        QueryCall.queryApiInstances = new String[]{"https://mocked.instance1.com/", "https://mocked.instance2.com/", "https://mocked.instance3.com/"};
        List<String> apiInstances = QueryCall.getApiInstances();
        assertEquals(apiInstances, List.of(QueryCall.queryApiInstances));
    }

}