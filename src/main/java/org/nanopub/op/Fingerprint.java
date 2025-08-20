package org.nanopub.op;

import com.beust.jcommander.ParameterException;
import net.trustyuri.TrustyUriException;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.Rio;
import org.nanopub.CliRunner;
import org.nanopub.MalformedNanopubException;
import org.nanopub.MultiNanopubRdfHandler;
import org.nanopub.Nanopub;
import org.nanopub.op.fingerprint.DefaultFingerprints;
import org.nanopub.op.fingerprint.FingerprintHandler;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPOutputStream;

/**
 * Command-line utility to calculate fingerprints for nanopublications.
 */
public class Fingerprint extends CliRunner {

    @com.beust.jcommander.Parameter(description = "input-nanopubs")
    private List<File> inputNanopubs = new ArrayList<>();

    @com.beust.jcommander.Parameter(names = "-o", description = "Output file")
    private File outputFile;

    @com.beust.jcommander.Parameter(names = "--in-format", description = "Format of the input nanopubs: trig, nq, trix, trig.gz, ...")
    private String inFormat;

    @com.beust.jcommander.Parameter(names = "--ignore-head", description = "Ignore the head graph for fingerprint calculation")
    private boolean ignoreHead;

    @com.beust.jcommander.Parameter(names = "--ignore-prov", description = "Ignore the provenance graph for fingerprint calculation")
    private boolean ignoreProv;

    @com.beust.jcommander.Parameter(names = "--ignore-pubinfo", description = "Ignore the publication info graph for fingerprint calculation")
    private boolean ignorePubinfo;

    @com.beust.jcommander.Parameter(names = "-h", description = "Fingerprint handler class")
    private String handlerClass;

    /**
     * Main method to run the Fingerprint utility.
     *
     * @param args Command-line arguments
     */
    public static void main(String[] args) {
        try {
            Fingerprint obj = CliRunner.initJc(new Fingerprint(), args);
            obj.init();
            obj.run();
        } catch (ParameterException ex) {
            System.exit(1);
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }

    private Fingerprint() {
    }

    /**
     * Creates an instance of the Fingerprint utility with the given command-line arguments.
     *
     * @param args Command-line arguments
     * @return An instance of Fingerprint
     * @throws com.beust.jcommander.ParameterException if there is an error in the parameters
     */
    public static Fingerprint getInstance(String args) throws ParameterException {
        if (args == null) {
            args = "";
        }
        Fingerprint obj = CliRunner.initJc(new Fingerprint(), args.trim().split(" "));
        obj.init();
        return obj;
    }

    private RDFFormat rdfInFormat;
    private OutputStream outputStream = System.out;
    private BufferedWriter writer;
    private FingerprintHandler fingerprintHandler;

    private void init() {
        if (handlerClass != null && !handlerClass.isEmpty()) {
            String detectorClassName = handlerClass;
            if (!handlerClass.contains(".")) {
                detectorClassName = "org.nanopub.op.fingerprint." + handlerClass;
            }
            try {
                fingerprintHandler = (FingerprintHandler) Class.forName(detectorClassName).getConstructor().newInstance();
            } catch (ReflectiveOperationException ex) {
                throw new RuntimeException(ex);
            }
        } else {
            fingerprintHandler = new DefaultFingerprints(ignoreHead, ignoreProv, ignorePubinfo);
        }
    }

    /**
     * Runs the Fingerprint utility to calculate fingerprints for the given nanopublications.
     *
     * @throws java.io.IOException                       if there is an error reading or writing files
     * @throws org.eclipse.rdf4j.rio.RDFParseException   if there is an error parsing RDF data
     * @throws org.eclipse.rdf4j.rio.RDFHandlerException if there is an error handling RDF data
     * @throws org.nanopub.MalformedNanopubException     if a nanopublication is malformed
     * @throws net.trustyuri.TrustyUriException          if there is an error with Trusty URIs
     */
    public void run() throws IOException, RDFParseException, RDFHandlerException,
            MalformedNanopubException, TrustyUriException {
        if (inputNanopubs == null || inputNanopubs.isEmpty()) {
            throw new ParameterException("No input files given");
        }
        for (File inputFile : inputNanopubs) {
            if (inFormat != null) {
                rdfInFormat = Rio.getParserFormatForFileName("file." + inFormat).orElse(null);
            } else {
                rdfInFormat = Rio.getParserFormatForFileName(inputFile.toString()).orElse(null);
            }
            if (outputFile != null) {
                if (outputFile.getName().endsWith(".gz")) {
                    outputStream = new GZIPOutputStream(new FileOutputStream(outputFile));
                } else {
                    outputStream = new FileOutputStream(outputFile);
                }
            }

            writer = new BufferedWriter(new OutputStreamWriter(outputStream));

            MultiNanopubRdfHandler.process(rdfInFormat, inputFile, np -> {
                try {
                    writer.write(np.getUri() + " " + getFingerprint(np) + "\n");
                } catch (RDFHandlerException | IOException ex) {
                    throw new RuntimeException(ex);
                }
            });

            writer.flush();
            if (outputStream != System.out) {
                writer.close();
            }
        }
    }

    /**
     * Calculates the fingerprint for a given nanopublication.
     *
     * @param np the nanopublication for which to calculate the fingerprint
     * @return the fingerprint as a string
     * @throws org.eclipse.rdf4j.rio.RDFHandlerException if there is an error handling RDF data
     * @throws java.io.IOException                       if there is an error reading or writing files
     */
    public String getFingerprint(Nanopub np) throws RDFHandlerException, IOException {
        return fingerprintHandler.getFingerprint(np);
    }

}
