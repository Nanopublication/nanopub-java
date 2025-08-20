package org.nanopub.utils;

import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.mockito.MockedStatic;
import org.nanopub.NanopubUtils;

import java.io.IOException;

import static org.mockito.Mockito.*;

public class MockNanopubUtils implements AutoCloseable {

    private final MockedStatic<NanopubUtils> mockedStatic;
    private final StatusLine mockStatusLine = mock(StatusLine.class);

    public MockNanopubUtils() throws IOException {
        this.mockedStatic = mockStatic(NanopubUtils.class);
        CloseableHttpClient mockHttpClient = mock(CloseableHttpClient.class);
        this.mockedStatic.when(NanopubUtils::getHttpClient).thenReturn(mockHttpClient);
        setHttpResponseStatusCode(300);

        when(mockHttpClient.execute(any(HttpGet.class))).thenAnswer(invocation -> {
            CloseableHttpResponse response = mock(CloseableHttpResponse.class);
            when(response.getStatusLine()).thenReturn(mockStatusLine);
            when(response.getEntity()).thenReturn(mock(HttpEntity.class));
            return response;
        });
    }

    public void setHttpResponseStatusCode(int statusCode) {
        when(mockStatusLine.getStatusCode()).thenReturn(statusCode);
    }

    @Override
    public void close() {
        if (this.mockedStatic != null) {
            this.mockedStatic.close();
        }
    }

}
