package org.nanopub;

import java.util.Calendar;
import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

/**
 * @author Tobias Kuhn
 */
public interface Nanopub {

	// URIs in the nanopub namespace:
	public static final IRI NANOPUB_TYPE_URI = SimpleValueFactory.getInstance().createIRI("http://www.nanopub.org/nschema#Nanopublication");
	public static final IRI HAS_ASSERTION_URI = SimpleValueFactory.getInstance().createIRI("http://www.nanopub.org/nschema#hasAssertion");
	public static final IRI HAS_PROVENANCE_URI = SimpleValueFactory.getInstance().createIRI("http://www.nanopub.org/nschema#hasProvenance");
	public static final IRI HAS_PUBINFO_URI = SimpleValueFactory.getInstance().createIRI("http://www.nanopub.org/nschema#hasPublicationInfo");

	// URIs that link nanopublications:
	public static final IRI SUPERSEDES = SimpleValueFactory.getInstance().createIRI("http://purl.org/nanopub/x/supersedes");

	public IRI getUri();

	public IRI getHeadUri();

	public Set<Statement> getHead();

	public IRI getAssertionUri();

	public Set<Statement> getAssertion();

	public IRI getProvenanceUri();

	public Set<Statement> getProvenance();

	public IRI getPubinfoUri();

	public Set<Statement> getPubinfo();

	public Set<IRI> getGraphUris();

	// TODO: Now that we have SimpleCreatorPattern and SimpleTimestampPattern,
	// we might not need the following three methods anymore...

	public Calendar getCreationTime();

	public Set<IRI> getAuthors();

	public Set<IRI> getCreators();

	public int getTripleCount();

	public long getByteCount();

}
