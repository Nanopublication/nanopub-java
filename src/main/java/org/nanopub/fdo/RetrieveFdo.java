package org.nanopub.fdo;

import org.eclipse.rdf4j.model.IRI;
import org.nanopub.MalformedNanopubException;
import org.nanopub.Nanopub;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;

// TODO class that provides the Op.Retrieve operations.
//      See https://fdo-connect.gitlab.io/ap1/architecture-documentation/main/operation-specification/
public class RetrieveFdo {

	private RetrieveFdo() {}  // no instances allowed

	/**
	 * Retrieve the NP from the handle system, if iriOrHandle looks like a handle.
	 */
	 public static FdoMetadata retrieveMetadataFromId(String iriOrHandle) throws MalformedNanopubException, URISyntaxException, IOException, InterruptedException {
		if (FdoUtils.looksLikeHandle(iriOrHandle)) {
			return retrieveMetadataFromHandle(iriOrHandle);
		}
		throw new RuntimeException("Retrieving from nanopub network is not yet implemented.");
	}

	/**
	 * Retrieve the data from the handle system.
	 */
	public static FdoMetadata retrieveMetadataFromHandle(String handle) throws MalformedNanopubException, URISyntaxException, IOException, InterruptedException {
		Nanopub np = FdoNanopubCreator.createFromHandleSystem(handle);
		return new FdoMetadata(np);
	}

	public static FdoMetadata retrieveMetadataFromIri(IRI iri) {
		// TODO
		// Not yet clear how to find it in the np world
		return null;
	}

	public static InputStream retrieveContentFromId(String iriOrHandle) {
		// TODO To be implemented later.
		return null;
	}

	public static InputStream retrieveContentFromHandle(String handle) {
		// TODO To be implemented later.
		return null;
	}

	public static InputStream retrieveContentFromIri(IRI iri) {
		// TODO To be implemented later.
		return null;
	}

}
