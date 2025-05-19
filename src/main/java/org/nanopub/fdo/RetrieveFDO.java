package org.nanopub.fdo;

import java.io.InputStream;

import org.eclipse.rdf4j.model.IRI;

// TODO class that provides the Op.Retrieve operations.
//      See https://fdo-connect.gitlab.io/ap1/architecture-documentation/main/operation-specification/
public class RetrieveFDO {

	private RetrieveFDO() {}  // no instances allowed

	public static FdoMetadata retrieveMetadataFromId(String iriOrHandle) {
		// TODO
		return null;
	}

	public static FdoMetadata retrieveMetadataFromHandle(String handle) {
		// TODO
		return null;
	}

	public static FdoMetadata retrieveMetadataFromIri(IRI iri) {
		// TODO
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
