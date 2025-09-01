package org.nanopub.op;

import com.beust.jcommander.ParameterException;
import net.trustyuri.TrustyUriException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.PROV;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.Rio;
import org.nanopub.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Command-line tool to create a new nanopublication with a placeholder assertion.
 */
public class Create extends CliRunner {

    @com.beust.jcommander.Parameter(names = "-o", description = "Output file")
    private File outputFile;

    @com.beust.jcommander.Parameter(names = "--format", description = "Format of the output nanopubs: trig, nq, trix, trig.gz, ...")
    private String format;

    /**
     * Main method to run the Create command-line tool.
     *
     * @param args Command-line arguments
     */
    public static void main(String[] args) {
        try {
            Create obj = CliRunner.initJc(new Create(), args);
            obj.run();
        } catch (ParameterException ex) {
            System.exit(1);
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }

    private RDFFormat rdfOutFormat;
    private OutputStream outputStream = System.out;

    private void run() throws IOException, RDFParseException, RDFHandlerException,
            MalformedNanopubException, TrustyUriException, NanopubAlreadyFinalizedException {

        if (outputFile == null) {
            if (format == null) {
                format = "trig";
            }
            rdfOutFormat = Rio.getParserFormatForFileName("file." + format).orElse(null);
        } else {
            rdfOutFormat = Rio.getParserFormatForFileName(outputFile.getName()).orElse(null);
            if (outputFile.getName().endsWith(".gz")) {
                outputStream = new GZIPOutputStream(new FileOutputStream(outputFile));
            } else {
                outputStream = new FileOutputStream(outputFile);
            }
        }

        IRI nanopubIri = NanopubUtils.createTempNanopubIri();
        IRI creatorIri = vf.createIRI(nanopubIri.stringValue() + "creator");
        NanopubCreator npCreator = new NanopubCreator(nanopubIri);
        npCreator.addAssertionStatement(npCreator.getAssertionUri(), RDFS.COMMENT, vf.createLiteral("Replace this with your assertion content."));
        npCreator.addProvenanceStatement(PROV.HAD_PRIMARY_SOURCE, creatorIri);
        npCreator.addTimestampNow();
        npCreator.addCreator(creatorIri);
        Nanopub np = npCreator.finalizeNanopub();
        NanopubUtils.writeToStream(np, outputStream, rdfOutFormat);

        outputStream.flush();
        if (outputStream != System.out) {
            outputStream.close();
        }
    }

    private static SimpleValueFactory vf = SimpleValueFactory.getInstance();

}
