package org.nanopub;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.nanopub.extra.server.GetNanopub;
import org.nanopub.extra.services.APINotReachableException;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.ApiResponseEntry;
import org.nanopub.extra.services.FailedApiCallException;
import org.nanopub.extra.services.NotEnoughAPIInstancesException;
import org.nanopub.extra.services.QueryAccess;
import org.nanopub.extra.services.QueryRef;
import org.nanopub.fdo.RetrieveFdo;

public class FdoQueryIT {

    @Test
    public void testSimpleQuery() throws Exception {
        // url = "https://w3id.org/np/RAPf3B_OK7X4oN7c-PLztmiuL0dIV94joR6WydTjA6Asc#get-most-frequent-a-pred"
        String queryId = "RAPf3B_OK7X4oN7c-PLztmiuL0dIV94joR6WydTjA6Asc/get-most-frequent-a-pred";
        ApiResponse apiResponse = QueryAccess.get(new QueryRef(queryId, null));
        List<ApiResponseEntry> data = apiResponse.getData();
        for (ApiResponseEntry entry : data) {
            String result = entry.get("pred");
            assertNotNull(result);
        }
    }

    @Test
    public void testQueryNanopubNetworkWithFdoHandle() throws Exception {
        ApiResponse apiResponse = QueryAccess.get(new QueryRef(RetrieveFdo.GET_FDO_QUERY_ID, "fdoid", "21.T11967/39b0ec87d17a4856c5f7"));
        List<ApiResponseEntry> data = apiResponse.getData();
        String npref = data.getFirst().get("np");

        Nanopub np = GetNanopub.get(npref);
        System.out.println("npref: " + npref);
        assertEquals(npref, np.getUri().stringValue());
    }

    @Test
    public void testQueryNanopubNetworkNpUriForFdo() throws FailedApiCallException, APINotReachableException, NotEnoughAPIInstancesException {
        ApiResponse apiResponse = QueryAccess.get(new QueryRef(RetrieveFdo.GET_FDO_QUERY_ID, "fdoid", "https://w3id.org/np/RAsSeIyT03LnZt3QvtwUqIHSCJHWW1YeLkyu66Lg4FeBk/nanodash-readme"));
        List<ApiResponseEntry> data = apiResponse.getData();
        String npref = data.getFirst().get("np");

        Nanopub np = GetNanopub.get(npref);
        System.out.println("npref: " + npref);
        assertEquals(npref, np.getUri().stringValue());
    }

}
