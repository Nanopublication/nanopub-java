package org.nanopub.fdo;

import com.beust.jcommander.ParameterException;
import org.eclipse.rdf4j.common.exception.ValidationException;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
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
import org.nanopub.CliRunner;
import org.nanopub.MalformedNanopubException;
import org.nanopub.Nanopub;
import org.nanopub.NanopubImpl;

import java.io.File;
import java.io.IOException;

/**
 * Using Shacl to validate a Nanopub against a Shape.
 */
public class ShaclValidator extends CliRunner {

    @com.beust.jcommander.Parameter(names = "-n", description = "nanopub-to-be-validated", required = true)
    private File nanopubFile;

    @com.beust.jcommander.Parameter(names = "-s", description = "SHACL shape file", required = true)
    private File shapeFile;

    public static void main(String[] args) {
        try {
            ShaclValidator obj = CliRunner.initJc(new ShaclValidator(), args);
            obj.run();
        } catch (ParameterException ex) {
            System.exit(1);
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        }
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

    public void run () throws MalformedNanopubException, IOException {
        Nanopub shape = new NanopubImpl(shapeFile);
        Nanopub data = new NanopubImpl(nanopubFile);
        if (validateShacl(shape, data)) {
            System.out.println("Validation successful.");
        } else {
            System.out.println("Validation not successful.");
        }
    }

}
