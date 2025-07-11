package org.nanopub;

import net.trustyuri.TrustyUriException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.nanopub.extra.security.SignNanopub;
import org.nanopub.extra.security.TransformContext;
import org.nanopub.extra.server.PublishNanopub;

import java.io.IOException;
import java.io.OutputStream;
import java.security.InvalidKeyException;
import java.security.SignatureException;
import java.util.Calendar;
import java.util.Set;

/**
 * This interface represents a nanopublication.
 *
 * @author Tobias Kuhn
 */
public interface Nanopub {

    // URIs in the nanopub namespace:
    /**
     * IRI of the Nanopublication type.
     */
    public static final IRI NANOPUB_TYPE_URI = SimpleValueFactory.getInstance().createIRI("http://www.nanopub.org/nschema#Nanopublication");

    /**
     * IRI of the has assertion property.
     */
    public static final IRI HAS_ASSERTION_URI = SimpleValueFactory.getInstance().createIRI("http://www.nanopub.org/nschema#hasAssertion");

    /**
     * IRI of the has provenance property.
     */
    public static final IRI HAS_PROVENANCE_URI = SimpleValueFactory.getInstance().createIRI("http://www.nanopub.org/nschema#hasProvenance");

    /**
     * IRI of the has pubinfo property.
     */
    public static final IRI HAS_PUBINFO_URI = SimpleValueFactory.getInstance().createIRI("http://www.nanopub.org/nschema#hasPublicationInfo");

    /**
     * IRI of the supersedes property.
     */
    // URIs that link nanopublications:
    public static final IRI SUPERSEDES = SimpleValueFactory.getInstance().createIRI("http://purl.org/nanopub/x/supersedes");

    /**
     * Returns the URI of this nanopublication. This is the URI that identifies the nanopub.
     *
     * @return the URI of this nanopublication
     */
    public IRI getUri();

    /**
     * Returns the URI of the head of this nanopublication.
     *
     * @return the URI of the head of this nanopublication
     */
    public IRI getHeadUri();

    /**
     * Returns the head of this nanopublication, which is a set of RDF statements.
     *
     * @return the head of this nanopublication
     */
    public Set<Statement> getHead();

    /**
     * Returns the URI of the assertion of this nanopublication.
     *
     * @return the URI of the assertion of this nanopublication
     */
    public IRI getAssertionUri();

    /**
     * Returns the assertion of this nanopublication, which is a set of RDF statements.
     *
     * @return the assertion of this nanopublication
     */
    public Set<Statement> getAssertion();

    /**
     * Returns the URI of the provenance of this nanopublication.
     *
     * @return the URI of the provenance of this nanopublication
     */
    public IRI getProvenanceUri();

    /**
     * Returns the provenance of this nanopublication, which is a set of RDF statements.
     *
     * @return the provenance of this nanopublication
     */
    public Set<Statement> getProvenance();

    /**
     * Returns the URI of the publication information of this nanopublication.
     *
     * @return the URI of the publication information of this nanopublication
     */
    public IRI getPubinfoUri();

    /**
     * Returns the publication information of this nanopublication, which is a set of RDF statements.
     *
     * @return the publication information of this nanopublication
     */
    public Set<Statement> getPubinfo();

    /**
     * Returns the URIs of the graphs that are used in this nanopublication.
     *
     * @return the URIs of the graphs that are used in this nanopublication
     */
    public Set<IRI> getGraphUris();

    // TODO: Now that we have SimpleCreatorPattern and SimpleTimestampPattern,
    // we might not need the following three methods anymore...

    /**
     * Returns the creation time of this nanopublication.
     *
     * @return the creation time of this nanopublication
     */
    public Calendar getCreationTime();

    /**
     * Returns the URI of the authors of this nanopublication.
     *
     * @return the URI of the authors of this nanopublication
     */
    public Set<IRI> getAuthors();

    /**
     * Returns the URIs of the creators of this nanopublication.
     *
     * @return the URIs of the creators of this nanopublication
     */
    public Set<IRI> getCreators();

    /**
     * Returns the number of triples in this nanopublication.
     *
     * @return the number of triples in this nanopublication
     */
    public int getTripleCount();

    /**
     * Returns the number of bytes that this nanopublication takes when serialized in RDF.
     *
     * @return the number of bytes that this nanopublication takes when serialized in RDF
     */
    public long getByteCount();

    /**
     * Serializes this nanopublication to an RDF stream in the specified format.
     *
     * @param out    the output stream to which the nanopub should be written
     * @param format the RDF format to use for serialization
     * @throws RDFHandlerException if an error occurs during serialization
     */
    public default void writeToStream(OutputStream out, RDFFormat format) throws RDFHandlerException {
        NanopubUtils.writeToStream(this, out, format);
    }

    /**
     * Serializes this nanopublication to a string in the specified format.
     *
     * @param format the RDF format to use for serialization
     * @return a string representation of the nanopublication in the specified format
     * @throws RDFHandlerException if an error occurs during serialization
     * @throws IOException         if an I/O error occurs during serialization
     */
    public default String writeToString(RDFFormat format) throws RDFHandlerException, IOException {
        return NanopubUtils.writeToString(this, format);
    }

    /**
     * Publishes this nanopublication to the default server.
     *
     * @return the response from the server after publishing
     * @throws IOException if an I/O error occurs during publishing
     */
    public default String publish() throws IOException {
        return PublishNanopub.publish(this);
    }

    /**
     * Publishes this nanopublication to the specified server URL.
     *
     * @param serverUrl the URL of the server to which the nanopub should be published
     * @return the response from the server after publishing
     * @throws IOException if an I/O error occurs during publishing
     */
    public default String publish(String serverUrl) throws IOException {
        return PublishNanopub.publish(this, serverUrl);
    }

    /**
     * Signs this nanopublication.
     *
     * @param context the context for signing, which may include keys and other parameters
     * @return the signed nanopublication
     * @throws TrustyUriException  if there is an issue with the Trusty URI
     * @throws SignatureException  if there is an issue with the signature process
     * @throws InvalidKeyException if the key used for signing is invalid
     */
    public default Nanopub sign(TransformContext context) throws TrustyUriException, SignatureException, InvalidKeyException {
        return SignNanopub.signAndTransform(this, context);
    }
}
