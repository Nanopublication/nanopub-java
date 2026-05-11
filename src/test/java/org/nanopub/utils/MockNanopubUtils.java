package org.nanopub.utils;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.mockito.MockedStatic;
import org.nanopub.NanopubUtils;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class MockNanopubUtils implements AutoCloseable {

    private final MockedStatic<NanopubUtils> mockedStatic;
    private final StatusLine mockStatusLine = mock(StatusLine.class);
    private volatile String nanopubQueryStatusValue = null;

    public MockNanopubUtils() throws IOException {
        this.mockedStatic = mockStatic(NanopubUtils.class);
        CloseableHttpClient mockHttpClient = mock(CloseableHttpClient.class);
        this.mockedStatic.when(NanopubUtils::getHttpClient).thenReturn(mockHttpClient);
        setHttpResponseStatusCode(300);

        when(mockHttpClient.execute(any(HttpGet.class))).thenAnswer(invocation -> {
            CloseableHttpResponse response = mock(CloseableHttpResponse.class);
            when(response.getStatusLine()).thenReturn(mockStatusLine);
            when(response.getEntity()).thenReturn(mock(HttpEntity.class));
            // By default getFirstHeader returns null (Mockito default).
            // When a status value is set, stub it via the read-side field.
            when(response.getFirstHeader(anyString())).thenAnswer(inv -> {
                String name = inv.getArgument(0);
                if ("Nanopub-Query-Status".equalsIgnoreCase(name) && nanopubQueryStatusValue != null) {
                    Header h = mock(Header.class);
                    when(h.getValue()).thenReturn(nanopubQueryStatusValue);
                    return h;
                }
                return null;
            });
            return response;
        });
    }

    public void setHttpResponseStatusCode(int statusCode) {
        when(mockStatusLine.getStatusCode()).thenReturn(statusCode);
    }

    /**
     * Sets the value of the {@code Nanopub-Query-Status} header returned on
     * every mocked response. Pass {@code null} to omit the header entirely
     * (simulating an older instance with no status reporting).
     */
    public void setNanopubQueryStatus(String status) {
        this.nanopubQueryStatusValue = status;
    }

    @Override
    public void close() {
        if (this.mockedStatic != null) {
            this.mockedStatic.close();
        }
    }

}
