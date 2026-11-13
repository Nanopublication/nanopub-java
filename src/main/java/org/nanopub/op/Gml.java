package org.nanopub.op;

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
import org.nanopub.Nanopub;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

/**
 * Command-line utility to convert nanopublications to GML format.
 */
public class Gml extends CliRunner {

    @com.beust.jcommander.Parameter(description = "input-nanopubs", required = true)
    private List<File> inputNanopubs = new ArrayList<>();

    @com.beust.jcommander.Parameter(names = "-o", description = "Output file")
    private File outputFile;

    @com.beust.jcommander.Parameter(names = "--in-format", description = "Format of the input nanopubs: trig, nq, trix, trig.gz, ...")
    private String inFormat;

    /**
     * Main method to run the Gml utility.
     *
     * @param args Command-line arguments
     */
    public static void main(String[] args) {
        CliSupport.exitWith(execute(args));
    }

    /**
     * Runs the tool and returns the process exit code, rather than ending the JVM itself,
     * so that callers (including tests) can check the outcome.
     *
     * @param args command-line arguments
     * @return 0 if the run completed, 1 if it failed
     */
    static int execute(String[] args) {
        return CliSupport.execute(() -> CliRunner.initJc(new Gml(), args).run());
    }

    private RDFFormat rdfInFormat;
    private OutputStream outputStream = System.out;
    private Map<String, Integer> nodes = new HashMap<>();
    private BufferedWriter writer;
    private int nodeCounter = 0;

    private void run() throws IOException, RDFParseException, RDFHandlerException,
            MalformedNanopubException, TrustyUriException {
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
            writer.write("graph [\n");

            MultiNanopubRdfHandler.process(rdfInFormat, inputFile, np -> {
                try {
                    process(np);
                } catch (RDFHandlerException | IOException ex) {
                    throw new RuntimeException(ex);
                }
            });

            writer.write("]\n");

            writer.flush();
            if (outputStream != System.out) {
                writer.close();
            }
        }
    }

    private void process(Nanopub np) throws RDFHandlerException, IOException {
        for (Statement st : np.getAssertion()) {
            if (!(st.getObject() instanceof IRI)) continue;
            String s = st.getSubject().stringValue();
            String p = st.getPredicate().stringValue();
            String o = st.getObject().stringValue();
            int si, oi;
            if (nodes.containsKey(s)) {
                si = nodes.get(s);
            } else {
                si = nodeCounter++;
                nodes.put(s, si);
                writer.write("node [\n");
                writer.write("id N" + si + "N\n");
                writer.write("label \"" + s + "\"\n");
                writer.write("]\n");
            }
            if (nodes.containsKey(o)) {
                oi = nodes.get(o);
            } else {
                oi = nodeCounter++;
                nodes.put(o, oi);
                writer.write("node [\n");
                writer.write("id N" + oi + "N\n");
                writer.write("label \"" + o + "\"\n");
                writer.write("]\n");
            }
            writer.write("edge [\n");
            writer.write("source N" + si + "N\n");
            writer.write("target N" + oi + "N\n");
            writer.write("label \"" + p + "\"\n");
            writer.write("]\n");
        }
    }

}
