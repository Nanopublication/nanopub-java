package org.nanopub.fdo.rest.gson;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.nanopub.fdo.FdoUtils.PROFILE_HANDLE;

class DataTest {

    @Test
    void dataWithEmptyFormatAndValue() {
        Data data = new Data();
        data.format = "";
        data.value = "";

        assertEquals("", data.format);
        assertEquals("", data.value);
    }

    @Test
    void dataWithValidFormatAndValue() {
        Data data = new Data();
        data.format = "JSON";
        data.value = "{\"key\":\"value\"}";

        assertEquals("JSON", data.format);
        assertEquals("{\"key\":\"value\"}", data.value);
    }

    @Test
    void dataWithNullValue_returnsNullValue() {
        Data data = new Data();
        data.format = PROFILE_HANDLE;
        data.value = null;

        assertEquals(PROFILE_HANDLE, data.format);
        assertNull(data.value);
    }

}