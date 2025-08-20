package org.nanopub.extra.services;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        Map<String, String> res = rq.prepareParamsMap(param);
        assertEquals(res.size(), 1);
        assertEquals(res.get("a"), "1");
    }

    @Test
    void twoParametersTest() {
        RunQuery rq = new RunQuery();
        List<String> param = Arrays.asList("a=1", "b=2");
        Map<String, String> res = rq.prepareParamsMap(param);
        assertEquals(res.size(), 2);
        assertEquals(res.get("a"), "1");
        assertEquals(res.get("b"), "2");
    }

}
