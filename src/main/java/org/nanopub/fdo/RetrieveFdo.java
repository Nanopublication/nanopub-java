package org.nanopub.fdo;

import org.eclipse.rdf4j.model.IRI;
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

// TODO class that provides the Op.Retrieve operations.
//      See https://fdo-connect.gitlab.io/ap1/architecture-documentation/main/operation-specification/
public class RetrieveFdo {

	public final static String GET_FDO_QUERY_ID = "RAbmaOhAIkABVWo10zx6552K2g1KxFnnFReDLDAGapZ8g/get-fdo-by-id";
	private static final ValueFactory vf = SimpleValueFactory.getInstance();

	private RetrieveFdo() {}  // no instances allowed

	/**
	 * Retrieve the NP/FdoRecord from the nanopub network or handle system, always check nanopub network first.
	 */
	 public static FdoRecord retrieveRecordFromId(String iriOrHandle) throws MalformedNanopubException, URISyntaxException, IOException, InterruptedException, FailedApiCallException {
		 Nanopub np = retrieveFromNanopubNetwork(iriOrHandle);
		 if (np != null) {
			 return new FdoRecord(np);
		 }
		 if (FdoUtils.looksLikeHandle(iriOrHandle)) {
			 return retrieveRecordFromHandleSystem(iriOrHandle);
		 } else if (FdoUtils.isHandleIri(vf.createIRI(iriOrHandle))) {
			 return retrieveRecordFromHandleSystem(FdoUtils.extractHandle(vf.createIRI(iriOrHandle)));
		 }
		 throw new RuntimeException("Record not found: " + iriOrHandle);
	}

	/**
	 * Retrieve the newest corresponding Nanopub from the Nanopub network.
	 * @return null if not available
	 */
	public static Nanopub retrieveFromNanopubNetwork(String iriOrHandle) throws FailedApiCallException {
		Map<String, String> params = new HashMap<>();
		params.put("fdoid", iriOrHandle);
		ApiResponse apiResponse = QueryAccess.get(GET_FDO_QUERY_ID, params);
		List<ApiResponseEntry> data = apiResponse.getData();
		if (data.isEmpty()) {
			return null;
		}
		String npref = data.get(0).get("np");
		Nanopub np = GetNanopub.get(npref);
		return np;
	}

	/**
	 * Retrieve the data from the handle system.
	 */
	public static FdoRecord retrieveRecordFromHandleSystem(String handle) throws MalformedNanopubException, URISyntaxException, IOException, InterruptedException {
		Nanopub np = FdoNanopubCreator.createFromHandleSystem(handle);
		return new FdoRecord(np);
	}

	public static InputStream retrieveContentFromHandle(String handle) throws MalformedNanopubException, URISyntaxException, IOException, InterruptedException {
		FdoNanopub fdo = new FdoNanopub(FdoNanopubCreator.createFromHandleSystem(handle));

		Value contentUrl = fdo.getFdoRecord().getDataRef();
		if (contentUrl == null) {
			throw new RuntimeException("FDO has no file / DataRef.");
		}
		HttpRequest req = HttpRequest.newBuilder().GET().uri(new URI(contentUrl.stringValue())).build();
		HttpResponse<InputStream> httpResponse = HttpClient.newHttpClient().send(req, responseInfo -> HttpResponse.BodySubscribers.ofInputStream());
		return httpResponse.body();
	}

	public static InputStream retrieveContentFromIri(IRI iri) throws MalformedNanopubException, URISyntaxException, IOException, InterruptedException {
		if (FdoUtils.isHandleIri(iri)) {
			return retrieveContentFromHandle(FdoUtils.extractHandle(iri));
		}
		throw new RuntimeException("Retrieving from nanopub network is not yet implemented.");
	}

}
