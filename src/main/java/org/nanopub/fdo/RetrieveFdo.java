package org.nanopub.fdo;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.nanopub.MalformedNanopubException;
import org.nanopub.Nanopub;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

// TODO class that provides the Op.Retrieve operations.
//      See https://fdo-connect.gitlab.io/ap1/architecture-documentation/main/operation-specification/
public class RetrieveFdo {

	private static final ValueFactory vf = SimpleValueFactory.getInstance();

	private RetrieveFdo() {}  // no instances allowed

	/**
	 * Retrieve the NP from the handle system, if iriOrHandle looks like a handle.
	 */
	 public static FdoRecord retrieveRecordFromId(String iriOrHandle) throws MalformedNanopubException, URISyntaxException, IOException, InterruptedException {
		if (FdoUtils.looksLikeHandle(iriOrHandle)) {
			return retrieveRecordFromHandle(iriOrHandle);
		} else if (FdoUtils.isHandleIri(vf.createIRI(iriOrHandle))) {
			return retrieveRecordFromHandle(FdoUtils.extractHandle(vf.createIRI(iriOrHandle)));
		}
		throw new RuntimeException("Retrieving from nanopub network is not yet implemented.");
	}

	/**
	 * Retrieve the data from the handle system.
	 */
	public static FdoRecord retrieveRecordFromHandle(String handle) throws MalformedNanopubException, URISyntaxException, IOException, InterruptedException {
		Nanopub np = FdoNanopubCreator.createFromHandleSystem(handle);
		return new FdoRecord(np);
	}

	public static FdoRecord retrieveRecordFromIri(IRI iri) throws MalformedNanopubException, URISyntaxException, IOException, InterruptedException {
		if (FdoUtils.isHandleIri(iri)) {
			return retrieveRecordFromHandle(FdoUtils.extractHandle(iri));
		}
		throw new RuntimeException("Retrieving from nanopub network is not yet implemented.");
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
