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

	public static final URI NANOPUB_TYPE_URI = new URIImpl("http://www.nanopub.org/nschema#Nanopublication");
	public static final URI HAS_ASSERTION_URI = new URIImpl("http://www.nanopub.org/nschema#hasAssertion");
	public static final URI HAS_PROVENANCE_URI = new URIImpl("http://www.nanopub.org/nschema#hasProvenance");
	public static final URI HAS_PUBINFO_URI = new URIImpl("http://www.nanopub.org/nschema#hasPublicationInfo");

	public URI getUri();

	public URI getHeadUri();

	public Set<URI> getHeadSuperGraphs();

	public Set<Statement> getHead();

	public URI getAssertionUri();

	public Set<URI> getAssertionSuperGraphs();

	public Set<Statement> getAssertion();

	public URI getProvenanceUri();

	public Set<URI> getProvenanceSuperGraphs();

	public Set<Statement> getProvenance();

	public URI getPubinfoUri();

	public Set<URI> getPubinfoSuperGraphs();

	public Set<Statement> getPubinfo();

	public Set<URI> getGraphUris();

	public Calendar getCreationTime();

	public Set<URI> getAuthors();

	public Set<URI> getCreators();

}
