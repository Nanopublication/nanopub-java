package org.nanopub;

import com.beust.jcommander.ParameterException;
import net.trustyuri.TrustyUriResource;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;

import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.zip.GZIPOutputStream;

/**
 * Update the timestamp of (unsigned) Nanopublications.
 */
public class TimestampUpdater extends CliRunner {

    @com.beust.jcommander.Parameter(description = "input-nanopub-files", required = true)
    private List<File> inputNanopubFiles = new ArrayList<>();

    @com.beust.jcommander.Parameter(names = "-o", description = "Output file")
    private File singleOutputFile; // only possible if there is only one inputFile

    @com.beust.jcommander.Parameter(names = "-v", description = "Verbose")
    private boolean verbose = false;

    private ValueFactory vf = SimpleValueFactory.getInstance();

    public static void main(String[] args) {
        try {
            TimestampUpdater obj = CliRunner.initJc(new TimestampUpdater(), args);
            obj.run();
        } catch (ParameterException ex) {
            System.exit(1);
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }

    public void run () throws MalformedNanopubException, IOException {

        final OutputStream singleOut;
        if (singleOutputFile != null) {
            if (singleOutputFile.getName().matches(".*\\.(gz|gzip)")) {
                singleOut = new GZIPOutputStream(new FileOutputStream(singleOutputFile));
            } else {
                singleOut = new FileOutputStream(singleOutputFile);
            }
        } else {
            singleOut = null;
        }

        for (File inputFile : inputNanopubFiles) {
            File outputFile;
            final OutputStream out;
            if (singleOutputFile == null) {
                outputFile = new File(inputFile.getParent(), "updated." + inputFile.getName());
                if (inputFile.getName().matches(".*\\.(gz|gzip)")) {
                    out = new GZIPOutputStream(new FileOutputStream(outputFile));
                } else {
                    out = new FileOutputStream(outputFile);
                }
            } else {
                outputFile = singleOutputFile;
                out = singleOut;
            }
            final RDFFormat inFormat = new TrustyUriResource(inputFile).getFormat(RDFFormat.TRIG);
            final RDFFormat outFormat = new TrustyUriResource(outputFile).getFormat(RDFFormat.TRIG);
            try (out) {
                MultiNanopubRdfHandler.process(inFormat, inputFile, new MultiNanopubRdfHandler.NanopubHandler() {

                    @Override
                    public void handleNanopub(Nanopub np) {
                        try {
                            List<Statement> newStatements = removeCreationTime(np);
                            newStatements.add(vf.createStatement(np.getUri(), SimpleTimestampPattern.DCT_CREATED, vf.createLiteral(new Date()), np.getPubinfoUri()));
                            Nanopub updatedNp;
                            if (np instanceof NanopubImpl) {
                                updatedNp = new NanopubImpl(newStatements, ((NanopubImpl)np).getNsPrefixes(), ((NanopubImpl)np).getNs());
                            } else {
                                updatedNp = new NanopubImpl(newStatements);
                            }

                            RDFWriter w = Rio.createWriter(outFormat, new OutputStreamWriter(out, Charset.forName("UTF-8")));
                            NanopubUtils.propagateToHandler(updatedNp, w);

                            if (verbose) {
                                System.out.println("Nanopub URI: " + np.getUri());
                            }
                        } catch (RDFHandlerException ex) {
                            ex.printStackTrace();
                            throw new RuntimeException(ex);
                        } catch (MalformedNanopubException e) {
                            throw new RuntimeException(e);
                        }
                    }

                });
            }
        }
    }

    private List<Statement> removeCreationTime(Nanopub np) {
        List<Statement> statements = NanopubUtils.getStatements(np);
        List<Statement> statementsToRemove = new ArrayList<>();
        for (Statement st : statements) {
            if (!st.getContext().equals(np.getPubinfoUri())) continue;
            if (!st.getSubject().equals(np.getUri())) continue;
            if (!SimpleTimestampPattern.isCreationTimeProperty(st.getPredicate())) continue;
            if (!(st.getObject() instanceof Literal l)) continue;
            if (!l.getDatatype().equals(SimpleTimestampPattern.XSD_DATETIME)) continue;
            statementsToRemove.add(st);
        }
        for (Statement st : statementsToRemove) {
            statements.remove(st);
        }
        return statements;
    }

}
