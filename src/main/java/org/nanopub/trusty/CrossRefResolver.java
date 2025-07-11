package org.nanopub.trusty;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.rio.RDFHandler;
import org.eclipse.rdf4j.rio.RDFHandlerException;

import java.util.Map;

/**
 * A RDFHandler that resolves cross-references in RDF statements.
 */
public class CrossRefResolver implements RDFHandler {

    private Map<Resource, IRI> tempRefMap;
    private Map<String, String> tempPrefixMap;
    private RDFHandler nestedHandler;

    /**
     * Constructs a CrossRefResolver with the given temporary reference map, prefix map, and nested RDF handler.
     *
     * @param tempRefMap    a map of temporary references to IRIs
     * @param tempPrefixMap a map of prefixes to their replacements
     * @param nestedHandler the nested RDF handler to delegate calls to
     */
    public CrossRefResolver(Map<Resource, IRI> tempRefMap, Map<String, String> tempPrefixMap, RDFHandler nestedHandler) {
        this.tempRefMap = tempRefMap;
        this.tempPrefixMap = tempPrefixMap;
        this.nestedHandler = nestedHandler;
    }

    /**
     * Handle a statement by replacing temporary references with their corresponding IRIs.
     *
     * @param st the RDF statement to handle
     */
    @Override
    public void handleStatement(Statement st) throws RDFHandlerException {
        nestedHandler.handleStatement(SimpleValueFactory.getInstance().createStatement((Resource) replace(st.getSubject()), (IRI) replace(st.getPredicate()), replace(st.getObject()), (Resource) replace(st.getContext())));
    }

    /**
     * Handle a namespace by replacing the URI with its corresponding transformed URI.
     *
     * @param prefix the namespace prefix
     * @param uri    the namespace URI
     */
    @Override
    public void handleNamespace(String prefix, String uri) throws RDFHandlerException {
        String transformedUri = replace(SimpleValueFactory.getInstance().createIRI(uri)).stringValue();
        nestedHandler.handleNamespace(prefix, transformedUri);
    }

    private Value replace(Value v) {
        if (!(v instanceof Resource)) return v;
        IRI i = tempRefMap.get(v);
        if (i != null) return i;
        if (v instanceof IRI && tempPrefixMap != null) {
            for (String prefix : tempPrefixMap.keySet()) {
                if (v.stringValue().startsWith(prefix)) {
                    return vf.createIRI(v.stringValue().replace(prefix, tempPrefixMap.get(prefix)));
                }
            }
        }
        return v;
    }

    /**
     * Starts the RDF processing by calling the startRDF method of the nested handler.
     *
     * @throws RDFHandlerException if an error occurs during processing
     */
    @Override
    public void startRDF() throws RDFHandlerException {
        nestedHandler.startRDF();
    }

    /**
     * Ends the RDF processing by calling the endRDF method of the nested handler.
     *
     * @throws RDFHandlerException if an error occurs during processing
     */
    @Override
    public void endRDF() throws RDFHandlerException {
        nestedHandler.endRDF();
    }

    /**
     * Handles a comment by passing it to the nested handler.
     *
     * @param comment the comment to handle
     * @throws RDFHandlerException if an error occurs during processing
     */
    @Override
    public void handleComment(String comment) throws RDFHandlerException {
        nestedHandler.handleComment(comment);
    }

    private static ValueFactory vf = SimpleValueFactory.getInstance();

}
