package org.nanopub.extra.services;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ApiResponseEntryTest {

    @Test
    void defaultConstructor() {
        ApiResponseEntry entry = new ApiResponseEntry();
        assertTrue(entry.getKeys().isEmpty());
    }

    @Test
    void getKeysReturnsAllKeysInEntry() {
        ApiResponseEntry entry = new ApiResponseEntry();
        entry.add("key1", "value1");
        entry.add("key2", "value2");
        assertEquals(Set.of("key1", "key2"), entry.getKeys());
    }

    @Test
    void getAsBooleanReturnsTrueForStringTrue() {
        ApiResponseEntry entry = new ApiResponseEntry();
        entry.add("key", "true");
        assertTrue(entry.getAsBoolean("key"));
    }

    @Test
    void getAsBooleanReturnsFalseForStringFalse() {
        ApiResponseEntry entry = new ApiResponseEntry();
        entry.add("key", "false");
        assertFalse(entry.getAsBoolean("key"));
    }

    @Test
    void getAsBooleanReturnsTrueForOneAsString() {
        ApiResponseEntry entry = new ApiResponseEntry();
        entry.add("key", "1");
        assertTrue(entry.getAsBoolean("key"));
    }

    @Test
    void getAsBooleanReturnsFalseForAnyNumberNotOneAsString() {
        ApiResponseEntry entry = new ApiResponseEntry();
        entry.add("key1", "0");
        entry.add("key2", "-10");
        assertFalse(entry.getAsBoolean("key1"));
        assertFalse(entry.getAsBoolean("key2"));
    }

    @Test
    void getAsBooleanThrowsNullPointerForNullValue() {
        ApiResponseEntry entry = new ApiResponseEntry();
        assertThrows(NullPointerException.class, () -> entry.getAsBoolean("key"));
    }

    @Test
    void getAsBooleanReturnsFalseForNonBooleanString() {
        ApiResponseEntry entry = new ApiResponseEntry();
        entry.add("key", "random");
        assertFalse(entry.getAsBoolean("key"));
    }

    @Test
    void dataComparatorCompare() {
        ApiResponseEntry entry1 = new ApiResponseEntry();
        entry1.add("key1", "value1");

        ApiResponseEntry entry2 = new ApiResponseEntry();
        entry2.add("key1", "value1");

        ApiResponseEntry.DataComparator comparator = new ApiResponseEntry.DataComparator();
        assertEquals(0, comparator.compare(entry1, entry2));

        ApiResponseEntry entry3 = new ApiResponseEntry();
        entry3.add("date", "value3");
        assertEquals(1, comparator.compare(entry1, entry3));
        assertEquals(-1, comparator.compare(entry3, entry1));

        ApiResponseEntry entry4 = new ApiResponseEntry();
        entry4.add("date", "value3");
        assertEquals(Integer.class, ((Object) comparator.compare(entry3, entry4)).getClass());
    }

}