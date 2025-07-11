package org.nanopub.trusty;

import java.util.Map;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.rio.RDFHandler;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.nanopub.Nanopub;

/**
 * You can use temporary URIs for your nanopublications that start with <a href="http://purl.org/nanopub/temp/">http://purl.org/nanopub/temp/</a>. These then become
 * <a href="https://w3id.org/np/ARTIFACTCODE-PLACEHOLDER/">https://w3id.org/np/ARTIFACTCODE-PLACEHOLDER/</a> before being transformed to
 * trusty nanopublications, and as final trusty nanopublications have the actual artifact code instead of the placeholder.
 *
 * @author Tobias Kuhn
 */
public class TempUriReplacer implements RDFHandler {

    /**
     * The temporary URI prefix for nanopublications.
     */
    public static final String tempUri = "http://purl.org/nanopub/temp/";

    /**
     * The normalized URI prefix for nanopublications, which is used in the
     */
    public static final String normUri = "https://w3id.org/np/ARTIFACTCODE-PLACEHOLDER/";

    private String uriPrefix;
    private RDFHandler nestedHandler;
    private Map<Resource, IRI> transformMap;

    /**
     * Creates a new temporary URI replacer.
     *
     * @param np            The nanopublication to be transformed.
     * @param nestedHandler The nested RDF handler that will receive the transformed statements.
     * @param transformMap  A map to store the transformation of temporary URIs to their final form.
     */
    public TempUriReplacer(Nanopub np, RDFHandler nestedHandler, Map<Resource, IRI> transformMap) {
        this.uriPrefix = np.getUri().stringValue();
        this.nestedHandler = nestedHandler;
        this.transformMap = transformMap;
    }

    /**
     * Checks whether the given nanopublication has a temporary URI.
     *
     * @param np The nanopublication to check.
     * @return True if the nanopublication has a temporary URI, false otherwise.
     */
    public static boolean hasTempUri(Nanopub np) {
        return np.getUri().stringValue().startsWith(tempUri);
    }

    /**
     * Handles a statement by replacing temporary URIs with the normalized URI prefix.
     *
     * @param st The statement to handle.
     */
    @Override
    public void handleStatement(Statement st) throws RDFHandlerException {
        nestedHandler.handleStatement(SimpleValueFactory.getInstance().createStatement((Resource) replace(st.getSubject()), (IRI) replace(st.getPredicate()), replace(st.getObject()), (Resource) replace(st.getContext())));
    }

    /**
     * Handles a namespace by replacing the temporary URI prefix with the normalized URI prefix.
     *
     * @param prefix The namespace prefix.
     * @param uri    The namespace URI.
     */
    @Override
    public void handleNamespace(String prefix, String uri) throws RDFHandlerException {
        if (uri.startsWith(uriPrefix)) {
            uri = uri.replace(uriPrefix, normUri);
        }
        nestedHandler.handleNamespace(prefix, uri);
    }

    private Value replace(Value v) {
        if (v instanceof IRI && v.stringValue().startsWith(uriPrefix)) {
            IRI i = SimpleValueFactory.getInstance().createIRI(v.stringValue().replace(uriPrefix, normUri));
            if (transformMap != null) transformMap.put((IRI) v, i);
            return i;
        } else {
            return v;
        }
    }

    /**
     * Starts the RDF processing by calling the startRDF method of the nested handler.
     *
     * @throws RDFHandlerException If an error occurs during the start of RDF processing.
     */
    @Override
    public void startRDF() throws RDFHandlerException {
        nestedHandler.startRDF();
    }

    /**
     * Ends the RDF processing by calling the endRDF method of the nested handler.
     *
     * @throws RDFHandlerException If an error occurs during the end of RDF processing.
     */
    @Override
    public void endRDF() throws RDFHandlerException {
        nestedHandler.endRDF();
    }

    /**
     * Handles a comment by passing it to the nested handler.
     *
     * @param comment The comment to handle.
     * @throws RDFHandlerException If an error occurs while handling the comment.
     */
    @Override
    public void handleComment(String comment) throws RDFHandlerException {
        nestedHandler.handleComment(comment);
    }

}
