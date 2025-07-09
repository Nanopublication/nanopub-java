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

import jakarta.xml.bind.DatatypeConverter;

import org.apache.commons.io.IOUtils;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.nanopub.MalformedNanopubException;
import org.nanopub.MultiNanopubRdfHandler;
import org.nanopub.MultiNanopubRdfHandler.NanopubHandler;
import org.nanopub.Nanopub;
import org.nanopub.NanopubImpl;
import org.nanopub.NanopubUtils;
import org.nanopub.NanopubWithNs;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

import net.trustyuri.TrustyUriException;
import net.trustyuri.TrustyUriResource;
import net.trustyuri.TrustyUriUtils;

public class LegacySignNanopub {

	@com.beust.jcommander.Parameter(description = "input-nanopubs", required = true)
	private List<File> inputNanopubs = new ArrayList<>();

	@com.beust.jcommander.Parameter(names = "-k", description = "Path and file name of key files")
	private String keyFilename = "~/.nanopub/id_dsa";

	@com.beust.jcommander.Parameter(names = "-v", description = "Verbose")
	private boolean verbose = false;

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

	public static Nanopub signAndTransform(Nanopub nanopub, KeyPair key)
			throws TrustyUriException, InvalidKeyException, SignatureException {
		return signAndTransform(nanopub, key, null);
	}

	public static Nanopub signAndTransform(Nanopub nanopub, KeyPair key, IRI signer)
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
			return LegacySignatureUtils.createSignedNanopub(nanopub, key, signer);
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new RuntimeException(ex);
		}
	}

	public static void signAndTransformMultiNanopub(final RDFFormat format, File file, KeyPair key, final OutputStream out)
			throws IOException, RDFParseException, RDFHandlerException, MalformedNanopubException {
		InputStream in = new FileInputStream(file);
		signAndTransformMultiNanopub(format, in, key, out);
	}

	public static void signAndTransformMultiNanopub(final RDFFormat format, InputStream in, final KeyPair key, final OutputStream out)
			throws IOException, RDFParseException, RDFHandlerException, MalformedNanopubException {
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

	public static Nanopub writeAsSignedTrustyNanopub(Nanopub np, RDFFormat format, KeyPair key, OutputStream out)
			throws RDFHandlerException, TrustyUriException, InvalidKeyException, SignatureException {
		np = signAndTransform(np, key);
		RDFWriter w = Rio.createWriter(format, new OutputStreamWriter(out, Charset.forName("UTF-8")));
		NanopubUtils.propagateToHandler(np, w);
		return np;
	}

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
