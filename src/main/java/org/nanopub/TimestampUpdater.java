package org.nanopub;

import com.beust.jcommander.ParameterException;
import net.trustyuri.TrustyUriUtils;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.zip.GZIPOutputStream;

/**
 * Update the timestamp of (unsigned) Nanopublications.
 */
public class TimestampUpdater extends CliRunner {

    @com.beust.jcommander.Parameter(description = "input-nanopub-files", required = true)
    private List<File> inputNanopubFiles = new ArrayList<>();

    @com.beust.jcommander.Parameter(names = "-o", description = "Output file")
    private File singleOutputFile; // receives the nanopubs of all input files; may be one of the input files

    @com.beust.jcommander.Parameter(names = "-v", description = "Verbose")
    private boolean verbose = false;

    private ValueFactory vf = SimpleValueFactory.getInstance();

    /**
     * Main method to run the TimestampUpdater.
     *
     * @param args Command line arguments.
     */
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

    /**
     * Runs the TimestampUpdater to update the timestamps of the provided Nanopublications.
     *
     * @throws org.nanopub.MalformedNanopubException if a Nanopublication is malformed.
     * @throws java.io.IOException                   if an I/O error occurs.
     */
    public void run() throws MalformedNanopubException, IOException {
        if (singleOutputFile == null) {
            for (File inputFile : inputNanopubFiles) {
                try (ReplacingOutput out = new ReplacingOutput(new File(inputFile.getParent(), "updated." + inputFile.getName()))) {
                    update(inputFile, out);
                    out.commit();
                }
            }
        } else {
            try (ReplacingOutput out = new ReplacingOutput(singleOutputFile)) {
                for (File inputFile : inputNanopubFiles) {
                    update(inputFile, out);
                }
                out.commit();
            }
        }
    }

    private void update(File inputFile, ReplacingOutput out) throws MalformedNanopubException, IOException {
        final RDFFormat inFormat = getFormat(inputFile, RDFFormat.TRIG);
        MultiNanopubRdfHandler.process(inFormat, inputFile, np -> {
            try {
                List<Statement> newStatements = removeCreationTime(np);
                newStatements.add(vf.createStatement(np.getUri(), DCTERMS.CREATED, TimestampNow.getTimestamp(), np.getPubinfoUri()));
                Nanopub updatedNp;
                if (np instanceof NanopubImpl) {
                    updatedNp = new NanopubImpl(newStatements, ((NanopubImpl) np).getNsPrefixes(), ((NanopubImpl) np).getNs());
                } else {
                    updatedNp = new NanopubImpl(newStatements);
                }

                RDFWriter w = Rio.createWriter(out.format, new OutputStreamWriter(out.stream, StandardCharsets.UTF_8));
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
        });
    }

    /**
     * Determines the RDF format of a file from its name alone, so it also works for an output file that does
     * not exist yet. Same derivation as in {@code SignNanopub}.
     */
    private static RDFFormat getFormat(File file, RDFFormat defaultFormat) {
        String mimetype = TrustyUriUtils.getMimetype(file.toString());
        Optional<RDFFormat> format = mimetype == null ? Optional.empty() : Rio.getParserFormatForMIMEType(mimetype);
        if (format.isEmpty()) {
            format = Rio.getParserFormatForFileName(file.toString());
        }
        return format.orElse(defaultFormat);
    }

    /**
     * An output that is written to a temporary file next to its target and only moved over the target once
     * the whole run has succeeded. This makes it safe to name an input file as the output (an in-place
     * update: the input is fully read before it is replaced), and a failed run leaves an existing target
     * untouched instead of empty.
     */
    private static class ReplacingOutput implements Closeable {

        private final File target;
        private final Path temp;
        final OutputStream stream;
        final RDFFormat format;
        private boolean committed = false;

        ReplacingOutput(File target) throws IOException {
            this.target = target;
            this.format = getFormat(target, RDFFormat.TRIG);
            Path dir = target.getAbsoluteFile().getParentFile().toPath();
            // Created with the default permissions, unlike Files.createTempFile, so an in-place update does
            // not change the file's permissions.
            this.temp = dir.resolve("." + target.getName() + "." + UUID.randomUUID() + ".tmp");
            OutputStream fileOut = Files.newOutputStream(temp, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            this.stream = target.getName().matches(".*\\.(gz|gzip)") ? new GZIPOutputStream(fileOut) : fileOut;
        }

        void commit() throws IOException {
            stream.close();
            try {
                Files.move(temp, target.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(temp, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            committed = true;
        }

        @Override
        public void close() throws IOException {
            if (committed) return;
            try {
                stream.close();
            } finally {
                Files.deleteIfExists(temp);
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
            if (!l.getDatatype().equals(XSD.DATETIME)) continue;
            statementsToRemove.add(st);
        }
        for (Statement st : statementsToRemove) {
            statements.remove(st);
        }
        return statements;
    }

}
