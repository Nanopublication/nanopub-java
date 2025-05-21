package org.nanopub;

import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.junit.jupiter.api.Test;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.ApiResponseEntry;
import org.nanopub.extra.services.QueryAccess;
import org.nanopub.fdo.FdoMetadata;
import org.nanopub.fdo.FdoNanopubCreator;
import org.nanopub.fdo.RetrieveFdo;
import org.nanopub.fdo.ValidateFdo;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.List;

import static java.lang.System.out;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Integration Tests must have suffix "IT".
 */
public class TheseTestsRequireOtherSystemsIT {

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
    void createNanopubFromHandleSystem() throws URISyntaxException, IOException, InterruptedException, MalformedNanopubException {
        String id = "21.T11967/39b0ec87d17a4856c5f7";
        Nanopub np = FdoNanopubCreator.createFromHandleSystem(id);

        RDFWriter w = Rio.createWriter(RDFFormat.TRIG, new OutputStreamWriter(out, Charset.forName("UTF-8")));
        NanopubUtils.propagateToHandler(np, w);
    }

    @Test
    void retrieveMetadataFromHandleSystem() throws URISyntaxException, IOException, InterruptedException, MalformedNanopubException {
        String id = "21.T11967/39b0ec87d17a4856c5f7";
        FdoMetadata metadata = RetrieveFdo.retrieveMetadataFromId(id);
        assertEquals(id, metadata.getId());

        Nanopub np = FdoNanopubCreator.createFromHandleSystem(id);
        assertEquals(metadata.getStatements().size(), np.getAssertion().size());
        // it would be great to compare all statements here
    }

//     @Test // TODO
    void validateFdo () throws URISyntaxException, IOException, InterruptedException, MalformedNanopubException {
        String id = "21.T11967/39b0ec87d17a4856c5f7";
        FdoMetadata metadata = RetrieveFdo.retrieveMetadataFromId(id);
        ValidateFdo.isValid(metadata);
    }

}