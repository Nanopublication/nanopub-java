package org.nanopub.extra.security;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import jakarta.xml.bind.DatatypeConverter;
import net.trustyuri.TrustyUriException;
import net.trustyuri.TrustyUriResource;
import net.trustyuri.TrustyUriUtils;
import org.apache.commons.io.IOUtils;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.rio.*;
import org.nanopub.*;
import org.nanopub.MultiNanopubRdfHandler.NanopubHandler;

import java.io.*;
import java.nio.charset.Charset;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPOutputStream;

/**
 * Legacy command-line tool to sign nanopubs.
 */
public class LegacySignNanopub {

    @com.beust.jcommander.Parameter(description = "input-nanopubs", required = true)
    private List<File> inputNanopubs = new ArrayList<>();

    @com.beust.jcommander.Parameter(names = "-k", description = "Path and file name of key files")
    private String keyFilename = "~/.nanopub/id_dsa";

    @com.beust.jcommander.Parameter(names = "-v", description = "Verbose")
    private boolean verbose = false;

    /**
     * Main method to run the command-line tool.
     *
     * @param args command-line arguments
     * @throws java.io.IOException if an I/O error occurs
     */
    public static void main(String[] args) throws IOException {
        NanopubImpl.ensureLoaded();
        LegacySignNanopub obj = new LegacySignNanopub();
        JCommander jc = new JCommander(obj);
        try {
            jc.parse(args);
        } catch (ParameterException ex) {
            jc.usage();
            System.exit(1);
        }
        try {
            obj.run();
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }

    private KeyPair key;

    private LegacySignNanopub() {
    }

    private void run() throws Exception {
        key = loadKey(keyFilename);
        for (File inputFile : inputNanopubs) {
            File outFile = new File(inputFile.getParent(), "signed." + inputFile.getName());
            final OutputStream out;
            if (inputFile.getName().matches(".*\\.(gz|gzip)")) {
                out = new GZIPOutputStream(new FileOutputStream(outFile));
            } else {
                out = new FileOutputStream(outFile);
            }
            final RDFFormat format = new TrustyUriResource(inputFile).getFormat(RDFFormat.TRIG);
            MultiNanopubRdfHandler.process(format, inputFile, new NanopubHandler() {

                @Override
                public void handleNanopub(Nanopub np) {
                    try {
                        np = writeAsSignedTrustyNanopub(np, format, key, out);
                        if (verbose) {
                            System.out.println("Nanopub URI: " + np.getUri());
                        }
                    } catch (RDFHandlerException | SignatureException | InvalidKeyException | TrustyUriException ex) {
                        ex.printStackTrace();
                        throw new RuntimeException(ex);
                    }
                }

            });
            out.close();
        }
    }

    /**
     * Signs a Nanopub and transforms it into a Trusty URI format.
     *
     * @param nanopub the Nanopub to sign and transform
     * @param key     the KeyPair used for signing
     * @return the signed and transformed Nanopub
     * @throws net.trustyuri.TrustyUriException  if there is an issue with the Trusty URI format
     * @throws java.security.InvalidKeyException if the provided key is invalid
     * @throws java.security.SignatureException  if there is an issue with the signature process
     */
    public static Nanopub signAndTransform(Nanopub nanopub, KeyPair key) throws TrustyUriException, InvalidKeyException, SignatureException {
        return signAndTransform(nanopub, key, null);
    }

    /**
     * Signs a Nanopub and transforms it into a Trusty URI format.
     *
     * @param nanopub the Nanopub to sign and transform
     * @param key     the KeyPair used for signing
     * @param signer  the IRI of the signer
     * @return the signed and transformed Nanopub
     * @throws net.trustyuri.TrustyUriException  if there is an issue with the Trusty URI format
     * @throws java.security.InvalidKeyException if the provided key is invalid
     * @throws java.security.SignatureException  if there is an issue with the signature process
     */
    public static Nanopub signAndTransform(Nanopub nanopub, KeyPair key, IRI signer) throws TrustyUriException, InvalidKeyException, SignatureException {
        if (TrustyUriUtils.getArtifactCode(nanopub.getUri().toString()) != null) {
            throw new SignatureException("Seems to have trusty URI before signing: " + nanopub.getUri());
        }
        if (SignatureUtils.seemsToHaveSignature(nanopub)) {
            throw new SignatureException("Seems to have signature before signing: " + nanopub.getUri());
        }
        if (nanopub instanceof NanopubWithNs) {
            ((NanopubWithNs) nanopub).removeUnusedPrefixes();
        }
        try {
            return LegacySignatureUtils.createSignedNanopub(nanopub, key, signer);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
    }

    /**
     * Signs and transforms multiple Nanopubs from an InputStream into a Trusty URI format.
     *
     * @param format the RDF format of the input Nanopubs
     * @param file   the input file containing multiple Nanopubs
     * @param key    the KeyPair used for signing
     * @param out    the OutputStream to write the signed Nanopubs to
     * @throws java.io.IOException                       if an I/O error occurs
     * @throws org.eclipse.rdf4j.rio.RDFParseException   if there is an error parsing the RDF data
     * @throws org.eclipse.rdf4j.rio.RDFHandlerException if there is an error handling the RDF data
     * @throws org.nanopub.MalformedNanopubException     if a Nanopub is malformed
     */
    public static void signAndTransformMultiNanopub(final RDFFormat format, File file, KeyPair key, final OutputStream out) throws IOException, RDFParseException, RDFHandlerException, MalformedNanopubException {
        InputStream in = new FileInputStream(file);
        signAndTransformMultiNanopub(format, in, key, out);
    }

    /**
     * Signs and transforms multiple Nanopubs from an InputStream into a Trusty URI format.
     *
     * @param format the RDF format of the input Nanopubs
     * @param in     the InputStream containing multiple Nanopubs
     * @param key    the KeyPair used for signing
     * @param out    the OutputStream to write the signed Nanopubs to
     * @throws java.io.IOException                       if an I/O error occurs
     * @throws org.eclipse.rdf4j.rio.RDFParseException   if there is an error parsing the RDF data
     * @throws org.eclipse.rdf4j.rio.RDFHandlerException if there is an error handling the RDF data
     * @throws org.nanopub.MalformedNanopubException     if a Nanopub is malformed
     */
    public static void signAndTransformMultiNanopub(final RDFFormat format, InputStream in, final KeyPair key, final OutputStream out) throws IOException, RDFParseException, RDFHandlerException, MalformedNanopubException {
        MultiNanopubRdfHandler.process(format, in, new NanopubHandler() {

            @Override
            public void handleNanopub(Nanopub np) {
                try {
                    writeAsSignedTrustyNanopub(np, format, key, out);
                } catch (RDFHandlerException | SignatureException | InvalidKeyException | TrustyUriException ex) {
                    ex.printStackTrace();
                    throw new RuntimeException(ex);
                }
            }

        });
        out.close();
    }

    /**
     * Writes a signed Nanopub in Trusty URI format to an OutputStream.
     *
     * @param np     the Nanopub to write
     * @param format the RDF format to use for writing
     * @param key    the KeyPair used for signing
     * @param out    the OutputStream to write the signed Nanopub to
     * @return the signed Nanopub
     * @throws org.eclipse.rdf4j.rio.RDFHandlerException if there is an error handling the RDF data
     * @throws net.trustyuri.TrustyUriException          if there is an issue with the Trusty URI format
     * @throws java.security.InvalidKeyException         if the provided key is invalid
     * @throws java.security.SignatureException          if there is an issue with the signature process
     */
    public static Nanopub writeAsSignedTrustyNanopub(Nanopub np, RDFFormat format, KeyPair key, OutputStream out) throws RDFHandlerException, TrustyUriException, InvalidKeyException, SignatureException {
        np = signAndTransform(np, key);
        RDFWriter w = Rio.createWriter(format, new OutputStreamWriter(out, Charset.forName("UTF-8")));
        NanopubUtils.propagateToHandler(np, w);
        return np;
    }

    /**
     * Loads a KeyPair from a specified key file.
     *
     * @param keyFilename the path to the key file
     * @return the loaded KeyPair
     * @throws java.security.NoSuchAlgorithmException     if the DSA algorithm is not available
     * @throws java.io.IOException                        if an I/O error occurs while reading the key file
     * @throws java.security.spec.InvalidKeySpecException if the key specification is invalid
     */
    public static KeyPair loadKey(String keyFilename) throws NoSuchAlgorithmException, IOException, InvalidKeySpecException {
        keyFilename = SignatureUtils.getFullFilePath(keyFilename);
        KeyFactory kf = KeyFactory.getInstance("DSA");
        byte[] privateKeyBytes = DatatypeConverter.parseBase64Binary(IOUtils.toString(new FileInputStream(keyFilename), "UTF-8"));
        KeySpec privateSpec = new PKCS8EncodedKeySpec(privateKeyBytes);
        PrivateKey privateKey = kf.generatePrivate(privateSpec);
        byte[] publicKeyBytes = DatatypeConverter.parseBase64Binary(IOUtils.toString(new FileInputStream(keyFilename + ".pub"), "UTF-8"));
        KeySpec publicSpec = new X509EncodedKeySpec(publicKeyBytes);
        PublicKey publicKey = kf.generatePublic(publicSpec);
        return new KeyPair(publicKey, privateKey);
    }

}
