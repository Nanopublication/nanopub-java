package org.nanopub.op;

import com.beust.jcommander.ParameterException;
import net.trustyuri.TrustyUriException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.Rio;
import org.nanopub.CliRunner;
import org.nanopub.MalformedNanopubException;
import org.nanopub.MultiNanopubRdfHandler;
import org.nanopub.MultiNanopubRdfHandler.NanopubHandler;
import org.nanopub.Nanopub;

import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.GZIPOutputStream;

/**
 * Command-line utility to work with namespaces in nanopublications.
 */
public class Namespaces extends CliRunner {

    @com.beust.jcommander.Parameter(description = "input-nanopubs")
    private List<File> inputNanopubs = new ArrayList<>();

    @com.beust.jcommander.Parameter(names = "-h", description = "Output file for namespaces used in head graph")
    private File headOutputFile;

    @com.beust.jcommander.Parameter(names = "-a", description = "Output file for namespaces used in assertion graph")
    private File assertionOutputFile;

    @com.beust.jcommander.Parameter(names = "-p", description = "Output file for namespaces used in provenance graph")
    private File provOutputFile;

    @com.beust.jcommander.Parameter(names = "-i", description = "Output file for namespaces used in pub info graph")
    private File pubinfoOutputFile;

    @com.beust.jcommander.Parameter(names = "--subj", description = "Include subject URIs")
    private boolean includeSubject;

    @com.beust.jcommander.Parameter(names = "--pred", description = "Include predicate URIs")
    private boolean includePredicate;

    @com.beust.jcommander.Parameter(names = "--obj", description = "Include object URIs")
    private boolean includeObject;

    @com.beust.jcommander.Parameter(names = "--in-format", description = "Format of the input nanopubs: trig, nq, trix, trig.gz, ...")
    private String inFormat;

    /**
     * Main method to run the Namespaces utility.
     *
     * @param args Command-line arguments
     */
    public static void main(String[] args) {
        try {
            Namespaces obj = CliRunner.initJc(new Namespaces(), args);
            obj.init();
            obj.run();
        } catch (ParameterException ex) {
            System.exit(1);
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Initializes the Namespaces utility with command-line arguments.
     *
     * @param args Command-line arguments
     * @return An instance of Namespaces
     * @throws ParameterException if there is an error in the parameters
     */
    public static Namespaces getInstance(String args) throws ParameterException {
        if (args == null) {
            args = "";
        }
        Namespaces obj = CliRunner.initJc(new Namespaces(), args.trim().split(" "));
        return obj;
    }

    private RDFFormat rdfInFormat;
    private BufferedWriter headWriter, assertionWriter, provWriter, pubinfoWriter;

    private void init() {
        if (!includeSubject && !includePredicate && !includeObject) {
            includeSubject = true;
            includePredicate = true;
            includeObject = true;
        }
    }

    /**
     * Runs the Namespaces utility to extract namespaces from nanopublications.
     *
     * @throws IOException               if there is an error reading or writing files
     * @throws RDFParseException         if there is an error parsing RDF data
     * @throws RDFHandlerException       if there is an error handling RDF data
     * @throws MalformedNanopubException if a nanopub is malformed
     * @throws TrustyUriException        if there is an error with Trusty URIs
     */
    public void run() throws IOException, RDFParseException, RDFHandlerException,
            MalformedNanopubException, TrustyUriException {
        if (inputNanopubs == null || inputNanopubs.isEmpty()) {
            throw new ParameterException("No input files given");
        }
        headWriter = makeWriter(headOutputFile);
        assertionWriter = makeWriter(assertionOutputFile);
        provWriter = makeWriter(provOutputFile);
        pubinfoWriter = makeWriter(pubinfoOutputFile);
        for (File inputFile : inputNanopubs) {
            if (inFormat != null) {
                rdfInFormat = Rio.getParserFormatForFileName("file." + inFormat).orElse(null);
            } else {
                rdfInFormat = Rio.getParserFormatForFileName(inputFile.toString()).orElse(null);
            }

            MultiNanopubRdfHandler.process(rdfInFormat, inputFile, new NanopubHandler() {

                @Override
                public void handleNanopub(Nanopub np) {
                    try {
                        process(np);
                    } catch (RDFHandlerException | IOException ex) {
                        throw new RuntimeException(ex);
                    }
                }

            });

        }
        closeWriter(headWriter);
        closeWriter(assertionWriter);
        closeWriter(provWriter);
        closeWriter(pubinfoWriter);
    }

    /**
     * Processes a single nanopublication to extract namespaces from its graphs.
     *
     * @param np the nanopublication to process
     * @throws RDFHandlerException if there is an error handling RDF data
     * @throws IOException         if there is an error writing to output files
     */
    public void process(Nanopub np) throws RDFHandlerException, IOException {
        writeNamespaces(np.getHead(), headWriter);
        writeNamespaces(np.getAssertion(), assertionWriter);
        writeNamespaces(np.getProvenance(), provWriter);
        writeNamespaces(np.getPubinfo(), pubinfoWriter);
    }

    private void writeNamespaces(Set<Statement> statements, BufferedWriter w) throws IOException {
        if (w == null) return;
        Set<String> namespaces = new HashSet<>();
        for (Statement st : statements) {
            if (includeSubject && st.getSubject() instanceof IRI) {
                namespaces.add(getNamespace((IRI) st.getSubject()));
            }
            if (includePredicate) {
                namespaces.add(getNamespace(st.getPredicate()));
            }
            if (includeObject && st.getObject() instanceof IRI) {
                namespaces.add(getNamespace((IRI) st.getObject()));
            }
        }
        for (String n : namespaces) {
            w.write(n + "\n");
        }
    }

    private String getNamespace(IRI uri) {
        return uri.toString().replaceFirst("[A-Za-z0-9_.-]*.$", "");
    }

    private BufferedWriter makeWriter(File f) throws IOException {
        if (f == null) return null;
        OutputStream stream = null;
        if (f.getName().endsWith(".gz")) {
            stream = new GZIPOutputStream(new FileOutputStream(f));
        } else {
            stream = new FileOutputStream(f);
        }
        return new BufferedWriter(new OutputStreamWriter(stream));
    }

    private void closeWriter(Writer w) throws IOException {
        if (w == null) return;
        w.flush();
        w.close();
    }

}
