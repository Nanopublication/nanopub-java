package org.nanopub.fdo.rest.gson;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ValueTest {

    @Test
    void valueWithValidTypeAndData() {
        Value value = new Value();
        value.type = String.class.getSimpleName();
        value.data = new Data();
        value.data.format = "JSON";
        value.data.value = "{\"key\":\"value\"}";

        assertEquals("String", value.type);
        assertNotNull(value.data);
        assertEquals("JSON", value.data.format);
        assertEquals("{\"key\":\"value\"}", value.data.value);
    }

    @Test
    void valueWithNullType() {
        Value value = new Value();
        value.type = null;
        value.data = new Data();
        value.data.format = "JSON";
        value.data.value = "{\"key\":\"value\"}";

        assertNull(value.type);
        assertNotNull(value.data);
        assertEquals("JSON", value.data.format);
        assertEquals("{\"key\":\"value\"}", value.data.value);
    }

    @Test
    void valueWithNullData() {
        Value value = new Value();
        value.type = Integer.class.getSimpleName();
        value.data = null;

        assertEquals("Integer", value.type);
        assertNull(value.data);
    }

    @Test
    void valueWithEmptyTypeAndData() {
        Value value = new Value();
        value.type = "";
        value.data = new Data();
        value.data.format = "";
        value.data.value = "";

        assertEquals("", value.type);
        assertNotNull(value.data);
        assertEquals("", value.data.format);
        assertEquals("", value.data.value);
    }

}