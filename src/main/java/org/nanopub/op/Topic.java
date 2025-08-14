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
import org.nanopub.op.topic.DefaultTopics;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPOutputStream;

/**
 * Command-line utility to work with topics in nanopublications.
 */
public class Topic extends CliRunner {

    @com.beust.jcommander.Parameter(description = "input-nanopubs")
    private List<File> inputNanopubs = new ArrayList<>();

    @com.beust.jcommander.Parameter(names = "-o", description = "Output file")
    private File outputFile;

    @com.beust.jcommander.Parameter(names = "--in-format", description = "Format of the input nanopubs: trig, nq, trix, trig.gz, ...")
    private String inFormat;

    @com.beust.jcommander.Parameter(names = "-i", description = "Property URIs to ignore, separated by '|' (has no effect if -d is set)")
    private String ignoreProperties;

    @com.beust.jcommander.Parameter(names = "-h", description = "Topic handler class")
    private String handlerClass;

    /**
     * Main method to run the Topic command-line utility.
     *
     * @param args Command-line arguments
     */
    public static void main(String[] args) {
        try {
            Topic obj = CliRunner.initJc(new Topic(), args);
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
     * Creates an instance of the Topic class with the given arguments.
     *
     * @param args Command-line arguments as a single string.
     * @return An instance of Topic initialized with the provided arguments.
     * @throws com.beust.jcommander.ParameterException If the arguments are invalid or missing.
     */
    public static Topic getInstance(String args) throws ParameterException {
        if (args == null) {
            args = "";
        }
        Topic obj = CliRunner.initJc(new Topic(), args.trim().split(" "));
        obj.init();
        return obj;
    }

    private RDFFormat rdfInFormat;
    private OutputStream outputStream = System.out;
    private BufferedWriter writer;
    private TopicHandler topicHandler;

    private void init() {
        if (handlerClass != null && !handlerClass.isEmpty()) {
            String detectorClassName = handlerClass;
            if (!handlerClass.contains(".")) {
                detectorClassName = "org.nanopub.op.topic." + handlerClass;
            }
            try {
                topicHandler = (TopicHandler) Class.forName(detectorClassName).getConstructor().newInstance();
            } catch (ReflectiveOperationException ex) {
                throw new RuntimeException(ex);
            }
        } else {
            topicHandler = new DefaultTopics(ignoreProperties);
        }
    }

    /**
     * Runs the Topic command-line utility.
     *
     * @throws java.io.IOException                       if there is an error reading or writing files.
     * @throws org.eclipse.rdf4j.rio.RDFParseException   if there is an error parsing RDF data.
     * @throws org.eclipse.rdf4j.rio.RDFHandlerException if there is an error handling RDF data.
     * @throws org.nanopub.MalformedNanopubException     if a nanopublication is malformed.
     * @throws net.trustyuri.TrustyUriException          if there is an error with Trusty URIs.
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
                    writer.write(np.getUri() + " " + getTopic(np) + "\n");
                } catch (IOException ex) {
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
     * Gets the topic of a given nanopublication using the configured topic handler.
     *
     * @param np The nanopublication for which to get the topic.
     * @return The topic of the nanopublication as a String.
     */
    public String getTopic(Nanopub np) {
        return topicHandler.getTopic(np);
    }


    /**
     * Interface for handling topics in nanopublications.
     */
    public interface TopicHandler {

        /**
         * Gets the topic of a given nanopublication.
         *
         * @param np The nanopublication for which to get the topic.
         * @return The topic of the nanopublication as a String.
         */
        public String getTopic(Nanopub np);

    }

}
