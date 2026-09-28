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

    @Test
    void parseStringWithoutParameters() {
        QueryRef queryRef = QueryRef.parseString(TEST_QUERY_ID);

        assertEquals(TEST_QUERY_ID, queryRef.getQueryId());
        assertTrue(queryRef.getParams().isEmpty());
        assertEquals(TEST_QUERY_ID + "?", queryRef.getAsUrlString());
    }

    @Test
    void parseStringWithParameters() {
        QueryRef queryRef = QueryRef.parseString(TEST_QUERY_ID + "?a=1&b=two+words");

        assertEquals(TEST_QUERY_ID, queryRef.getQueryId());
        assertEquals("1", queryRef.getParams().get("a").iterator().next());
        assertEquals("two words", queryRef.getParams().get("b").iterator().next());
    }

    @Test
    void parseStringWithATrailingQuestionMark() {
        QueryRef queryRef = QueryRef.parseString(TEST_QUERY_ID + "?");

        assertEquals(TEST_QUERY_ID, queryRef.getQueryId());
        assertTrue(queryRef.getParams().isEmpty());
    }

    @Test
    void getAsUrlStringSortsAndEncodesTheParameters() {
        Multimap<String, String> params = ArrayListMultimap.create();
        params.put("b", "second");
        params.put("a", "two words");
        params.put("a", "first");
        QueryRef queryRef = new QueryRef(TEST_QUERY_ID, params);

        assertEquals(TEST_QUERY_ID + "?a=first&a=two+words&b=second", queryRef.getAsUrlString());
    }

    @Test
    void getAsUrlStringWritesPlaceholdersForANullKeyOrValue() {
        Multimap<String, String> nullKey = ArrayListMultimap.create();
        nullKey.put(null, "a value");
        assertEquals(TEST_QUERY_ID + "?$null=a+value", new QueryRef(TEST_QUERY_ID, nullKey).getAsUrlString());

        Multimap<String, String> nullValue = ArrayListMultimap.create();
        nullValue.put("a", null);
        assertEquals(TEST_QUERY_ID + "?a=", new QueryRef(TEST_QUERY_ID, nullValue).getAsUrlString());
    }

    @Test
    void getAsUrlStringCannotSortNullsAlongsideOtherParameters() {
        // with more than one parameter the list gets sorted first, and the comparators
        // reach the null before the placeholders above are ever applied
        Multimap<String, String> params = ArrayListMultimap.create();
        params.put(null, "a value");
        params.put("z", "other");

        assertThrows(NullPointerException.class, () -> new QueryRef(TEST_QUERY_ID, params).getAsUrlString());
    }

    @Test
    void toStringIsTheUrlString() {
        QueryRef queryRef = new QueryRef(TEST_QUERY_ID);

        assertEquals(queryRef.getAsUrlString(), queryRef.toString());
    }

    @Test
    void getAsUrlStringIsComputedOnlyOnce() {
        QueryRef queryRef = new QueryRef(TEST_QUERY_ID);

        assertSame(queryRef.getAsUrlString(), queryRef.getAsUrlString());
    }

}
