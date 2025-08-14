package org.nanopub.op;

import com.beust.jcommander.ParameterException;
import net.trustyuri.TrustyUriException;
import net.trustyuri.TrustyUriUtils;
import org.apache.commons.codec.Charsets;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.Rio;
import org.nanopub.*;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Command-line utility to create a tar archive of nanopublications.
 */
public class Tar extends CliRunner {

    @com.beust.jcommander.Parameter(description = "input-nanopubs", required = true)
    private List<File> inputNanopubs = new ArrayList<>();

    @com.beust.jcommander.Parameter(names = "-o", description = "Output file", required = true)
    private File outputFile;

    @com.beust.jcommander.Parameter(names = "--in-format", description = "Format of the input nanopubs: trig, nq, trix, trig.gz, ...")
    private String inFormat;

    /**
     * Main method to run the Tar utility.
     *
     * @param args Command-line arguments
     */
    public static void main(String[] args) {
        try {
            Tar obj = CliRunner.initJc(new Tar(), args);
            obj.run();
        } catch (ParameterException ex) {
            System.exit(1);
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }

    private RDFFormat rdfInFormat;
    private TarArchiveOutputStream outputStream;

    private void run() throws IOException, RDFParseException, RDFHandlerException,
            MalformedNanopubException, TrustyUriException {

        outputStream = new TarArchiveOutputStream(new BufferedOutputStream(new FileOutputStream(outputFile)));
        try {

            for (File inputFile : inputNanopubs) {
                if (inFormat != null) {
                    rdfInFormat = Rio.getParserFormatForFileName("file." + inFormat).orElse(null);
                } else {
                    rdfInFormat = Rio.getParserFormatForFileName(inputFile.toString()).orElse(null);
                }

                MultiNanopubRdfHandler.process(rdfInFormat, inputFile, np -> {
                    try {
                        process(np);
                    } catch (RDFHandlerException ex) {
                        throw new RuntimeException(ex);
                    }
                });

            }
            outputStream.finish();
            outputStream.flush();
        } finally {
            outputStream.close();
        }
    }

    private void process(Nanopub np) throws RDFHandlerException {
        try {
            String filename = TrustyUriUtils.getArtifactCode(np.getUri().stringValue());
            filename = filename.replaceFirst("^(..)(..)(..)", "$1/$2/$3/");
            TarArchiveEntry tarEntry = new TarArchiveEntry(filename);
            byte[] npBytes = NanopubUtils.writeToString(np, RDFFormat.TRIG).getBytes(Charsets.UTF_8);
            tarEntry.setSize(npBytes.length);
            outputStream.putArchiveEntry(tarEntry);
            outputStream.write(npBytes);
            outputStream.closeArchiveEntry();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

}
