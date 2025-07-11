package org.nanopub.trusty;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.nanopub.CliRunner;
import org.nanopub.MalformedNanopubException;
import org.nanopub.MultiNanopubRdfHandler;
import org.nanopub.MultiNanopubRdfHandler.NanopubHandler;
import org.nanopub.Nanopub;
import org.nanopub.NanopubRdfHandler;
import org.nanopub.NanopubUtils;
import org.nanopub.NanopubWithNs;

import com.beust.jcommander.ParameterException;

import net.trustyuri.TrustyUriException;
import net.trustyuri.TrustyUriResource;
import net.trustyuri.TrustyUriUtils;
import net.trustyuri.rdf.RdfFileContent;
import net.trustyuri.rdf.TransformRdf;

/**
 * Command-line tool to transform nanopubs into Trusty Nanopubs.
 */
public class MakeTrustyNanopub extends CliRunner {

    @com.beust.jcommander.Parameter(description = "input-nanopub-files", required = true)
    private List<File> inputNanopubsFiles = new ArrayList<>();

    @com.beust.jcommander.Parameter(names = "-o", description = "Output file")
    private File singleOutputFile;

    @com.beust.jcommander.Parameter(names = "-r", description = "Resolve cross-nanopub references")
    private boolean resolveCrossRefs = false;

    @com.beust.jcommander.Parameter(names = "-R", description = "Resolve cross-nanopub references based on prefixes")
    private boolean resolveCrossRefsPrefixBased = false;

    @com.beust.jcommander.Parameter(names = "-v", description = "Verbose")
    private boolean verbose = false;

