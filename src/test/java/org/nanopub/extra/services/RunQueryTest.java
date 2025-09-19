package org.nanopub.extra.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

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

}
