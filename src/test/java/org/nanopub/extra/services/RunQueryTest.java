package org.nanopub.extra.services;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class RunQueryTest {

    @Test
    void noParametersTest () {
        RunQuery rq = new RunQuery();
        assertThat(rq.prepareParamsMap(null)).isEmpty();
    }

    @Test
    void oneParameterTest () {
        RunQuery rq = new RunQuery();
        List<String> param = Arrays.asList("a=1");
        Map<String, String> res = rq.prepareParamsMap(param);
        assertThat(res.size()).isEqualTo(1);
        assertThat(res.get("a")).isEqualTo("1");
    }

    @Test
    void twoParametersTest () {
        RunQuery rq = new RunQuery();
        List<String> param = Arrays.asList("a=1", "b=2");
        Map<String, String> res = rq.prepareParamsMap(param);
        assertThat(res.size()).isEqualTo(2);
        assertThat(res.get("a")).isEqualTo("1");
        assertThat(res.get("b")).isEqualTo("2");
    }

}
