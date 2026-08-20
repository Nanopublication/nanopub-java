package org.nanopub.extra.security;

import com.beust.jcommander.ParameterException;
import jakarta.xml.bind.DatatypeConverter;
import net.trustyuri.TrustyUriException;
import net.trustyuri.TrustyUriResource;
import org.apache.commons.io.IOUtils;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.rio.*;
import org.nanopub.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;

/**
 * Command line tool to sign nanopubs with a private key.
 */
public class SignNanopub extends CliRunner {

    private static final Logger logger = LoggerFactory.getLogger(SignNanopub.class);

    @com.beust.jcommander.Parameter(description = "input-nanopub-files", required = true)
    private List<File> inputNanopubFiles = new ArrayList<>();

    @com.beust.jcommander.Parameter(names = "-o", description = "Output file")
    private File singleOutputFile;

    @com.beust.jcommander.Parameter(names = "-k", description = "Path and file name of key files")
    private String keyFilename;

    @com.beust.jcommander.Parameter(names = "-i", description = "Ignore already signed nanopubs")
    private boolean ignoreSigned = false;

    @com.beust.jcommander.Parameter(names = "-v", description = "Verbose")
    private boolean verbose = false;

    @com.beust.jcommander.Parameter(names = "-r", description = "Resolve cross-nanopub references")
    private boolean resolveCrossRefs = false;

    @com.beust.jcommander.Parameter(names = "-R", description = "Resolve cross-nanopub references based on prefixes")
    private boolean resolveCrossRefsPrefixBased = false;

    @com.beust.jcommander.Parameter(names = "-s", description = "The IRI of the signer, typically an ORCID IRI. It can also be a sub-IRI of the nanopub being signed, given under its temporary URI (e.g. http://purl.org/nanopub/temp/np001/my-bot), which lets an agent self-sign its own introduction")
    private String signer;

    @com.beust.jcommander.Parameter(names = "--profile", description = "Profile file for signer iri and key files, " + "defaults to ~/.nanopub/profile.yaml")
    private File profileFile;


    private SignatureAlgorithm algorithm; // we guess the algorithm is RSA as long as the key name does not end in _dsa

    private ValueFactory vf = SimpleValueFactory.getInstance();

