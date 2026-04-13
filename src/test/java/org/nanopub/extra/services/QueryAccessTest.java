package org.nanopub.extra.services;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class QueryAccessTest {

    private static final String QUERY_ID = "RAcjK5MtLviwMCuVwkRIknLxOJj0qZwMCPoZn1TCd5Occ/sparql-construct-query-test";

    private HttpResponse mockResponse(String body, String contentType) throws Exception {
        HttpResponse resp = mock(HttpResponse.class);

        StatusLine statusLine = mock(StatusLine.class);
        when(statusLine.getStatusCode()).thenReturn(200);
        when(resp.getStatusLine()).thenReturn(statusLine);

        HttpEntity entity = mock(HttpEntity.class);
        when(entity.getContent()).thenReturn(new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)));
        when(entity.getContentLength()).thenReturn((long) body.length());
        when(resp.getEntity()).thenReturn(entity);

        Header contentTypeHeader = mock(Header.class);
        when(contentTypeHeader.getValue()).thenReturn(contentType);
        when(resp.getFirstHeader("Content-Type")).thenReturn(contentTypeHeader);

        return resp;
    }

    @Test
    void constructQueryWithTurtleResponseReturnsRdfModel() throws Exception {
        String turtle = "@prefix ex: <http://example.org/> .\nex:subject ex:predicate ex:object .\n";
        HttpResponse resp = mockResponse(turtle, "text/turtle");

        try (MockedStatic<QueryCall> mockedQueryCall = mockStatic(QueryCall.class)) {
            mockedQueryCall.when(() -> QueryCall.run(any(QueryRef.class))).thenReturn(resp);

            ApiResponse response = QueryAccess.get(new QueryRef(QUERY_ID));

            assertTrue(response.isRdfResponse());
            assertNotNull(response.getRdfContent());
            assertFalse(response.getRdfContent().isEmpty());
            assertTrue(response.getData().isEmpty());
        }
    }

    @Test
    void constructQueryWithContentTypeParamsReturnsRdfModel() throws Exception {
        String turtle = "@prefix ex: <http://example.org/> .\nex:s ex:p ex:o .\n";
        HttpResponse resp = mockResponse(turtle, "text/turtle; charset=UTF-8");

        try (MockedStatic<QueryCall> mockedQueryCall = mockStatic(QueryCall.class)) {
            mockedQueryCall.when(() -> QueryCall.run(any(QueryRef.class))).thenReturn(resp);

            ApiResponse response = QueryAccess.get(new QueryRef(QUERY_ID));

            assertTrue(response.isRdfResponse());
            assertFalse(response.getRdfContent().isEmpty());
        }
    }

    @Test
    void selectQueryWithCsvResponseReturnsTabularData() throws Exception {
        String csv = "np,label\nhttps://example.org/np1,Label 1\n";
        HttpResponse resp = mockResponse(csv, "text/csv");

        try (MockedStatic<QueryCall> mockedQueryCall = mockStatic(QueryCall.class)) {
            mockedQueryCall.when(() -> QueryCall.run(any(QueryRef.class))).thenReturn(resp);

            ApiResponse response = QueryAccess.get(new QueryRef(QUERY_ID));

            assertFalse(response.isRdfResponse());
            assertNull(response.getRdfContent());
            assertEquals(1, response.size());
            assertEquals("https://example.org/np1", response.getData().getFirst().get("np"));
            assertEquals("Label 1", response.getData().getFirst().get("label"));
        }
    }

}
