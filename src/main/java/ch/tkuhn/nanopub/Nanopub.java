package ch.tkuhn.nanopub;

import org.openrdf.model.Statement;
import org.openrdf.model.URI;

public interface Nanopub {

	public static final String TYPE_URI = "http://www.nanopub.org/nschema#Nanopublication";

	public URI getURI();

	public Iterable<Statement> getHeadStatements();

	public Iterable<Statement> getAssertionStatements();

	public Iterable<Statement> getProvenanceStatements();

	public Iterable<Statement> getPubinfoStatements();

}
