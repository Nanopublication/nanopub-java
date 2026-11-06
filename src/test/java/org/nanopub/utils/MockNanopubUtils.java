package org.nanopub.utils;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicStatusLine;
import org.mockito.MockedStatic;
import org.nanopub.NanopubUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class MockNanopubUtils implements AutoCloseable {

    /**
     * Answers a request in place of the default response.
     */
    @FunctionalInterface
    public interface RequestHandler {

        /**
         * @param url the requested URL
         * @return the response, or {@code null} to serve the default one
         * @throws IOException to make the request fail
         */
        CloseableHttpResponse handle(String url) throws IOException;

    }

    private final MockedStatic<NanopubUtils> mockedStatic;
    private final StatusLine mockStatusLine = mock(StatusLine.class);
    private final List<String> requestedUrls = new CopyOnWriteArrayList<>();
    private volatile String nanopubQueryStatusValue = null;
    private volatile String responseBody = null;
    private volatile String contentType = null;
    private volatile RequestHandler requestHandler = null;

    public MockNanopubUtils() throws IOException {
        this.mockedStatic = mockStatic(NanopubUtils.class);
        CloseableHttpClient mockHttpClient = mock(CloseableHttpClient.class);
        this.mockedStatic.when(NanopubUtils::getHttpClient).thenReturn(mockHttpClient);
        setHttpResponseStatusCode(300);

        when(mockHttpClient.execute(any(HttpGet.class))).thenAnswer(invocation -> {
            String url = invocation.<HttpGet>getArgument(0).getURI().toString();
            requestedUrls.add(url);
            RequestHandler handler = requestHandler;
            if (handler != null) {
                CloseableHttpResponse response = handler.handle(url);
                if (response != null) {
                    return response;
                }
            }
            return createResponse(mockStatusLine, name -> {
                if ("Nanopub-Query-Status".equalsIgnoreCase(name)) {
                    return nanopubQueryStatusValue;
                }
                if ("Content-Type".equalsIgnoreCase(name)) {
                    return contentType;
                }
                return null;
            });
        });
    }

    private CloseableHttpResponse createResponse(StatusLine statusLine, Function<String, String> headerValues) {
        CloseableHttpResponse response = mock(CloseableHttpResponse.class);
        when(response.getStatusLine()).thenReturn(statusLine);
        HttpEntity entity = mock(HttpEntity.class);
        try {
            // Each call gets its own stream, so a response can be consumed more than once
            // over the lifetime of the mock.
            when(entity.getContent()).thenAnswer(inv -> new ByteArrayInputStream(
                    (responseBody == null ? "" : responseBody).getBytes(StandardCharsets.UTF_8)));
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
        when(response.getEntity()).thenReturn(entity);
        // Mockito's default for array-returning methods is null, not an empty array.
        when(response.getHeaders(anyString())).thenReturn(new Header[0]);
        when(response.getFirstHeader(anyString())).thenAnswer(inv -> {
            String value = headerValues.apply(inv.getArgument(0));
            if (value == null) {
                return null;
            }
            Header h = mock(Header.class);
            when(h.getValue()).thenReturn(value);
            return h;
        });
        return response;
    }

    /**
     * Creates a response with the given status code and headers, for use in a
     * {@link RequestHandler}. It serves the body set with {@link #setResponseBody(String)}.
     *
     * @param statusCode the HTTP status code
     * @param headers    the response headers; names are matched case-insensitively
     * @return the response
     */
    public CloseableHttpResponse response(int statusCode, Map<String, String> headers) {
        Map<String, String> headerMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        headerMap.putAll(headers);
        return createResponse(new BasicStatusLine(HttpVersion.HTTP_1_1, statusCode, null), headerMap::get);
    }

    /**
     * Sets a handler that answers requests in place of the default response.
     * Pass {@code null} to serve the default response to every request.
     */
    public void setRequestHandler(RequestHandler requestHandler) {
        this.requestHandler = requestHandler;
    }

    /**
     * Returns the URLs of all requests made so far, in order.
     */
    public List<String> getRequestedUrls() {
        return List.copyOf(requestedUrls);
    }

    /**
     * Sets the body that every mocked response serves. Pass {@code null} for an empty body.
     */
    public void setResponseBody(String responseBody) {
        this.responseBody = responseBody;
    }

    /**
     * Sets the {@code Content-Type} header of every mocked response. Pass {@code null} to omit it.
     */
    public void setContentType(String contentType) {
        this.contentType = contentType;
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
