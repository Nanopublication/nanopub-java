package org.nanopub;

import java.util.Calendar;
import java.util.Set;

import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;

/**
 * @author Tobias Kuhn
 */
public interface Nanopub {

	// URIs in the nanopub namespace:
	public static final URI NANOPUB_TYPE_URI = new URIImpl("http://www.nanopub.org/nschema#Nanopublication");
	public static final URI HAS_ASSERTION_URI = new URIImpl("http://www.nanopub.org/nschema#hasAssertion");
	public static final URI HAS_PROVENANCE_URI = new URIImpl("http://www.nanopub.org/nschema#hasProvenance");
	public static final URI HAS_PUBINFO_URI = new URIImpl("http://www.nanopub.org/nschema#hasPublicationInfo");

	// URIs for nanopub collections (preliminary and not stable):
	public static final URI NANOPUBCOLL_TYPE_URI = new URIImpl("http://www.nanopub.org/nschema#NanopublicationCollection");
	public static final URI HAS_ASSERTIONSET_URI = new URIImpl("http://www.nanopub.org/nschema#hasAssertionSet");
	public static final URI HAS_COLLPROVENANCE_URI = new URIImpl("http://www.nanopub.org/nschema#hasCollectionProvenance");
	public static final URI HAS_COLLPUBINFO_URI = new URIImpl("http://www.nanopub.org/nschema#hasCollectionPubInfo");
	public static final URI HAS_MEMBER = new URIImpl("http://www.nanopub.org/nschema#hasMember");

	public URI getUri();

	public URI getHeadUri();

	public Set<Statement> getHead();

	public URI getAssertionUri();

	public Set<Statement> getAssertion();

	public URI getProvenanceUri();

	public Set<Statement> getProvenance();

	public URI getPubinfoUri();

	public Set<Statement> getPubinfo();

	public Set<URI> getGraphUris();

	// TODO: Now that we have SimpleCreatorPattern and SimpleTimestampPattern,
	// we might not need the following three methods anymore...

	public Calendar getCreationTime();

	public Set<URI> getAuthors();

	public Set<URI> getCreators();

	public int getTripleCount();

	public long getByteCount();

}
