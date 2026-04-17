package org.nanopub.extra.services;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class QueryRefTest {

    private static final String TEST_QUERY_ID = "RAapc3jbJ3GkDy0ncKx3pok_zEKqwrT6-Z5TkCP1k96II/test-query";

    @Test
    void constructorWithInvalidQueryId() {
        assertThrows(IllegalArgumentException.class, () -> new QueryRef(null));
        assertThrows(IllegalArgumentException.class, () -> new QueryRef(""));
        assertThrows(IllegalArgumentException.class, () -> new QueryRef(" "));
        assertThrows(IllegalArgumentException.class, () -> new QueryRef("test-query"));
    }

    @Test
    void constructorWithNameAndParams() {
        Multimap<String, String> params = ArrayListMultimap.create();
        params.put("param1", "value1");
        QueryRef queryRef = new QueryRef(TEST_QUERY_ID, params);
        assertNotNull(queryRef);
    }

    @Test
    void constructorWithNameAndParam() {
        QueryRef queryRef = new QueryRef(TEST_QUERY_ID, "param1", "value1");
        assertNotNull(queryRef);
        assertEquals(1, queryRef.getParams().size());
        assertEquals("value1", queryRef.getParams().get("param1").iterator().next());
    }

    @Test
    void constructorWithNameAndParamNameNullOrEmpty() {
        assertThrows(IllegalArgumentException.class, () -> new QueryRef(TEST_QUERY_ID, null, "value1"));
        assertThrows(IllegalArgumentException.class, () -> new QueryRef(TEST_QUERY_ID, "", "value1"));
        assertThrows(IllegalArgumentException.class, () -> new QueryRef(TEST_QUERY_ID, " ", "value1"));
    }

    @Test
    void constructorWithName() {
        QueryRef queryRef = new QueryRef(TEST_QUERY_ID);
        assertNotNull(queryRef);
        assertTrue(queryRef.getParams().isEmpty());
    }

    @Test
    void getQueryId() {
        QueryRef queryRef = new QueryRef(TEST_QUERY_ID);
        assertNotNull(queryRef.getQueryId());
        assertEquals(TEST_QUERY_ID, queryRef.getQueryId());
    }

    @Test
    void getParams() {
        Multimap<String, String> params = ArrayListMultimap.create();
        params.put("param1", "value1");
        QueryRef queryRef = new QueryRef(TEST_QUERY_ID, params);
        assertNotNull(queryRef.getParams());
        assertEquals(1, queryRef.getParams().size());
        assertEquals("value1", queryRef.getParams().get("param1").iterator().next());
    }

}