    /**
     * Main method to run the SignNanopub command line tool.
     *
     * @param args command line arguments
     * @throws java.io.IOException if an I/O error occurs
     */
    public static void main(String[] args) throws IOException {
        try {
            SignNanopub obj = CliRunner.initJc(new SignNanopub(), args);
            obj.run();
        } catch (ParameterException ex) {
            System.exit(1);
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }

    private KeyPair key;

    /**
     * Default constructor for SignNanopub.
     * Initializes the command line parameters.
     */
    public SignNanopub() {
    }

    /**
     * Runs the signing process for the nanopubs.
     *
     * @throws java.lang.Exception if an error occurs during signing
     */
    protected void run() throws Exception {
        NanopubProfile profile;
        if (profileFile != null) {
            profile = new NanopubProfile(profileFile.getPath());
        } else {
            profile = new NanopubProfile(NanopubProfile.IMPLICIT_PROFILE_FILE_NAME);
        }
        if (keyFilename == null) {
            keyFilename = profile.getPrivateKeyPath() != null ? profile.getPrivateKeyPath() : TransformContext.DEFAULT_KEY_PATH;
        }

        if (keyFilename.endsWith("_dsa")) {
            algorithm = SignatureAlgorithm.DSA;
        } else {
            // Assuming RSA if not other information is available
            algorithm = SignatureAlgorithm.RSA;
        }

        key = loadKey(keyFilename, algorithm);
        IRI signerIri = null;
        if (signer != null) {
            signerIri = vf.createIRI(signer);
        } else if (profile.getOrcidId() != null) {
            signerIri = vf.createIRI(profile.getOrcidId());
        } else {
            String msg = "No valid signer specified. Use either: -s or --profile !";
            throw new Exception(msg);
        }
        final TransformContext c = new TransformContext(algorithm, key, signerIri, resolveCrossRefs, resolveCrossRefsPrefixBased, ignoreSigned);

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
                outputFile = new File(inputFile.getParent(), "signed." + inputFile.getName());
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
                MultiNanopubRdfHandler.process(inFormat, inputFile, np -> {
                    try {
                        np = writeAsSignedTrustyNanopub(np, outFormat, c, out);
                        if (verbose) {
                            System.out.println("Nanopub URI: " + np.getUri());
                        }
                    } catch (RDFHandlerException | SignatureException | InvalidKeyException |
                             TrustyUriException ex) {
                        ex.printStackTrace();
                        throw new RuntimeException(ex);
                    }
                });
            }
        }
    }

    /**
     * Signs and transforms a nanopub.
     *
     * @param nanopub the nanopub to sign
     * @param c       the transform context containing signing information
     * @return the signed and transformed nanopub
     * @throws net.trustyuri.TrustyUriException  if there is an error with the Trusty URI
     * @throws java.security.InvalidKeyException if the key is invalid
     * @throws java.security.SignatureException  if there is an error during signing
     */
    public static Nanopub signAndTransform(Nanopub nanopub, TransformContext c) throws TrustyUriException, InvalidKeyException, SignatureException {
        if (nanopub instanceof NanopubWithNs) {
            ((NanopubWithNs) nanopub).removeUnusedPrefixes();
        }
        if (SignatureUtils.seemsToHaveSignature(nanopub)) {
            if (c.isIgnoreSignedEnabled()) {
                return nanopub;
            } else {
                throw new SignatureException("Seems to have signature before signing: " + nanopub.getUri());
            }
        }
        try {
            Nanopub signed = SignatureUtils.createSignedNanopub(nanopub, c);
            logger.debug("Signed nanopub {} as {}", nanopub.getUri(), signed.getUri());
            return signed;
        } catch (MalformedNanopubException ex) {
            // the nanopub is not fit to be signed; the caller gets the reason rather than a runtime error
            throw new SignatureException("Could not sign nanopub " + nanopub.getUri() + ": " + ex.getMessage(), ex);
        } catch (Exception ex) {
            // Not logged here: the cause travels with the exception and is the caller's to report.
            throw new RuntimeException("Could not sign nanopub " + nanopub.getUri(), ex);
        }
    }

    /**
     * Signs and transforms multiple nanopubs from a file.
     *
     * @param format the RDF format of the nanopubs
     * @param file   the input file containing nanopubs
     * @param c      the transform context containing signing information
     * @param out    the output stream to write signed nanopubs
     * @throws java.io.IOException                       if an I/O error occurs
     * @throws org.eclipse.rdf4j.rio.RDFParseException   if there is an error parsing RDF
     * @throws org.eclipse.rdf4j.rio.RDFHandlerException if there is an error handling RDF
     * @throws org.nanopub.MalformedNanopubException     if a nanopub is malformed
     */
    public static void signAndTransformMultiNanopub(final RDFFormat format, File file, TransformContext c, OutputStream out) throws IOException, RDFParseException, RDFHandlerException, MalformedNanopubException {
        InputStream in = new FileInputStream(file);
        signAndTransformMultiNanopub(format, in, c, out);
    }

    /**
     * Signs and transforms multiple nanopubs from an input stream.
     *
     * @param format the RDF format of the nanopubs
     * @param in     the input stream containing nanopubs
     * @param c      the transform context containing signing information
     * @param out    the output stream to write signed nanopubs
     * @throws java.io.IOException                       if an I/O error occurs
     * @throws org.eclipse.rdf4j.rio.RDFParseException   if there is an error parsing RDF
     * @throws org.eclipse.rdf4j.rio.RDFHandlerException if there is an error handling RDF
     * @throws org.nanopub.MalformedNanopubException     if a nanopub is malformed
     */
    public static void signAndTransformMultiNanopub(final RDFFormat format, InputStream in, final TransformContext c, final OutputStream out) throws IOException, RDFParseException, RDFHandlerException, MalformedNanopubException {
        try (out) {
            MultiNanopubRdfHandler.process(format, in, np -> {
                try {
                    writeAsSignedTrustyNanopub(np, format, c, out);
                } catch (RDFHandlerException | SignatureException | InvalidKeyException | TrustyUriException ex) {
                    ex.printStackTrace();
                    throw new RuntimeException(ex);
                }
            });
        }
    }

    /**
     * Writes a signed nanopub to an output stream in the specified RDF format.
     *
     * @param np     the nanopub to write
     * @param format the RDF format to use for writing
     * @param c      the transform context containing signing information
     * @param out    the output stream to write the signed nanopub
     * @return the signed nanopub
     * @throws org.eclipse.rdf4j.rio.RDFHandlerException if there is an error handling RDF
     * @throws net.trustyuri.TrustyUriException          if there is an error with the Trusty URI
     * @throws java.security.InvalidKeyException         if the key is invalid
     * @throws java.security.SignatureException          if there is an error during signing
     */
    public static Nanopub writeAsSignedTrustyNanopub(Nanopub np, RDFFormat format, TransformContext c, OutputStream out) throws RDFHandlerException, TrustyUriException, InvalidKeyException, SignatureException {
        np = signAndTransform(np, c);
        RDFWriter w = Rio.createWriter(format, new OutputStreamWriter(out, StandardCharsets.UTF_8));
        NanopubUtils.propagateToHandler(np, w);
        return np;
    }

    /**
     * Loads a key pair from the specified key file.
     * <p>
     * The private key is read from the given file and the public key from the same file name with the
     * suffix {@code .pub}. Both are expected to be base64-encoded, the private one in PKCS#8 and the
     * public one in X.509/SubjectPublicKeyInfo format. PEM header and footer lines (such as
     * {@code -----BEGIN PRIVATE KEY-----}) and line breaks are accepted and ignored, so plain PEM files
     * can be used as they are produced by tools like OpenSSL.
     *
     * @param keyFilename the path to the key file
     * @param algorithm   the signature algorithm used for the key
     * @return the loaded KeyPair
     * @throws java.security.NoSuchAlgorithmException     if the specified algorithm is not available
     * @throws java.io.IOException                        if an I/O error occurs while reading the key file
     * @throws java.security.spec.InvalidKeySpecException if the key specification is invalid
     */
    public static KeyPair loadKey(String keyFilename, SignatureAlgorithm algorithm) throws NoSuchAlgorithmException, IOException, InvalidKeySpecException {
        keyFilename = SignatureUtils.getFullFilePath(keyFilename);
        KeyFactory kf = KeyFactory.getInstance(algorithm.name());
        String publicKeyFilename = keyFilename + ".pub";
        String privateKeyString = readKeyFile(keyFilename);
        String publicKeyString = readKeyFile(publicKeyFilename);
        PrivateKey privateKey;
        try {
            KeySpec privateSpec = new PKCS8EncodedKeySpec(decodeKey(privateKeyString));
            privateKey = kf.generatePrivate(privateSpec);
        } catch (IllegalArgumentException | InvalidKeySpecException ex) {
            throw new InvalidKeySpecException(getKeyErrorMessage(keyFilename, privateKeyString, true), ex);
        }
        PublicKey publicKey;
        try {
            KeySpec publicSpec = new X509EncodedKeySpec(decodeKey(publicKeyString));
            publicKey = kf.generatePublic(publicSpec);
        } catch (IllegalArgumentException | InvalidKeySpecException ex) {
            throw new InvalidKeySpecException(getKeyErrorMessage(publicKeyFilename, publicKeyString, false), ex);
        }
        return new KeyPair(publicKey, privateKey);
    }

    private static final Pattern pemArmorPattern = Pattern.compile("-----(BEGIN|END)[^-]*-----");

    private static String readKeyFile(String keyFilename) throws IOException {
        try (InputStream in = new FileInputStream(keyFilename)) {
            return IOUtils.toString(in, StandardCharsets.UTF_8);
        }
    }

    /**
     * Decodes a base64-encoded key, ignoring PEM header/footer lines and line breaks if present.
     */
    private static byte[] decodeKey(String keyString) {
        return DatatypeConverter.parseBase64Binary(pemArmorPattern.matcher(keyString).replaceAll(""));
    }

    private static String getKeyErrorMessage(String keyFilename, String keyString, boolean isPrivateKey) {
        String hint;
        if (keyString.contains("BEGIN ENCRYPTED PRIVATE KEY")) {
            hint = "The key seems to be protected by a passphrase, which is not supported. Decrypt it first and " +
                    "use the decrypted key, e.g. with: openssl pkcs8 -topk8 -nocrypt -in " + keyFilename + " -out " + keyFilename + ".pkcs8";
        } else if (keyString.contains("BEGIN RSA PRIVATE KEY")) {
            hint = "The key seems to be in PKCS#1 format, which is not supported. Convert it to PKCS#8 and " +
                    "use the converted key, e.g. with: openssl pkcs8 -topk8 -nocrypt -in " + keyFilename + " -out " + keyFilename + ".pkcs8";
        } else if (keyString.contains("BEGIN RSA PUBLIC KEY")) {
            hint = "The key seems to be in PKCS#1 format, which is not supported. Convert it to X.509 and " +
                    "use the converted key, e.g. with: openssl rsa -RSAPublicKey_in -in " + keyFilename + " -pubout -out " + keyFilename + ".x509";
        } else if (keyString.contains("BEGIN OPENSSH PRIVATE KEY") || keyString.startsWith("ssh-")) {
            hint = "The key seems to be in OpenSSH format, which is not supported. Convert it to PKCS#8, " +
                    "e.g. with: ssh-keygen -p -m PKCS8 -f " + keyFilename;
        } else {
            hint = "Expected a base64-encoded " + (isPrivateKey ? "PKCS#8 private key" : "X.509 public key") +
                    ", with or without PEM header/footer lines.";
        }
        return "Could not read the " + (isPrivateKey ? "private" : "public") + " key from file " + keyFilename + ". " + hint;
    }

}
