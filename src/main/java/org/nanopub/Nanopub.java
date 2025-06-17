package org.nanopub;

import java.io.IOException;
import java.io.OutputStream;
import java.security.InvalidKeyException;
import java.security.SignatureException;
import java.util.Calendar;
import java.util.Set;

import net.trustyuri.TrustyUriException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.nanopub.extra.security.SignNanopub;
import org.nanopub.extra.security.TransformContext;
import org.nanopub.extra.server.PublishNanopub;

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

	public default void writeToStream(OutputStream out, RDFFormat format) throws RDFHandlerException {
		NanopubUtils.writeToStream(this, out, format);
	}

	public default String writeToString(RDFFormat format) throws RDFHandlerException, IOException {
		return NanopubUtils.writeToString(this, format);
	}

	public default String publish() throws IOException {
		return PublishNanopub.publish(this);
	}

	public default String publish(String serverUrl) throws IOException {
		return PublishNanopub.publish(this, serverUrl);
	}

	public default Nanopub sign(TransformContext context) throws TrustyUriException, SignatureException, InvalidKeyException {
		return SignNanopub.signAndTransform(this, context);
	}
}
