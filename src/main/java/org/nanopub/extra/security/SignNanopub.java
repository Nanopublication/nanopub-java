package org.nanopub.extra.security;

import com.beust.jcommander.ParameterException;
import net.trustyuri.TrustyUriException;
import net.trustyuri.TrustyUriResource;
import org.apache.commons.io.IOUtils;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.rio.*;
import org.nanopub.*;
import org.nanopub.MultiNanopubRdfHandler.NanopubHandler;

import jakarta.xml.bind.DatatypeConverter;
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

public class SignNanopub extends CliRunner {

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

	@com.beust.jcommander.Parameter(names = "-s", description = "The orcid IRI of the signer")
	private String signer;

	@com.beust.jcommander.Parameter(names = "--profile", description = "Profile file for signer iri and key files, " +
			"defaults to ~/.nanopub/profile.yaml")
	private File profileFile;


	private SignatureAlgorithm algorithm; // we guess the algorithm is RSA as long as the key name does not end in _dsa

	private ValueFactory vf = SimpleValueFactory.getInstance();

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

	public SignNanopub() {
    }

	protected void run() throws Exception {
		NanopubProfile profile;
		if (profileFile != null) {
			profile = new NanopubProfile(profileFile.getPath());
		} else {
			profile = new NanopubProfile(NanopubProfile.IMPLICIT_PROFILE_FILE_NAME);
		}
		if (keyFilename == null) {
			keyFilename = profile.getPrivateKeyPath();
		}
		if (keyFilename == null) {
			keyFilename = "~/.nanopub/id_rsa";
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
				MultiNanopubRdfHandler.process(inFormat, inputFile, new NanopubHandler() {
	
					@Override
					public void handleNanopub(Nanopub np) {
						try {
							np = writeAsSignedTrustyNanopub(np, outFormat, c, out);
							if (verbose) {
								System.out.println("Nanopub URI: " + np.getUri());
							}
						} catch (RDFHandlerException | SignatureException | InvalidKeyException | TrustyUriException ex) {
							ex.printStackTrace();
							throw new RuntimeException(ex);
						}
                    }
	
				});
			}
		}
	}

	public static Nanopub signAndTransform(Nanopub nanopub, TransformContext c)
			throws TrustyUriException, InvalidKeyException, SignatureException {
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
			return SignatureUtils.createSignedNanopub(nanopub, c);
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new RuntimeException(ex);
		}
	}

	public static void signAndTransformMultiNanopub(final RDFFormat format, File file, TransformContext c, OutputStream out)
			throws IOException, RDFParseException, RDFHandlerException, MalformedNanopubException {
		InputStream in = new FileInputStream(file);
		signAndTransformMultiNanopub(format, in, c, out);
	}

	public static void signAndTransformMultiNanopub(final RDFFormat format, InputStream in, final TransformContext c, final OutputStream out)
			throws IOException, RDFParseException, RDFHandlerException, MalformedNanopubException {
		try (out) {
			MultiNanopubRdfHandler.process(format, in, new NanopubHandler() {
	
				@Override
				public void handleNanopub(Nanopub np) {
					try {
						writeAsSignedTrustyNanopub(np, format, c, out);
					} catch (RDFHandlerException | SignatureException | InvalidKeyException | TrustyUriException ex) {
						ex.printStackTrace();
						throw new RuntimeException(ex);
					}
                }
	
			});
		}
	}

	public static Nanopub writeAsSignedTrustyNanopub(Nanopub np, RDFFormat format, TransformContext c, OutputStream out)
			throws RDFHandlerException, TrustyUriException, InvalidKeyException, SignatureException {
		np = signAndTransform(np, c);
		RDFWriter w = Rio.createWriter(format, new OutputStreamWriter(out, Charset.forName("UTF-8")));
		NanopubUtils.propagateToHandler(np, w);
		return np;
	}

	public static KeyPair loadKey(String keyFilename, SignatureAlgorithm algorithm) throws NoSuchAlgorithmException, IOException, InvalidKeySpecException {
		keyFilename = SignatureUtils.getFullFilePath(keyFilename);
		KeyFactory kf = KeyFactory.getInstance(algorithm.name());
		byte[] privateKeyBytes = DatatypeConverter.parseBase64Binary(IOUtils.toString(new FileInputStream(keyFilename), "UTF-8"));
		KeySpec privateSpec = new PKCS8EncodedKeySpec(privateKeyBytes);
		PrivateKey privateKey = kf.generatePrivate(privateSpec);
		byte[] publicKeyBytes = DatatypeConverter.parseBase64Binary(IOUtils.toString(new FileInputStream(keyFilename + ".pub"), "UTF-8"));
		KeySpec publicSpec = new X509EncodedKeySpec(publicKeyBytes);
		PublicKey publicKey = kf.generatePublic(publicSpec);
		return new KeyPair(publicKey, privateKey);
	}

}
