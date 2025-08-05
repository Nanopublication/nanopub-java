package org.nanopub.fdo;

import org.junit.jupiter.api.Test;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.FailedApiCallException;

import static org.junit.jupiter.api.Assertions.*;

class FdoQueryTest {

    @Test
    void textSearch() throws FailedApiCallException {
        String query = "test";
        ApiResponse response = FdoQuery.textSearch(query);
        assertNotNull(response);
        assertFalse(response.getData().isEmpty());
    }

    @Test
    void findByRef() throws FailedApiCallException {
        String ref = "21.T11966/82045bd97a0acce88378";
        ApiResponse response = FdoQuery.findByRef(ref);
        assertNotNull(response);
        assertFalse(response.getData().isEmpty());
    }

    @Test
    void getFeed() throws Exception {
        String creator = "https://orcid.org/0009-0008-3635-347X";
        ApiResponse response = FdoQuery.getFeed(creator);
        assertNotNull(response);
        assertTrue(response.getData().size() > 5);
    }

    @Test
    void getFavoriteThings() throws Exception {
        String creator = "https://orcid.org/0000-0002-1267-0234";
        ApiResponse response = FdoQuery.getFavoriteThings(creator);
        assertNotNull(response);
        assertTrue(response.getData().size() > 1);
    }

}