    /**
     * Main method to run the command-line tool.
     *
     * @param args Command-line arguments
     */
    public static void main(String[] args) {
        try {
            MakeTrustyNanopub obj = CliRunner.initJc(new MakeTrustyNanopub(), args);
            obj.run();
        } catch (ParameterException ex) {
            System.exit(1);
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }

    private void run() throws IOException, RDFParseException, RDFHandlerException, MalformedNanopubException, TrustyUriException {
        final Map<Resource, IRI> tempRefMap;
        final Map<String, String> tempPrefixMap;
        if (resolveCrossRefsPrefixBased) {
            tempPrefixMap = new HashMap<>();
            tempRefMap = new HashMap<>();
        } else if (resolveCrossRefs) {
            tempPrefixMap = null;
            tempRefMap = new HashMap<>();
        } else {
            tempPrefixMap = null;
            tempRefMap = null;
        }
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
        for (File inputFile : inputNanopubsFiles) {
            File outputFile;
            final OutputStream out;
            if (singleOutputFile == null) {
                outputFile = new File(inputFile.getParent(), "trusty." + inputFile.getName());
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
            MultiNanopubRdfHandler.process(inFormat, inputFile, new NanopubHandler() {

                @Override
                public void handleNanopub(Nanopub np) {
                    try {
                        np = writeAsTrustyNanopub(np, outFormat, out, tempRefMap, tempPrefixMap);
                        if (verbose) {
                            System.out.println("Nanopub URI: " + np.getUri());
                        }
                    } catch (RDFHandlerException | TrustyUriException ex) {
                        throw new RuntimeException(ex);
                    }
                }

            });
            if (singleOutputFile == null) {
                out.close();
            }
        }
        if (singleOutputFile != null) {
            singleOut.close();
        }
    }

    /**
     * Transform a Nanopub into a Trusty Nanopub.
     *
     * @param nanopub the Nanopub to transform
     * @return the transformed Trusty Nanopub
     * @throws TrustyUriException if the transformation fails due to an invalid URI or other issues
     */
    public static Nanopub transform(Nanopub nanopub) throws TrustyUriException {
        return transform(nanopub, null, null);
    }

    /**
     * Transform a Nanopub into a Trusty Nanopub, resolving cross-references and prefixes if provided.
     *
     * @param nanopub       the Nanopub to transform
     * @param tempRefMap    a map for temporary resource references, can be null
     * @param tempPrefixMap a map for temporary prefixes, can be null
     * @return the transformed Trusty Nanopub
     * @throws TrustyUriException if the transformation fails due to an invalid URI or other issues
     */
    public static Nanopub transform(Nanopub nanopub, Map<Resource, IRI> tempRefMap, Map<String, String> tempPrefixMap) throws TrustyUriException {
        String u = nanopub.getUri().stringValue();
        if (!nanopub.getHeadUri().stringValue().startsWith(u) || !nanopub.getAssertionUri().stringValue().startsWith(u) || !nanopub.getProvenanceUri().stringValue().startsWith(u) || !nanopub.getPubinfoUri().stringValue().startsWith(u)) {
            throw new TrustyUriException("Graph URIs need have the nanopub URI as prefix: " + u + "...");
        }
        Nanopub np;
        try {
            RdfFileContent r = new RdfFileContent(RDFFormat.TRIG);
            String npUri;
            Map<Resource, IRI> tempUriReplacerMap = null;
            if (TempUriReplacer.hasTempUri(nanopub)) {
                npUri = TempUriReplacer.normUri;
                tempUriReplacerMap = new HashMap<>();
                NanopubUtils.propagateToHandler(nanopub, new TempUriReplacer(nanopub, r, tempUriReplacerMap));
            } else {
                npUri = nanopub.getUri().toString();
                NanopubUtils.propagateToHandler(nanopub, r);
            }
            if (tempRefMap != null || tempPrefixMap != null) {
                if (tempRefMap == null) {
                    tempRefMap = new HashMap<>();
                }
                mergeTransformMaps(tempRefMap, tempUriReplacerMap);
                RdfFileContent r2 = new RdfFileContent(RDFFormat.TRIG);
                r.propagate(new CrossRefResolver(tempRefMap, tempPrefixMap, r2));
                r = r2;
            }
            NanopubRdfHandler h = new NanopubRdfHandler();
            Map<Resource, IRI> transformMap = TransformRdf.transformAndGetMap(r, h, npUri, TrustyNanopubUtils.transformRdfSetting);
            np = h.getNanopub();
            mergeTransformMaps(tempRefMap, transformMap);
            mergePrefixTransformMaps(tempPrefixMap, transformMap);
        } catch (RDFHandlerException | MalformedNanopubException ex) {
            throw new TrustyUriException(ex);
        }
        if (np instanceof NanopubWithNs) {
            ((NanopubWithNs) np).removeUnusedPrefixes();
        }
        return np;
    }

    /**
     * Transform a multi-nanopub file into Trusty Nanopubs, writing to the specified output stream.
     *
     * @param format the RDF format of the input file
     * @param file   the input file containing multiple nanopubs
     * @param out    the output stream to write the transformed nanopubs
     * @throws IOException               if an I/O error occurs
     * @throws RDFParseException         if there is an error parsing the RDF data
     * @throws RDFHandlerException       if there is an error handling the RDF data
     * @throws MalformedNanopubException if a nanopub is malformed
     */
    public static void transformMultiNanopub(final RDFFormat format, File file, final OutputStream out) throws IOException, RDFParseException, RDFHandlerException, MalformedNanopubException {
        transformMultiNanopub(format, file, out, false);
    }

    /**
     * Transform a multi-nanopub file into Trusty Nanopubs, writing to the specified output stream,
     * with an option to resolve cross-references.
     *
     * @param format           the RDF format of the input file
     * @param file             the input file containing multiple nanopubs
     * @param out              the output stream to write the transformed nanopubs
     * @param resolveCrossRefs whether to resolve cross-nanopub references
     * @throws IOException               if an I/O error occurs
     * @throws RDFParseException         if there is an error parsing the RDF data
     * @throws RDFHandlerException       if there is an error handling the RDF data
     * @throws MalformedNanopubException if a nanopub is malformed
     */
    public static void transformMultiNanopub(final RDFFormat format, File file, final OutputStream out, boolean resolveCrossRefs) throws IOException, RDFParseException, RDFHandlerException, MalformedNanopubException {
        InputStream in = new FileInputStream(file);
        transformMultiNanopub(format, in, out, resolveCrossRefs);
    }

    /**
     * Transform a multi-nanopub input stream into Trusty Nanopubs, writing to the specified output stream.
     *
     * @param format the RDF format of the input stream
     * @param in     the input stream containing multiple nanopubs
     * @param out    the output stream to write the transformed nanopubs
     * @throws IOException               if an I/O error occurs
     * @throws RDFParseException         if there is an error parsing the RDF data
     * @throws RDFHandlerException       if there is an error handling the RDF data
     * @throws MalformedNanopubException if a nanopub is malformed
     */
    public static void transformMultiNanopub(final RDFFormat format, InputStream in, final OutputStream out) throws IOException, RDFParseException, RDFHandlerException, MalformedNanopubException {
        transformMultiNanopub(format, in, out, false);
    }

    /**
     * Transform a multi-nanopub input stream into Trusty Nanopubs, writing to the specified output stream,
     * with an option to resolve cross-references.
     *
     * @param format           the RDF format of the input stream
     * @param in               the input stream containing multiple nanopubs
     * @param out              the output stream to write the transformed nanopubs
     * @param resolveCrossRefs whether to resolve cross-nanopub references
     * @throws IOException               if an I/O error occurs
     * @throws RDFParseException         if there is an error parsing the RDF data
     * @throws RDFHandlerException       if there is an error handling the RDF data
     * @throws MalformedNanopubException if a nanopub is malformed
     */
    public static void transformMultiNanopub(final RDFFormat format, InputStream in, final OutputStream out, boolean resolveCrossRefs) throws IOException, RDFParseException, RDFHandlerException, MalformedNanopubException {
        final Map<Resource, IRI> tempRefMap;
        if (resolveCrossRefs) {
            tempRefMap = new HashMap<>();
        } else {
            tempRefMap = null;
        }
        try (in; out) {
            MultiNanopubRdfHandler.process(format, in, new NanopubHandler() {

                @Override
                public void handleNanopub(Nanopub np) {
                    try {
                        // TODO temporary URI ref resolution not yet supported here
                        // TODO prefix-based cross-ref resolution also not yet supported
                        writeAsTrustyNanopub(np, format, out, tempRefMap, null);
                    } catch (RDFHandlerException | TrustyUriException ex) {
                        throw new RuntimeException(ex);
                    }
                }

            });
        }
    }

    /**
     * Write a Nanopub as a Trusty Nanopub to the specified output stream in the given RDF format.
     *
     * @param np            the Nanopub to write
     * @param format        the RDF format for the output
     * @param out           the output stream to write to
     * @param tempRefMap    a map for temporary resource references, can be null
     * @param tempPrefixMap a map for temporary prefixes, can be null
     * @return the transformed Trusty Nanopub
     * @throws RDFHandlerException if there is an error writing the RDF data
     * @throws TrustyUriException  if there is an error with Trusty URIs
     */
    public static Nanopub writeAsTrustyNanopub(Nanopub np, RDFFormat format, OutputStream out, Map<Resource, IRI> tempRefMap, Map<String, String> tempPrefixMap) throws RDFHandlerException, TrustyUriException {
        np = MakeTrustyNanopub.transform(np, tempRefMap, tempPrefixMap);
        RDFWriter w = Rio.createWriter(format, new OutputStreamWriter(out, Charset.forName("UTF-8")));
        NanopubUtils.propagateToHandler(np, w);
        return np;
    }

    static void mergeTransformMaps(Map<Resource, IRI> mainMap, Map<Resource, IRI> mapToMerge) {
        if (mainMap == null || mapToMerge == null) return;
        for (Resource r : new HashSet<>(mainMap.keySet())) {
            IRI v = mainMap.get(r);
            if (mapToMerge.containsKey(v)) {
                mainMap.put(r, mapToMerge.get(v));
                mapToMerge.remove(v);
            }
        }
        for (Resource r : mapToMerge.keySet()) {
            mainMap.put(r, mapToMerge.get(r));
        }
    }

    static void mergePrefixTransformMaps(Map<String, String> mainPrefixMap, Map<Resource, IRI> mapToMerge) {
        if (mainPrefixMap == null || mapToMerge == null) return;
        for (Resource r : mapToMerge.keySet()) {
            if (r instanceof IRI && TrustyUriUtils.isPotentialTrustyUri(mapToMerge.get(r).stringValue())) {
                mainPrefixMap.put(r.stringValue(), mapToMerge.get(r).stringValue());
            }
        }
    }

}
