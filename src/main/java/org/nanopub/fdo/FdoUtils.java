package org.nanopub.fdo;

import org.eclipse.rdf4j.common.exception.ValidationException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF4J;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.WriterConfig;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.sail.shacl.ShaclSail;
import org.nanopub.Nanopub;

public class FdoUtils {

    private static final ValueFactory vf = SimpleValueFactory.getInstance();

    public static final String FDO_URI_PREFIX = "https://hdl.handle.net/";
    public static final IRI RDF_TYPE_FDO = vf.createIRI("https://w3id.org/fdof/ontology#FAIRDigitalObject");
    public static final IRI RDF_FDO_PROFILE = vf.createIRI("https://hdl.handle.net/0.FDO/Profile");

    /**
     * Add the prefix "https://hdl.handle.net/" to the fdoHandle and returns it as IRI.
     */
    public static IRI toIri(String fdoHandle) {
        return vf.createIRI(FDO_URI_PREFIX + fdoHandle);
    }

    /**
     * Remove the prefix "https://hdl.handle.net/" from the IRI and returns the handle as string.
     * returns null if the prefix is not there.
     */
    public static String extractHandle(IRI iri) {
        String iriString = iri.toString();
        if (iriString.startsWith(FDO_URI_PREFIX)) {
            return iriString.substring(FDO_URI_PREFIX.length());
        }
        return null;
    }

    /**
     * We here assume that a handle starts with 2 digits (minimal prefix),
     * no whitespaces, and contains at least one "/".
     */
    public static boolean looksLikeHandle(String potentialHandle) {
        if (potentialHandle.matches("\\d\\d\\S*/+\\S*")){
            return true;
        }
        return false;
    }

    public static boolean looksLikeUrl(String potentialUrl) {
        if (potentialUrl.matches("http(s)?://\\S+\\.[a-z]{2,}.*")){
            return true;
        }
        return false;
    }

    /**
     * Create an IRI by prefixing a handle with https://hdl.handle.net/ if it's a handle,
     * or by just converting an url.
     */
    public static IRI createIri(String handleOrUrl) {
        if (looksLikeHandle(handleOrUrl)) {
            return vf.createIRI(FDO_URI_PREFIX + handleOrUrl);
        } else if (looksLikeUrl(handleOrUrl)) {
            return vf.createIRI(handleOrUrl);
        }
        throw new IllegalArgumentException("Neither handle nor url found: " + handleOrUrl);
    }

    /**
     * Logs constraints validation to System.out
     *
     * @return true, iff the data respects the specification of the shape.
     */
    public static boolean validateShacl(Nanopub shape, Nanopub data) {
        ShaclSail shaclSail = new ShaclSail(new MemoryStore());
        Repository repo = new SailRepository(shaclSail);
        RepositoryConnection connection = repo.getConnection();

        // add shape
        connection.begin();
        for (Statement st: shape.getAssertion()) {
            connection.add(st, RDF4J.SHACL_SHAPE_GRAPH);
        }
        connection.commit();

        connection.begin();
        // add data to be validated
        for (Statement st: data.getAssertion()) {
            connection.add(st);
        }
        try {
            connection.commit();
            return true;
        } catch (RepositoryException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof ValidationException) {
                Model validationReportModel = ((ValidationException) cause).validationReportAsModel();

                WriterConfig writerConfig = new WriterConfig()
                        .set(BasicWriterSettings.INLINE_BLANK_NODES, true)
                        .set(BasicWriterSettings.XSD_STRING_TO_PLAIN_LITERAL, true)
                        .set(BasicWriterSettings.PRETTY_PRINT, true);

                Rio.write(validationReportModel, System.out, RDFFormat.TURTLE, writerConfig);
                return false;
            }
            throw new RuntimeException(cause);
        }
    }

}
