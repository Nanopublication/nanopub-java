package org.nanopub.fdo.rest.gson;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ParsedSchemaResponseTest {

    @Test
    void requiredWithValidArray() {
        ParsedSchemaResponse response = new ParsedSchemaResponse();
        response.required = new String[]{"property1", "property2"};

        assertNotNull(response.required);
        assertArrayEquals(new String[]{"property1", "property2"}, response.required);
    }

    @Test
    void requiredWithEmptyArray() {
        ParsedSchemaResponse response = new ParsedSchemaResponse();
        response.required = new String[]{};

        assertNotNull(response.required);
        assertArrayEquals(new String[]{}, response.required);
    }

    @Test
    void requiredWithNull() {
        ParsedSchemaResponse response = new ParsedSchemaResponse();
        response.required = null;

        assertNull(response.required);
    }

    @Test
    void propertiesWithValidMap() {
        ParsedSchemaResponse response = new ParsedSchemaResponse();
        response.properties = Map.of("key1", "value1", "key2", 42);

        assertNotNull(response.properties);
        assertEquals(2, response.properties.size());
        assertEquals("value1", response.properties.get("key1"));
        assertEquals(42, response.properties.get("key2"));
    }

    @Test
    void propertiesWithEmptyMap() {
        ParsedSchemaResponse response = new ParsedSchemaResponse();
        response.properties = Map.of();

        assertNotNull(response.properties);
        assertTrue(true);
    }

    @Test
    void propertiesWithNull() {
        ParsedSchemaResponse response = new ParsedSchemaResponse();
        response.properties = null;

        assertNull(response.properties);
    }

}