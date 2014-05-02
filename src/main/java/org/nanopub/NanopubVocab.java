package org.nanopub;

import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;

/**
 * Recommended third-party vocabulary to be used in provenance and publication info graphs.
 *
 * @author Tobias Kuhn
 */
public class NanopubVocab {

	private NanopubVocab() {}  // no instances allowed

	public static final URI DATETIME_TYPE = new URIImpl("http://www.w3.org/2001/XMLSchema#dateTime");
	public static final URI CREATION_TIME = new URIImpl("http://purl.org/dc/terms/created");
	public static final URI HAS_AUTHOR = new URIImpl("http://swan.mindinformatics.org/ontologies/1.2/pav/authoredBy");
	public static final URI HAS_CREATOR = new URIImpl("http://swan.mindinformatics.org/ontologies/1.2/pav/createdBy");

}
