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

	public static final URI XSD_DATETIME = new URIImpl("http://www.w3.org/2001/XMLSchema#dateTime");

	public static final URI DCT_CREATED = new URIImpl("http://purl.org/dc/terms/created");
	public static final URI PROV_GENERATEDATTIME = new URIImpl("http://www.w3.org/ns/prov#generatedAtTime");

	public static boolean isCreationTimeProperty(URI uri) {
		return uri.equals(DCT_CREATED) || uri.equals(PROV_GENERATEDATTIME);
	}

	public static final URI PAV_CREATEDBY = new URIImpl("http://purl.org/pav/createdBy");
	public static final URI PAV_CREATEDBY_1 = new URIImpl("http://swan.mindinformatics.org/ontologies/1.2/pav/createdBy");
	public static final URI DCT_CREATOR = new URIImpl("http://purl.org/dc/terms/creator");
	public static final URI DCE_CREATOR = new URIImpl("http://purl.org/dc/elements/1.1/creator");
	public static final URI PROV_WASATTRIBUTEDTO = new URIImpl("http://www.w3.org/ns/prov#wasAttributedTo");

	public static final URI PAV_AUTHOREDBY = new URIImpl("http://purl.org/pav/authoredBy");
	public static final URI PAV_AUTHOREDBY_1 = new URIImpl("http://swan.mindinformatics.org/ontologies/1.2/pav/authoredBy");

	public static boolean isCreatorProperty(URI uri) {
		return uri.equals(PAV_CREATEDBY) || uri.equals(PAV_CREATEDBY_1) || uri.equals(DCT_CREATOR) || uri.equals(DCE_CREATOR)
				|| uri.equals(PROV_WASATTRIBUTEDTO);
	}

	public static boolean isAuthorProperty(URI uri) {
		return uri.equals(PAV_AUTHOREDBY) || uri.equals(PAV_AUTHOREDBY_1);
	}

}
