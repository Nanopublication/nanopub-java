package org.nanopub.op;

import com.beust.jcommander.ParameterException;
import net.trustyuri.TrustyUriException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.rio.*;
import org.eclipse.rdf4j.rio.helpers.AbstractRDFHandler;
import org.nanopub.*;

import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Command-line utility to import RDF data into nanopublications.
 */
public class Import extends CliRunner {

    @com.beust.jcommander.Parameter(description = "input-file", required = true)
    private List<File> inputFiles = new ArrayList<>();

    @com.beust.jcommander.Parameter(names = "-t", description = "Input type (currently supported: cedar)", required = true)
    private String type = null;

    @com.beust.jcommander.Parameter(names = "-o", description = "Output file")
    private File outputFile;

    @com.beust.jcommander.Parameter(names = "--in-format", description = "Format of the input: ttl, nt, trig, nq, trix, trig.gz, ...")
    private String inFormat;

    @com.beust.jcommander.Parameter(names = "--out-format", description = "Format of the output nanopubs: trig, nq, trix, trig.gz, ...")
    private String outFormat;

    /**
     * Main method to run the import operation.
     *
     * @param args Command-line arguments
     */
    public static void main(String[] args) {
        try {
            Import obj = CliRunner.initJc(new Import(), args);
            if (obj.inputFiles.size() != 1) {
                obj.getJc().usage();
                System.exit(1);
            }
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

    private void run() throws IOException, RDFParseException, RDFHandlerException,
            MalformedNanopubException, TrustyUriException {

        File inputFile = inputFiles.get(0);
        if (inFormat != null) {
            rdfInFormat = Rio.getParserFormatForFileName("file." + inFormat).orElse(null);
        } else {
            rdfInFormat = Rio.getParserFormatForFileName(inputFile.toString()).orElse(null);
        }
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

        List<Nanopub> nanopubs = createNanopubs(inputFile, type, rdfInFormat);
        for (Nanopub np : nanopubs) {
            NanopubUtils.writeToStream(np, outputStream, rdfOutFormat);
        }

        outputStream.flush();
        if (outputStream != System.out) {
            outputStream.close();
        }
    }

    /**
     * Creates nanopublications from the given RDF file.
     *
     * @param file   the RDF file to read
     * @param type   the type of import
     * @param format the RDF format of the input file
     * @return a list of created nanopublications
     * @throws IOException               if an I/O error occurs
     * @throws RDFParseException         if the RDF data cannot be parsed
     * @throws RDFHandlerException       if there is an error handling the RDF data
     * @throws MalformedNanopubException if the nanopublication is malformed
     */
    public static List<Nanopub> createNanopubs(File file, String type, RDFFormat format)
            throws IOException, RDFParseException, RDFHandlerException, MalformedNanopubException {
        final NanopubImporter importer;
        if ("cedar".equals(type)) {
            importer = new CedarNanopubImporter();
        } else {
            throw new IllegalArgumentException("Unknown import type: " + type);
        }

        InputStream in;
        if (file.getName().matches(".*\\.(gz|gzip)")) {
            in = new GZIPInputStream(new BufferedInputStream(new FileInputStream(file)));
        } else {
            in = new BufferedInputStream(new FileInputStream(file));
        }
        RDFParser p = NanopubUtils.getParser(format);
        final List<Statement> statements = new ArrayList<>();
        RDFHandler rdfHandler = new AbstractRDFHandler() {

            @Override
            public void handleStatement(Statement st) throws RDFHandlerException {
                statements.add(st);
                super.handleStatement(st);
            }

        };
        p.setRDFHandler(rdfHandler);
        try {
            p.parse(new InputStreamReader(in, Charset.forName("UTF-8")), "");
        } finally {
            in.close();
        }
        importer.readStatements(statements);
        importer.finalizeNanopubs();
        return importer.getNanopubs();
    }


    /**
     * Interface for importing RDF data into nanopublications.
     */
    public static interface NanopubImporter {

        /**
         * Reads RDF statements and prepares them for nanopublication creation.
         *
         * @param statements the list of RDF statements to read
         */
        public void readStatements(List<Statement> statements);

        /**
         * Finalizes the nanopublications after all statements have been read.
         */
        public void finalizeNanopubs();

        /**
         * Returns the list of created nanopublications.
         *
         * @return a list of nanopublications
         */
        public List<Nanopub> getNanopubs();

    }

    /**
     * Importer for Cedar template instances into nanopublications.
     */
    public static class CedarNanopubImporter implements NanopubImporter {

        private String npIriString;
        private IRI npIri;
        private NanopubCreator npCreator;
        private List<Nanopub> nanopubs;

        /**
         * Default constructor for CedarNanopubImporter.
         */
        public CedarNanopubImporter() {
        }

        /**
         * Read RDF statements and prepare them for nanopublication creation.
         *
         * @param statements the list of RDF statements to read
         */
        @Override
        public void readStatements(List<Statement> statements) {
            String cedarId = getCedarId(statements);
            npIriString = "http://purl.org/nanopub/temp/" + cedarId + "#";
            npIri = vf.createIRI(npIriString);

            npCreator = new NanopubCreator(npIri);
            addNamespaces();
            npCreator.setAssertionUri(npIriString + "assertion");
            npCreator.setProvenanceUri(npIriString + "provenance");
            npCreator.setPubinfoUri(npIriString + "pubinfo");

            npCreator.addPubinfoStatement(vf.createIRI("https://repo.metadatacenter.org/template-instances/" + cedarId), DCTERMS.HAS_VERSION, npIri);

            for (Statement st : statements) {
                if (st.getPredicate().stringValue().equals("http://open-services.net/ns/core#modifiedBy")) {
                    npCreator.addProvenanceStatement(SimpleCreatorPattern.PAV_AUTHOREDBY, st.getObject());
                    npCreator.addPubinfoStatement(DCTERMS.CREATOR, st.getObject());
                } else if (st.getPredicate().stringValue().equals("http://purl.org/pav/lastUpdatedOn")) {
                    npCreator.addPubinfoStatement(DCTERMS.CREATED, st.getObject());
                } else if (st.getPredicate().stringValue().equals("http://purl.org/pav/createdBy")) {
                    npCreator.addPubinfoStatements(st);
                } else if (st.getPredicate().stringValue().equals("http://purl.org/pav/createdOn")) {
                    npCreator.addPubinfoStatements(st);
                } else if (st.getPredicate().stringValue().equals("http://schema.org/description")) {
                    npCreator.addPubinfoStatements(st);
                } else if (st.getPredicate().stringValue().equals("http://schema.org/isBasedOn")) {
                    npCreator.addPubinfoStatements(st);
                } else if (st.getPredicate().stringValue().equals("http://schema.org/name")) {
                    npCreator.addPubinfoStatements(st);
                } else if (st.getSubject() instanceof IRI) {
                    String s = st.getSubject().stringValue().replaceFirst("^https://repo.metadatacenter.org/template-instances/.*$", npIriString + "subj");
                    npCreator.addAssertionStatements(vf.createStatement(vf.createIRI(s), st.getPredicate(), st.getObject()));
                } else {
                    npCreator.addAssertionStatements(st);
                }
            }
        }

        private String getCedarId(List<Statement> statements) {
            for (Statement st : statements) {
                String s = st.getSubject().stringValue();
                if (s.startsWith("https://repo.metadatacenter.org/template-instances/")) {
                    return s.replaceFirst("^https://repo.metadatacenter.org/template-instances/", "");
                }
            }
            throw new RuntimeException("No Cedar ID found");
        }

        private void addNamespaces() {
            npCreator.addNamespace("", npIriString);
            npCreator.addNamespace("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
            npCreator.addNamespace("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
            npCreator.addNamespace("rdfg", "http://www.w3.org/2004/03/trix/rdfg-1/");
            npCreator.addNamespace("xsd", "http://www.w3.org/2001/XMLSchema#");
            npCreator.addNamespace("owl", "http://www.w3.org/2002/07/owl#");
            npCreator.addNamespace("dct", "http://purl.org/dc/terms/");
            npCreator.addNamespace("dce", "http://purl.org/dc/elements/1.1/");
            npCreator.addNamespace("pav", "http://purl.org/pav/");
            npCreator.addNamespace("np", "http://www.nanopub.org/nschema#");
            npCreator.addNamespace("skos", "http://www.w3.org/TR/skos-reference/skos-owl1-dl#");
            npCreator.addNamespace("obo", "http://purl.obolibrary.org/obo/");
            npCreator.addNamespace("schema", "http://schema.org/");
            npCreator.addNamespace("cedar-user", "https://metadatacenter.org/users/");
            npCreator.addNamespace("cedar-temp", "https://repo.metadatacenter.org/templates/");
            npCreator.addNamespace("cedar-tempinst", "https://repo.metadatacenter.org/template-instances/");
            npCreator.addNamespace("cedar-tempelinst", "https://repo.metadatacenter.org/template-element-instances/");
            npCreator.addNamespace("cedar-prop", "https://schema.metadatacenter.org/properties/");
        }

        /**
         * Finalizes the nanopublications after all statements have been read.
         */
        @Override
        public void finalizeNanopubs() {
            try {
                nanopubs = new ArrayList<>();
                nanopubs.add(npCreator.finalizeNanopub());
            } catch (MalformedNanopubException ex) {
                throw new RuntimeException(ex);
            }
        }

        /**
         * Returns the list of created nanopublications.
         *
         * @return a list of nanopublications
         */
        @Override
        public List<Nanopub> getNanopubs() {
            return nanopubs;
        }

    }

    private static ValueFactory vf = SimpleValueFactory.getInstance();

}
