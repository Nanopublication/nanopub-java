package org.nanopub.extra.services;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ApiResponseTest {

    @Test
    void defaultConstructorCreatesEmptyResponse() {
        ApiResponse response = new ApiResponse();
        assertNull(response.getHeader());
        assertTrue(response.getData().isEmpty());
    }

    @Test
    void constructorWithEntriesInitializesData() {
        ApiResponseEntry entry1 = new ApiResponseEntry();
        ApiResponseEntry entry2 = new ApiResponseEntry();
        ApiResponse response = new ApiResponse(List.of(entry1, entry2));
        assertEquals(2, response.size());
        assertTrue(response.getData().contains(entry1));
        assertTrue(response.getData().contains(entry2));
    }

    @Test
    void addEntryAsApiResponseEntry() {
        ApiResponse response = new ApiResponse();
        ApiResponseEntry entry = new ApiResponseEntry();
        response.add(entry);
        assertEquals(1, response.size());
        assertTrue(response.getData().contains(entry));
    }

    @Test
    void addEntryAsString() {
        ApiResponse response = new ApiResponse();
        response.setHeader(new String[]{"key1", "key2"});
        response.add(new String[]{"value1", "value2"});

        ApiResponseEntry entry = response.getData().get(0);

        assertEquals(1, response.size());
        assertEquals(response.getData().size(), 1);
        assertEquals("value1", entry.get("key1"));
        assertEquals("value2", entry.get("key2"));
    }

    @Test
    void setHeaderUpdatesHeader() {
        ApiResponse response = new ApiResponse();
        String[] header = {"column1", "column2"};
        response.setHeader(header);
        assertArrayEquals(header, response.getHeader());
    }

    @Test
    void sizeTest() {
        ApiResponse response = new ApiResponse();
        assertEquals(0, response.size());
        response.add(new ApiResponseEntry());
        assertEquals(1, response.size());
    }

}
