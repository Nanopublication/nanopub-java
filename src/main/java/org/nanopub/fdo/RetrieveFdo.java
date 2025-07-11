package org.nanopub.fdo;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.nanopub.MalformedNanopubException;
import org.nanopub.Nanopub;
import org.nanopub.extra.server.GetNanopub;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.ApiResponseEntry;
import org.nanopub.extra.services.FailedApiCallException;
import org.nanopub.extra.services.QueryAccess;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Retrieve FDOs (FDO Records) from the nanopub network or handle system.
 */
// TODO class that provides the Op.Retrieve operations.
//      See https://fdo-connect.gitlab.io/ap1/architecture-documentation/main/operation-specification/
public class RetrieveFdo {

    /**
     * The query ID for retrieving FDOs by their ID from the nanopub network.
     */
    public final static String GET_FDO_QUERY_ID = "RAs0HI_KRAds4w_OOEMl-_ed0nZHFWdfePPXsDHf4kQkU/get-fdo-by-id";
    private static final ValueFactory vf = SimpleValueFactory.getInstance();

    private RetrieveFdo() {
    }  // no instances allowed

    /**
     * Retrieve the NP/FdoRecord from the nanopub network or handle system, always check nanopub network first.
     *
     * @param iriOrHandle the IRI or handle of the FDO to resolve
     * @return the FdoRecord corresponding to the IRI or handle
     */
    public static FdoRecord resolveId(String iriOrHandle) throws FdoNotFoundException {
        try {
            Nanopub np = resolveInNanopubNetwork(iriOrHandle);
            if (np != null) {
                return new FdoRecord(np);
            }
            if (FdoUtils.looksLikeHandle(iriOrHandle)) {
                return resolveInHandleSystem(iriOrHandle);
            } else if (FdoUtils.isHandleIri(vf.createIRI(iriOrHandle))) {
                return resolveInHandleSystem(FdoUtils.extractHandle(vf.createIRI(iriOrHandle)));
            }
        } catch (Exception e) {
            throw new FdoNotFoundException(e);
        }
        throw new FdoNotFoundException("Could not find fdo: " + iriOrHandle);
    }

    /**
     * Retrieve the newest corresponding Nanopub from the Nanopub network.
     *
     * @param iriOrHandle the IRI or handle of the FDO to resolve
     * @return the Nanopub corresponding to the IRI or handle, or null if not found
     */
    public static Nanopub resolveInNanopubNetwork(String iriOrHandle) throws FailedApiCallException {
        Map<String, String> params = new HashMap<>();
        params.put("fdoid", iriOrHandle);
        ApiResponse apiResponse = QueryAccess.get(GET_FDO_QUERY_ID, params);
        List<ApiResponseEntry> data = apiResponse.getData();
        if (data.isEmpty()) {
            return null;
        }
        String npRef = data.getFirst().get("np");
        Nanopub np = GetNanopub.get(npRef);
        return np;
    }

    /**
     * Retrieve the data from the handle system.
     *
     * @param handle the handle of the FDO to resolve
     * @return the FdoRecord corresponding to the handle
     */
    public static FdoRecord resolveInHandleSystem(String handle) throws MalformedNanopubException, URISyntaxException, IOException, InterruptedException {
        Nanopub np = FdoNanopubCreator.createFromHandleSystem(handle);
        return new FdoRecord(np);
    }

    /**
     * Retrieve the NP/FdoRecord Content (DataRef) from the nanopub network or handle system, always check nanopub network first.
     *
     * @param iriOrHandle the IRI or handle of the FDO to resolve
     * @return an InputStream containing the content of the FDO
     */
    public static InputStream retrieveContentFromId(String iriOrHandle) throws URISyntaxException, IOException, InterruptedException, FdoNotFoundException {
        FdoRecord fdo = resolveId(iriOrHandle);

        Value contentUrl = fdo.getDataRef();
        if (contentUrl == null) {
            throw new FdoNotFoundException("FDO has no file / DataRef.");
        }
        HttpRequest req = HttpRequest.newBuilder().GET().uri(new URI(contentUrl.stringValue())).build();
        HttpResponse<InputStream> httpResponse = HttpClient.newHttpClient().send(req, responseInfo -> HttpResponse.BodySubscribers.ofInputStream());
        return httpResponse.body();
    }

}
