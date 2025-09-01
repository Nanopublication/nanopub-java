package org.nanopub;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.rio.*;
import org.eclipse.rdf4j.rio.helpers.AbstractRDFHandler;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.GZIPInputStream;

/**
 * Handles files or streams with a sequence of nanopubs.
 *
 * @author Tobias Kuhn
 */
public class MultiNanopubRdfHandler extends AbstractRDFHandler {

    /**
     * Processes a stream of nanopubs in the specified format.
     *
     * @param format    The RDF format of the nanopubs.
     * @param in        The input stream containing the nanopubs.
     * @param npHandler The handler to process each nanopub.
     * @throws java.io.IOException                       If an I/O error occurs.
     * @throws org.eclipse.rdf4j.rio.RDFParseException   If an error occurs while parsing the RDF data.
     * @throws org.eclipse.rdf4j.rio.RDFHandlerException If an error occurs while handling the RDF data.
     * @throws org.nanopub.MalformedNanopubException     If a nanopub is malformed.
     */
    public static void process(RDFFormat format, InputStream in, NanopubHandler npHandler)
            throws IOException, RDFParseException, RDFHandlerException, MalformedNanopubException {
        process(format, in, null, npHandler);
    }

    /**
     * Processes a file containing a sequence of nanopubs.
     *
     * @param format    The RDF format of the nanopubs.
     * @param file      The file containing the nanopubs.
     * @param npHandler The handler to process each nanopub.
     * @throws java.io.IOException                       If an I/O error occurs.
     * @throws org.eclipse.rdf4j.rio.RDFParseException   If an error occurs while parsing the RDF data.
     * @throws org.eclipse.rdf4j.rio.RDFHandlerException If an error occurs while handling the RDF data.
     * @throws org.nanopub.MalformedNanopubException     If a nanopub is malformed.
     */
    public static void process(RDFFormat format, File file, NanopubHandler npHandler)
            throws IOException, RDFParseException, RDFHandlerException, MalformedNanopubException {
        InputStream in;
        if (file.getName().matches(".*\\.(gz|gzip)")) {
            in = new GZIPInputStream(new BufferedInputStream(new FileInputStream(file)));
        } else {
            in = new BufferedInputStream(new FileInputStream(file));
        }
        process(format, in, file, npHandler);
    }

    /**
     * Processes a file containing a sequence of nanopubs, automatically detecting the format.
     *
     * @param file      The file containing the nanopubs.
     * @param npHandler The handler to process each nanopub.
     * @throws java.io.IOException                       If an I/O error occurs.
     * @throws org.eclipse.rdf4j.rio.RDFParseException   If an error occurs while parsing the RDF data.
     * @throws org.eclipse.rdf4j.rio.RDFHandlerException If an error occurs while handling the RDF data.
     * @throws org.nanopub.MalformedNanopubException     If a nanopub is malformed.
     */
    public static void process(File file, NanopubHandler npHandler)
            throws IOException, RDFParseException, RDFHandlerException, MalformedNanopubException {
        RDFFormat format = Rio.getParserFormatForFileName(file.getName()).orElse(RDFFormat.TRIG);
        process(format, file, npHandler);
    }

    private static void process(RDFFormat format, InputStream in, File file, NanopubHandler npHandler)
            throws IOException, RDFParseException, RDFHandlerException, MalformedNanopubException {
        RDFParser p = NanopubUtils.getParser(format);
        p.setRDFHandler(new MultiNanopubRdfHandler(npHandler));
        try (InputStreamReader is = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            p.parse(is, "");
        } catch (RuntimeException ex) {
            if ("wrapped MalformedNanopubException".equals(ex.getMessage()) &&
                ex.getCause() instanceof MalformedNanopubException) {
                throw (MalformedNanopubException) ex.getCause();
            } else {
                throw ex;
            }
        }
    }

    private NanopubHandler npHandler;

    private Map<IRI, Boolean> graphs = new HashMap<>();
    private Map<IRI, Map<IRI, Boolean>> members = new HashMap<>();
    private Set<Statement> statements = new LinkedHashSet<>();
    private List<String> nsPrefixes = new ArrayList<>();
    private Map<String, String> ns = new HashMap<>();

    private List<String> newNsPrefixes = new ArrayList<>();
    private List<String> newNs = new ArrayList<>();

    /**
     * Constructs a new MultiNanopubRdfHandler with the specified nanopub handler.
     *
     * @param npHandler The handler to process each nanopub.
     */
    public MultiNanopubRdfHandler(NanopubHandler npHandler) {
        this.npHandler = npHandler;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Handles the statement to be added to the current nanopub.
     */
    @Override
    public void handleStatement(Statement st) throws RDFHandlerException {
        if (!graphs.containsKey(st.getContext())) {
            if (graphs.size() == 4) {
                finishAndReset();
                handleStatement(st);
                return;
            }
            graphs.put((IRI) st.getContext(), true);
        }
        addNamespaces();
        statements.add(st);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Handles the namespace declaration in the RDF data.
     */
    @Override
    public void handleNamespace(String prefix, String uri) throws RDFHandlerException {
        newNs.add(uri);
        newNsPrefixes.add(prefix);
    }

    /**
     * Adds the namespaces.
     *
     * @throws org.eclipse.rdf4j.rio.RDFHandlerException If an error occurs while handling the RDF data.
     */
    public void addNamespaces() throws RDFHandlerException {
        for (int i = 0; i < newNs.size(); i++) {
            String prefix = newNsPrefixes.get(i);
            String nsUri = newNs.get(i);
            nsPrefixes.remove(prefix);
            nsPrefixes.add(prefix);
            ns.put(prefix, nsUri);
        }
        newNs.clear();
        newNsPrefixes.clear();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Handles the end of the RDF data.
     */
    @Override
    public void endRDF() throws RDFHandlerException {
        finishAndReset();
    }

    private void finishAndReset() {
        try {
            npHandler.handleNanopub(new NanopubImpl(statements, nsPrefixes, ns));
        } catch (MalformedNanopubException ex) {
            if (ex.getMessage().equals("No content received for nanopub")) { // TODO: Improve this check!
                // ignore (a stream of zero nanopubs is also a valid nanopub stream)
            } else {
                throwMalformed(ex);
            }
        } catch (NanopubAlreadyFinalizedException e) {
            throw new RuntimeException(e);
        }
        clearAll();
    }

    private void clearAll() {
        graphs.clear();
        members.clear();
        statements.clear();
    }

    private void throwMalformed(MalformedNanopubException ex) {
        throw new RuntimeException("wrapped MalformedNanopubException", ex);
    }


    /**
     * Interface for handling nanopubs.
     */
    public interface NanopubHandler {

        /**
         * Handles a nanopub.
         *
         * @param np The nanopub to handle.
         */
        public void handleNanopub(Nanopub np) throws NanopubAlreadyFinalizedException;

    }

}
