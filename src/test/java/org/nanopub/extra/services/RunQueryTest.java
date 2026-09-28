package org.nanopub.extra.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

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

    @Test
    void mainPrintsATabularResponseAsCsv() {
        ApiResponse response = new ApiResponse();
        response.setHeader(new String[]{"np", "label"});
        response.add(new String[]{"https://example.org/np1", "Label 1"});

        try (MockedStatic<QueryAccess> queryAccess = Mockito.mockStatic(QueryAccess.class)) {
            queryAccess.when(() -> QueryAccess.get(Mockito.any(QueryRef.class))).thenReturn(response);
            queryAccess.when(() -> QueryAccess.printCvsResponse(Mockito.any(QueryRef.class), Mockito.any()))
                    .thenAnswer(invocation -> {
                        java.io.Writer writer = invocation.getArgument(1);
                        writer.write("np,label\n");
                        writer.flush();
                        return null;
                    });

            String printed = captureStandardOutput(() -> RunQuery.main(new String[]{QUERY_ID}));

            assertTrue(printed.contains("np,label"), printed);
        }
    }

    @Test
    void mainPrintsAnRdfResponseAsTurtle() {
        ApiResponse response = new ApiResponse();
        org.eclipse.rdf4j.model.Model model = new org.eclipse.rdf4j.model.impl.LinkedHashModel();
        model.add(org.eclipse.rdf4j.model.util.Values.iri("https://example.org/s"),
                org.eclipse.rdf4j.model.util.Values.iri("https://example.org/p"),
                org.eclipse.rdf4j.model.util.Values.iri("https://example.org/o"));
        response.setRdfContent(model);

        try (MockedStatic<QueryAccess> queryAccess = Mockito.mockStatic(QueryAccess.class)) {
            queryAccess.when(() -> QueryAccess.get(Mockito.any(QueryRef.class))).thenReturn(response);

            String printed = captureStandardOutput(() -> RunQuery.main(new String[]{QUERY_ID}));

            assertTrue(printed.contains("https://example.org/s"), printed);
        }
    }

    @Test
    void mainPassesTheGivenParametersOn() {
        ApiResponse response = new ApiResponse();
        response.setHeader(new String[]{"a"});

        try (MockedStatic<QueryAccess> queryAccess = Mockito.mockStatic(QueryAccess.class)) {
            queryAccess.when(() -> QueryAccess.get(Mockito.any(QueryRef.class))).thenReturn(response);
            queryAccess.when(() -> QueryAccess.printCvsResponse(Mockito.any(QueryRef.class), Mockito.any()))
                    .thenAnswer(invocation -> null);

            captureStandardOutput(() -> RunQuery.main(new String[]{"-p", "a=1", QUERY_ID}));

            queryAccess.verify(() -> QueryAccess.get(Mockito.argThat(ref ->
                    "1".equals(ref.getParams().get("a").iterator().next()))));
        }
    }

}
