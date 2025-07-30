package org.nanopub.op;

import com.beust.jcommander.ParameterException;
import net.trustyuri.TrustyUriException;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.PROV;
import org.eclipse.rdf4j.rio.*;
import org.nanopub.*;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Command-line utility to build nanopublications from RDF files.
 */
public class Build extends CliRunner {

    @com.beust.jcommander.Parameter(description = "input-rdf-files", required = true)
    private List<File> inputRdfdFiles = new ArrayList<>();

    @com.beust.jcommander.Parameter(names = "-d", description = "Point to the URI of the resource the created nanopublications are derived from")
    private String derivedFrom;

    @com.beust.jcommander.Parameter(names = "-c", description = "Creator of nanopublication")
    private List<String> creators = new ArrayList<>();

    @com.beust.jcommander.Parameter(names = "-o", description = "Output file")
    private File outputFile;

    @com.beust.jcommander.Parameter(names = "--in-format", description = "Format of the input RDF files: ttl, jsonld, nt, ttl.gz, ...")
    private String inFormat;

    @com.beust.jcommander.Parameter(names = "--out-format", description = "Format of the output nanopubs: trig, nq, trix, trig.gz, ...")
    private String outFormat;

    /**
     * Main method to run the Build command-line utility.
     *
     * @param args Command-line arguments
     */
    public static void main(String[] args) {
        try {
            Build obj = CliRunner.initJc(new Build(), args);
            obj.run();
        } catch (ParameterException ex) {
            System.exit(1);
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }

    private RDFFormat rdfInFormat, rdfOutFormat;
    private OutputStream outputStream = System.out;
    private Random random = new Random();
    private NanopubCreator npCreator;
    private IRI nanopubIri, assertionIri;
    private Resource previousSubj;
    private IRI derivedFromUri;
    private List<Pair<String, String>> namespaces = new ArrayList<>();

    private void run() throws IOException, RDFParseException, RDFHandlerException,
            MalformedNanopubException, TrustyUriException {

        if (outputFile == null) {
            if (outFormat == null) {
                outFormat = "trig";
            }
            rdfOutFormat = Rio.getParserFormatForFileName("file." + outFormat).orElse(null);
        } else {
            rdfOutFormat = Rio.getParserFormatForFileName(outputFile.getName()).orElse(null);
            if (outputFile.getName().endsWith(".gz")) {
                outputStream = new GZIPOutputStream(new FileOutputStream(outputFile));
            } else {
                outputStream = new FileOutputStream(outputFile);
            }
        }
        if (derivedFrom != null) {
            derivedFromUri = vf.createIRI(derivedFrom);
        }

        for (File inputFile : inputRdfdFiles) {
            String dummyFileName;
            if (inFormat != null) {
                dummyFileName = "file." + inFormat;
            } else {
                dummyFileName = inputFile.toString();
            }
            rdfInFormat = Rio.getParserFormatForFileName(dummyFileName).orElse(null);
            InputStream inputStream;
            if (dummyFileName.matches(".*\\.(gz|gzip)")) {
                inputStream = new GZIPInputStream(new BufferedInputStream(new FileInputStream(inputFile)));
            } else {
                inputStream = new BufferedInputStream(new FileInputStream(inputFile));
            }

            RDFParser parser = Rio.createParser(rdfInFormat);
            parser.setRDFHandler(new RDFHandler() {

                @Override
                public void startRDF() throws RDFHandlerException {
                }

                @Override
                public void handleStatement(Statement st) throws RDFHandlerException {
                    processStatement(st);
                }

                @Override
                public void handleNamespace(String prefix, String uri) throws RDFHandlerException {
                    processNamespace(prefix, uri);
                }

                @Override
                public void handleComment(String comment) throws RDFHandlerException {
                }

                @Override
                public void endRDF() throws RDFHandlerException {
                }

            });
            parser.parse(inputStream, "http://example.com/baseuri");

            inputStream.close();
        }

        if (previousSubj != null) finalizeNanopub();

        outputStream.flush();
        if (outputStream != System.out) {
            outputStream.close();
        }
    }

    private void processStatement(Statement st) {
        prepareCreator(st.getSubject());
        npCreator.addAssertionStatements(st);
    }

    private void processNamespace(String prefix, String uri) {
        prepareCreator(null);
        namespaces.add(Pair.of(prefix, uri));
        npCreator.addNamespace(prefix, uri);
    }

    private void prepareCreator(Resource subj) {
        if (previousSubj != null && subj != null && !subj.equals(previousSubj)) {
            finalizeNanopub();
        }
        if (npCreator == null) {
            initNanopub();
        }
        previousSubj = subj;
    }

    private void initNanopub() {
        String npUriString = "http://purl.org/nanopub/temp/" + Math.abs(random.nextInt()) + "/";
        nanopubIri = vf.createIRI(npUriString);
        assertionIri = vf.createIRI(npUriString + "assertion");
        if (creators.isEmpty()) creators.add(npUriString + "creator");
        npCreator = new NanopubCreator(nanopubIri);
        npCreator.setAssertionUri(assertionIri);
        npCreator.addDefaultNamespaces();
        for (Pair<String, String> p : namespaces) {
            npCreator.addNamespace(p.getLeft(), p.getRight());
        }
        if (derivedFromUri != null) {
            npCreator.addProvenanceStatement(PROV.WAS_DERIVED_FROM, derivedFromUri);
        } else {
            for (String c : creators) {
                npCreator.addProvenanceStatement(PROV.HAD_PRIMARY_SOURCE, vf.createIRI(c));
            }
        }
        for (String c : creators) {
            npCreator.addCreator(vf.createIRI(c));
        }
    }

    private void finalizeNanopub() {
        npCreator.addTimestampNow();
        npCreator.setRemoveUnusedPrefixesEnabled(true);
        try {
            Nanopub np = npCreator.finalizeNanopub();
            NanopubUtils.writeToStream(np, outputStream, rdfOutFormat);
        } catch (MalformedNanopubException ex) {
            throw new RuntimeException(ex);
        }
        npCreator = null;
    }

    private static SimpleValueFactory vf = SimpleValueFactory.getInstance();

}
