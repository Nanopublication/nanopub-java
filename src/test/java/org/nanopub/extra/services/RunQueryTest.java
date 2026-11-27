package org.nanopub.extra.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.google.common.collect.Multimap;

public class RunQueryTest {

    @Test
    void noParametersTest() {
        RunQuery rq = new RunQuery();
        assertTrue(rq.prepareParamsMap(null).isEmpty());
    }

    @Test
    void oneParameterTest() {
        RunQuery rq = new RunQuery();
        List<String> param = List.of("a=1");
        Multimap<String, String> res = rq.prepareParamsMap(param);
        assertEquals(res.size(), 1);
        assertEquals(res.get("a").iterator().next(), "1");
    }

    @Test
    void twoParametersTest() {
        RunQuery rq = new RunQuery();
        List<String> param = Arrays.asList("a=1", "b=2");
        Multimap<String, String> res = rq.prepareParamsMap(param);
        assertEquals(res.size(), 2);
        assertEquals(res.get("a").iterator().next(), "1");
        assertEquals(res.get("b").iterator().next(), "2");
    }

    private static final String QUERY_ID = "RAcjK5MtLviwMCuVwkRIknLxOJj0qZwMCPoZn1TCd5Occ/test-query";

    private static String captureStandardOutput(Runnable action) {
        java.io.PrintStream originalOut = System.out;
        java.io.ByteArrayOutputStream captured = new java.io.ByteArrayOutputStream();
        try {
            System.setOut(new java.io.PrintStream(captured, true, java.nio.charset.StandardCharsets.UTF_8));
            action.run();
        } finally {
            System.setOut(originalOut);
        }
        return captured.toString(java.nio.charset.StandardCharsets.UTF_8);
    }

    /**
     * An answer from a query server with the given content type and body.
     */
    private static HttpResponse answer(String contentType, String body) {
        BasicHttpResponse response = new BasicHttpResponse(new BasicStatusLine(HttpVersion.HTTP_1_1, 200, "OK"));
        response.setHeader("Content-Type", contentType);
        response.setEntity(new StringEntity(body, StandardCharsets.UTF_8));
        return response;
    }

    @Test
    void mainPrintsATabularResponseAsCsv() {
        try (MockedStatic<QueryCall> queryCall = Mockito.mockStatic(QueryCall.class)) {
            queryCall.when(() -> QueryCall.run(Mockito.any(QueryRef.class)))
                    .thenReturn(answer("text/csv; charset=utf-8", "np,label\nhttps://example.org/np1,Label 1\n"));

            String printed = captureStandardOutput(() -> RunQuery.main(new String[]{QUERY_ID}));

            assertEquals("\"np\",\"label\"\n\"https://example.org/np1\",\"Label 1\"\n", printed);
            queryCall.verify(() -> QueryCall.run(Mockito.any(QueryRef.class)), Mockito.times(1));
        }
    }

    @Test
    void mainPrintsAnRdfResponseAsTurtle() {
        try (MockedStatic<QueryCall> queryCall = Mockito.mockStatic(QueryCall.class)) {
            queryCall.when(() -> QueryCall.run(Mockito.any(QueryRef.class)))
                    .thenReturn(answer("text/turtle", "<https://example.org/s> <https://example.org/p> <https://example.org/o> .\n"));

            String printed = captureStandardOutput(() -> RunQuery.main(new String[]{QUERY_ID}));

            assertTrue(printed.contains("<https://example.org/s> <https://example.org/p> <https://example.org/o>"), printed);
            queryCall.verify(() -> QueryCall.run(Mockito.any(QueryRef.class)), Mockito.times(1));
        }
    }

    @Test
    void mainPassesTheGivenParametersOn() {
        try (MockedStatic<QueryCall> queryCall = Mockito.mockStatic(QueryCall.class)) {
            queryCall.when(() -> QueryCall.run(Mockito.any(QueryRef.class))).thenReturn(answer("text/csv", "a\n"));

            captureStandardOutput(() -> RunQuery.main(new String[]{"-p", "a=1", QUERY_ID}));

            queryCall.verify(() -> QueryCall.run(Mockito.argThat(ref ->
                    "1".equals(ref.getParams().get("a").iterator().next()))));
        }
    }

}
