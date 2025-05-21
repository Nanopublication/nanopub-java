package org.nanopub.fdo;

import org.nanopub.MalformedNanopubException;

import java.io.IOException;
import java.net.URISyntaxException;

// TODO class that provides the Op.Validate operations.
//      See https://fdo-connect.gitlab.io/ap1/architecture-documentation/main/operation-specification/
public class ValidateFdo {

	private ValidateFdo() {}  // no instances allowed

	// TODO Just a boolean as return value. Later probably an object that also includes errors/warnings.
	public static boolean isValid(FdoMetadata fdoMetadata) throws MalformedNanopubException, URISyntaxException, IOException, InterruptedException {

		String profileId = FdoUtils.extractHandle(fdoMetadata.getProfile());
		String schemaUrl = RetrieveFdo.retrieveMetadataFromHandle(profileId).getSchemaUrl();

		// TODO get construct shacl from schema
		throw new RuntimeException("Not yet implemented");

		// return ShaclValidator.validateShacl(shaclShape, fdoMetadata.getStatements());
	}


}
