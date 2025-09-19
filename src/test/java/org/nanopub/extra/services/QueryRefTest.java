package org.nanopub.extra.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

class QueryRefTest {

    @Test
    void constructorWithNullQueryNameNullOrEmpty() {
        assertThrows(IllegalArgumentException.class, () -> new QueryRef(null));
        assertThrows(IllegalArgumentException.class, () -> new QueryRef(""));
        assertThrows(IllegalArgumentException.class, () -> new QueryRef(" "));
    }

    @Test
    void constructorWithNameAndParams() {
        Multimap<String, String> params = ArrayListMultimap.create();
        params.put("param1", "value1");
        QueryRef queryRef = new QueryRef("test-query", params);
        assertNotNull(queryRef);
    }

    @Test
    void constructorWithNameAndParam() {
        QueryRef queryRef = new QueryRef("test-query", "param1", "value1");
        assertNotNull(queryRef);
        assertEquals(1, queryRef.getParams().size());
        assertEquals("value1", queryRef.getParams().get("param1").iterator().next());
    }

    @Test
    void constructorWithNameAndParamNameNullOrEmpty() {
        assertThrows(IllegalArgumentException.class, () -> new QueryRef("test-query", null, "value1"));
        assertThrows(IllegalArgumentException.class, () -> new QueryRef("test-query", "", "value1"));
        assertThrows(IllegalArgumentException.class, () -> new QueryRef("test-query", " ", "value1"));
    }

    @Test
    void constructorWithName() {
        QueryRef queryRef = new QueryRef("test-query");
        assertNotNull(queryRef);
        assertTrue(queryRef.getParams().isEmpty());
    }

    @Test
    void getName() {
        QueryRef queryRef = new QueryRef("test-query");
        assertNotNull(queryRef.getName());
        assertEquals("test-query", queryRef.getName());
    }

    @Test
    void getParams() {
        Multimap<String, String> params = ArrayListMultimap.create();
        params.put("param1", "value1");
        QueryRef queryRef = new QueryRef("test-query", params);
        assertNotNull(queryRef.getParams());
        assertEquals(1, queryRef.getParams().size());
        assertEquals("value1", queryRef.getParams().get("param1").iterator().next());
    }

}