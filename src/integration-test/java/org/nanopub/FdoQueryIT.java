package org.nanopub;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.nanopub.extra.server.GetNanopub;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.ApiResponseEntry;
import org.nanopub.extra.services.FailedApiCallException;
import org.nanopub.extra.services.QueryAccess;
import org.nanopub.fdo.FdoQuery;
import org.nanopub.fdo.RetrieveFdo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class FdoQueryIT {

    @Test
    void testTextQuery() throws Exception {
        ApiResponse response = FdoQuery.textSearch("test");
        Assertions.assertTrue(response.getData().size() > 1);
    }

    @Test
    void testRefQuery() throws Exception {
        ApiResponse response = FdoQuery.findByRef("21.T11966/82045bd97a0acce88378");
        Assertions.assertTrue(response.getData().size() > 0);
    }

        @Test
    void testFeedQuery() throws Exception {
        ApiResponse response = FdoQuery.getFeed("https://orcid.org/0009-0008-3635-347X");
        Assertions.assertTrue(response.getData().size() > 5);
    }

    @Test
    void testFavoriteThingsQuery() throws Exception {
        ApiResponse response = FdoQuery.getFavoriteThings("https://orcid.org/0000-0002-1267-0234");
        Assertions.assertTrue(response.getData().size() > 1);
    }

    @Test
    public void testSimpleQuery() throws Exception {

        // url = "https://w3id.org/np/RAPf3B_OK7X4oN7c-PLztmiuL0dIV94joR6WydTjA6Asc#get-most-frequent-a-pred"
        String queryId = "RAPf3B_OK7X4oN7c-PLztmiuL0dIV94joR6WydTjA6Asc/get-most-frequent-a-pred";
        ApiResponse apiResponse = QueryAccess.get(queryId, null);
        List<ApiResponseEntry> data = apiResponse.getData();
        for (ApiResponseEntry entry : data) {
            String result = entry.get("pred");
            assertNotNull(result);
        }
    }

    @Test
    public void testQueryNanopubNetworkWithFdoHandle () throws Exception {
        Map<String, String> params = new HashMap<>();
        params.put("fdoid", "21.T11967/39b0ec87d17a4856c5f7");
        ApiResponse apiResponse = QueryAccess.get(RetrieveFdo.GET_FDO_QUERY_ID, params);
        List<ApiResponseEntry> data = apiResponse.getData();
        String npref = data.get(0).get("np");

        Nanopub np = GetNanopub.get(npref);
        System.out.println("npref: " + npref);
        assertEquals(npref, np.getUri().stringValue());
    }

    @Test
    public void testQueryNanopubNetworkNpUriForFdo () throws FailedApiCallException {
        Map<String, String> params = new HashMap<>();
        params.put("fdoid", "https://w3id.org/np/RAsSeIyT03LnZt3QvtwUqIHSCJHWW1YeLkyu66Lg4FeBk/nanodash-readme");
        ApiResponse apiResponse = QueryAccess.get(RetrieveFdo.GET_FDO_QUERY_ID, params);
        List<ApiResponseEntry> data = apiResponse.getData();
        String npref = data.get(0).get("np");

        Nanopub np = GetNanopub.get(npref);
        System.out.println("npref: " + npref);
        assertEquals(npref, np.getUri().stringValue());
    }

}
