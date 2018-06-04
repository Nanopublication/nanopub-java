package org.nanopub.extra.security;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import javax.xml.bind.DatatypeConverter;

import org.apache.commons.io.IOUtils;
import org.nanopub.MalformedNanopubException;
import org.nanopub.MultiNanopubRdfHandler;
import org.nanopub.MultiNanopubRdfHandler.NanopubHandler;
import org.nanopub.Nanopub;
import org.nanopub.NanopubImpl;
import org.nanopub.NanopubUtils;
import org.nanopub.NanopubWithNs;
import org.openrdf.model.URI;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.RDFWriter;
import org.openrdf.rio.Rio;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

import net.trustyuri.TrustyUriException;
import net.trustyuri.TrustyUriResource;
import net.trustyuri.TrustyUriUtils;

public class SignNanopub {

	@com.beust.jcommander.Parameter(description = "input-nanopubs", required = true)
	private List<File> inputNanopubs = new ArrayList<File>();

	@com.beust.jcommander.Parameter(names = "-k", description = "Path and file name of key files")
	private String keyFilename;

	@com.beust.jcommander.Parameter(names = "-a", description = "Signature algorithm: either RSA or DSA")
	private SignatureAlgorithm algorithm;

	@com.beust.jcommander.Parameter(names = "-v", description = "Verbose")
	private boolean verbose = false;

	public static void main(String[] args) throws IOException {
		NanopubImpl.ensureLoaded();
		SignNanopub obj = new SignNanopub();
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

	private SignNanopub() {
	}

	private void run() throws Exception {
		if (algorithm == null) {
			if (keyFilename == null) {
				keyFilename = "~/.nanopub/id_rsa";
				algorithm = SignatureAlgorithm.RSA;
			} else if (keyFilename.endsWith("_rsa")) {
				algorithm = SignatureAlgorithm.RSA;
			} else if (keyFilename.endsWith("_dsa")) {
				algorithm = SignatureAlgorithm.DSA;
			} else {
				// Assuming RSA if not other information is available
				algorithm = SignatureAlgorithm.RSA;
			}
		} else if (keyFilename == null) {
			keyFilename = "~/.nanopub/id_" + algorithm.name().toLowerCase();
		}
		key = loadKey(keyFilename, algorithm);
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
						np = writeAsSignedTrustyNanopub(np, format, algorithm, key, out);
						if (verbose) {
							System.out.println("Nanopub URI: " + np.getUri());
						}
					} catch (RDFHandlerException ex) {
						ex.printStackTrace();
						throw new RuntimeException(ex);
					} catch (TrustyUriException ex) {
						ex.printStackTrace();
						throw new RuntimeException(ex);
					} catch (InvalidKeyException ex) {
						ex.printStackTrace();
						throw new RuntimeException(ex);
					} catch (SignatureException ex) {
						ex.printStackTrace();
						throw new RuntimeException(ex);
					}
				}

			});
			out.close();
		}
	}

	public static Nanopub signAndTransform(Nanopub nanopub, SignatureAlgorithm algorithm, KeyPair key)
			throws TrustyUriException, InvalidKeyException, SignatureException {
		return signAndTransform(nanopub, algorithm, key, null);
	}

	public static Nanopub signAndTransform(Nanopub nanopub, SignatureAlgorithm algorithm, KeyPair key, URI signer)
			throws TrustyUriException, InvalidKeyException, SignatureException {
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
			return SignatureUtils.createSignedNanopub(nanopub, algorithm, key, signer);
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new RuntimeException(ex);
		}
	}

	public static void signAndTransformMultiNanopub(final RDFFormat format, File file, SignatureAlgorithm algorithm,
				KeyPair key, OutputStream out)
			throws IOException, RDFParseException, RDFHandlerException, MalformedNanopubException {
		InputStream in = new FileInputStream(file);
		signAndTransformMultiNanopub(format, in, algorithm, key, out);
	}

	public static void signAndTransformMultiNanopub(final RDFFormat format, InputStream in, final SignatureAlgorithm algorithm,
				final KeyPair key, final OutputStream out)
			throws IOException, RDFParseException, RDFHandlerException, MalformedNanopubException {
		MultiNanopubRdfHandler.process(format, in, new NanopubHandler() {

			@Override
			public void handleNanopub(Nanopub np) {
				try {
					writeAsSignedTrustyNanopub(np, format, algorithm, key, out);
				} catch (RDFHandlerException ex) {
					ex.printStackTrace();
					throw new RuntimeException(ex);
				} catch (TrustyUriException ex) {
					ex.printStackTrace();
					throw new RuntimeException(ex);
				} catch (InvalidKeyException ex) {
					ex.printStackTrace();
					throw new RuntimeException(ex);
				} catch (SignatureException ex) {
					ex.printStackTrace();
					throw new RuntimeException(ex);
				}
			}

		});
		out.close();
	}

	public static Nanopub writeAsSignedTrustyNanopub(Nanopub np, RDFFormat format, SignatureAlgorithm algorithm, KeyPair key, OutputStream out)
			throws RDFHandlerException, TrustyUriException, InvalidKeyException, SignatureException {
		np = signAndTransform(np, algorithm, key);
		RDFWriter w = Rio.createWriter(format, new OutputStreamWriter(out, Charset.forName("UTF-8")));
		NanopubUtils.propagateToHandler(np, w);
		return np;
	}

	public static KeyPair loadKey(String keyFilename, SignatureAlgorithm algorithm) throws NoSuchAlgorithmException, IOException, InvalidKeySpecException {
		keyFilename = keyFilename.replaceFirst("^~", System.getProperty("user.home"));
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
