package org.nanopub.fdo;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.nanopub.extra.services.APINotReachableException;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.ApiResponseEntry;
import org.nanopub.extra.services.FailedApiCallException;
import org.nanopub.extra.services.NotEnoughAPIInstancesException;
import org.nanopub.extra.services.QueryAccess;

class FdoQueryTest {

    @Test
    void textSearch() throws FailedApiCallException, APINotReachableException, NotEnoughAPIInstancesException {
        String query = "test";
        try (MockedStatic<QueryAccess> mockedQueryAccess = mockStatic(QueryAccess.class)) {
            ApiResponse mockedResponse = mock(ApiResponse.class);
            when(mockedResponse.getData()).thenReturn(List.of(
                    mock(ApiResponseEntry.class),
                    mock(ApiResponseEntry.class)
            ));
            mockedQueryAccess.when(() -> QueryAccess.get(any())).thenReturn(mockedResponse);

            ApiResponse response = FdoQuery.textSearch(query);
            assertNotNull(response);
            assertFalse(response.getData().isEmpty());
        }
    }

    @Test
    void findByRef() throws FailedApiCallException, APINotReachableException, NotEnoughAPIInstancesException {
        String ref = "21.T11966/82045bd97a0acce88378";
        try (MockedStatic<QueryAccess> mockedQueryAccess = mockStatic(QueryAccess.class)) {
            ApiResponse mockedResponse = mock(ApiResponse.class);
            when(mockedResponse.getData()).thenReturn(List.of(
                    mock(ApiResponseEntry.class),
                    mock(ApiResponseEntry.class)
            ));
            mockedQueryAccess.when(() -> QueryAccess.get(any())).thenReturn(mockedResponse);

            ApiResponse response = FdoQuery.findByRef(ref);
            assertNotNull(response);
            assertFalse(response.getData().isEmpty());
        }
    }

    @Test
    void getFeed() throws Exception {
        String creator = "https://orcid.org/0009-0008-3635-347X";
        try (MockedStatic<QueryAccess> mockedQueryAccess = mockStatic(QueryAccess.class)) {
            ApiResponse mockedResponse = mock(ApiResponse.class);
            when(mockedResponse.getData()).thenReturn(List.of(
                    mock(ApiResponseEntry.class),
                    mock(ApiResponseEntry.class),
                    mock(ApiResponseEntry.class),
                    mock(ApiResponseEntry.class),
                    mock(ApiResponseEntry.class),
                    mock(ApiResponseEntry.class)
            ));
            mockedQueryAccess.when(() -> QueryAccess.get(any())).thenReturn(mockedResponse);

            ApiResponse response = FdoQuery.getFeed(creator);
            assertNotNull(response);
            assertTrue(response.getData().size() > 5);
        }
    }

    @Test
    void getFavoriteThings() throws Exception {
        String creator = "https://orcid.org/0000-0002-1267-0234";
        try (MockedStatic<QueryAccess> mockedQueryAccess = mockStatic(QueryAccess.class)) {
            ApiResponse mockedResponse = mock(ApiResponse.class);
            when(mockedResponse.getData()).thenReturn(List.of(
                    mock(ApiResponseEntry.class),
                    mock(ApiResponseEntry.class)
            ));
            mockedQueryAccess.when(() -> QueryAccess.get(any())).thenReturn(mockedResponse);

            ApiResponse response = FdoQuery.getFavoriteThings(creator);
            assertNotNull(response);
            assertTrue(response.getData().size() > 1);
        }
    }

}