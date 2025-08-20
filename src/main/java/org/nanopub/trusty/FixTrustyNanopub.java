package org.nanopub.trusty;

import com.beust.jcommander.ParameterException;
import net.trustyuri.TrustyUriException;
import net.trustyuri.TrustyUriResource;
import net.trustyuri.TrustyUriUtils;
import net.trustyuri.rdf.RdfFileContent;
import net.trustyuri.rdf.RdfUtils;
import org.eclipse.rdf4j.rio.*;
import org.nanopub.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPOutputStream;

/**
 * Fixes nanopubs with broken Trusty URIs.
 */
public class FixTrustyNanopub extends CliRunner {

    @com.beust.jcommander.Parameter(description = "input-nanopubs", required = true)
    private List<File> inputNanopubs = new ArrayList<>();

    @com.beust.jcommander.Parameter(names = "-v", description = "Verbose")
    private boolean verbose = false;

    /**
     * Main method to run the FixTrustyNanopub tool.
     *
     * @param args Command line arguments.
     */
    public static void main(String[] args) {
        try {
            FixTrustyNanopub obj = CliRunner.initJc(new FixTrustyNanopub(), args);
            obj.run();
        } catch (ParameterException ex) {
            System.exit(1);
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }

    private int count;

    private void run() throws IOException, RDFParseException, RDFHandlerException,
            MalformedNanopubException, TrustyUriException {
        for (File inputFile : inputNanopubs) {
            File outFile = new File(inputFile.getParent(), "fixed." + inputFile.getName());
            final OutputStream out;
            if (inputFile.getName().matches(".*\\.(gz|gzip)")) {
                out = new GZIPOutputStream(new FileOutputStream(outFile));
            } else {
                out = new FileOutputStream(outFile);
            }
            final RDFFormat format = new TrustyUriResource(inputFile).getFormat(RDFFormat.TRIG);
            try (out) {
                MultiNanopubRdfHandler.process(format, inputFile, np -> {
                    try {
                        np = writeAsFixedNanopub(np, format, out);
                        count++;
                        if (verbose) {
                            System.out.println("Nanopub URI: " + np.getUri());
                        } else {
                            if (count % 100 == 0) {
                                System.err.print(count + " nanopubs...\r");
                            }
                        }
                    } catch (RDFHandlerException | TrustyUriException ex) {
                        throw new RuntimeException(ex);
                    }
                });
            }
        }
    }

    /**
     * Fixes a nanopub with a broken Trusty URI.
     *
     * @param nanopub the nanopub to fix
     * @return the fixed nanopub
     * @throws net.trustyuri.TrustyUriException if the nanopub cannot be fixed due to a malformed Trusty URI
     */
    public static Nanopub fix(Nanopub nanopub) throws TrustyUriException {
        Nanopub np;
        if (nanopub instanceof NanopubWithNs) {
            ((NanopubWithNs) nanopub).removeUnusedPrefixes();
        }
        try {
            RdfFileContent r = new RdfFileContent(RDFFormat.TRIG);
            NanopubUtils.propagateToHandler(nanopub, r);
            NanopubRdfHandler h = new NanopubRdfHandler();
            if (!TrustyUriUtils.isPotentialTrustyUri(nanopub.getUri())) {
                throw new TrustyUriException("Not a (broken) trusty URI: " + nanopub.getUri());
            }
            String oldArtifactCode = TrustyUriUtils.getArtifactCode(nanopub.getUri().toString());
            RdfUtils.fixTrustyRdf(r, oldArtifactCode, h);
            np = h.getNanopub();
        } catch (RDFHandlerException | MalformedNanopubException ex) {
            throw new TrustyUriException(ex);
        }
        return np;
    }

    /**
     * Transforms a multi-nanopub file into fixed nanopubs and writes them to the output stream.
     *
     * @param format the RDF format of the input nanopubs
     * @param file   the input file containing multiple nanopubs
     * @param out    the output stream to write the fixed nanopubs
     * @throws java.io.IOException                       if an I/O error occurs
     * @throws org.eclipse.rdf4j.rio.RDFParseException   if an error occurs while parsing RDF
     * @throws org.eclipse.rdf4j.rio.RDFHandlerException if an error occurs while handling RDF
     * @throws org.nanopub.MalformedNanopubException     if a nanopub is malformed
     */
    public static void transformMultiNanopub(final RDFFormat format, File file, final OutputStream out)
            throws IOException, RDFParseException, RDFHandlerException, MalformedNanopubException {
        InputStream in = new FileInputStream(file);
        transformMultiNanopub(format, in, out);
    }

    /**
     * Transforms a multi-nanopub input stream into fixed nanopubs and writes them to the output stream.
     *
     * @param format the RDF format of the input nanopubs
     * @param in     the input stream containing multiple nanopubs
     * @param out    the output stream to write the fixed nanopubs
     * @throws java.io.IOException                       if an I/O error occurs
     * @throws org.eclipse.rdf4j.rio.RDFParseException   if an error occurs while parsing RDF
     * @throws org.eclipse.rdf4j.rio.RDFHandlerException if an error occurs while handling RDF
     * @throws org.nanopub.MalformedNanopubException     if a nanopub is malformed
     */
    public static void transformMultiNanopub(final RDFFormat format, InputStream in, final OutputStream out)
            throws IOException, RDFParseException, RDFHandlerException, MalformedNanopubException {
        try (in; out) {
            MultiNanopubRdfHandler.process(format, in, np -> {
                try {
                    writeAsFixedNanopub(np, format, out);
                } catch (RDFHandlerException | TrustyUriException ex) {
                    throw new RuntimeException(ex);
                }
            });
        }
    }

    /**
     * Writes a fixed nanopub to the output stream in the specified RDF format.
     *
     * @param np     the nanopub to write
     * @param format the RDF format to use for writing
     * @param out    the output stream to write the fixed nanopub
     * @return the fixed nanopub
     * @throws org.eclipse.rdf4j.rio.RDFHandlerException if an error occurs while writing RDF
     * @throws net.trustyuri.TrustyUriException          if the nanopub cannot be fixed due to a malformed Trusty URI
     */
    public static Nanopub writeAsFixedNanopub(Nanopub np, RDFFormat format, OutputStream out)
            throws RDFHandlerException, TrustyUriException {
        np = FixTrustyNanopub.fix(np);
        RDFWriter w = Rio.createWriter(format, new OutputStreamWriter(out, StandardCharsets.UTF_8));
        NanopubUtils.propagateToHandler(np, w);
        return np;
    }

}
