package org.nanopub.extra.security;

import static org.nanopub.extra.security.NanopubSignature.HAS_PUBLIC_KEY;
import static org.nanopub.extra.security.NanopubSignature.HAS_SIGNATURE;
import static org.nanopub.extra.security.NanopubSignature.HAS_SIGNATURE_ELEMENT;
import static org.nanopub.extra.security.NanopubSignature.SIGNED_BY;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import javax.xml.bind.DatatypeConverter;

import net.trustyuri.TrustyUriException;
import net.trustyuri.TrustyUriResource;
import net.trustyuri.TrustyUriUtils;
import net.trustyuri.rdf.RdfFileContent;
import net.trustyuri.rdf.RdfHasher;
import net.trustyuri.rdf.RdfPreprocessor;
import net.trustyuri.rdf.TransformRdf;

import org.apache.commons.io.IOUtils;
import org.nanopub.MalformedNanopubException;
import org.nanopub.MultiNanopubRdfHandler;
import org.nanopub.MultiNanopubRdfHandler.NanopubHandler;
import org.nanopub.Nanopub;
import org.nanopub.NanopubImpl;
import org.nanopub.NanopubRdfHandler;
import org.nanopub.NanopubUtils;
import org.nanopub.NanopubWithNs;
import org.openrdf.model.Literal;
import org.openrdf.model.URI;
import org.openrdf.model.impl.ContextStatementImpl;
import org.openrdf.model.impl.LiteralImpl;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.RDFWriter;
import org.openrdf.rio.Rio;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

public class SignNanopub {

	@com.beust.jcommander.Parameter(description = "input-nanopubs", required = true)
	private List<File> inputNanopubs = new ArrayList<File>();

	@com.beust.jcommander.Parameter(names = "-k", description = "Path and file name of key files")
	private String keyFilename = "~/.nanopub/id_dsa";

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

	public static Nanopub signAndTransform(Nanopub nanopub, KeyPair key)
			throws TrustyUriException, InvalidKeyException, SignatureException {
		return signAndTransform(nanopub, key, null);
	}

	public static Nanopub signAndTransform(Nanopub nanopub, KeyPair key, URI signer)
			throws TrustyUriException, InvalidKeyException, SignatureException {
		Nanopub np;
		Signature dsa;
		try {
			dsa = Signature.getInstance("SHA1withDSA", "SUN");
			dsa.initSign(key.getPrivate());
		} catch (NoSuchAlgorithmException ex) {
			throw new RuntimeException(ex);
		} catch (NoSuchProviderException ex) {
			throw new RuntimeException(ex);
		}
		if (nanopub instanceof NanopubWithNs) {
			((NanopubWithNs) nanopub).removeUnusedPrefixes();
		}
		try {
			// TODO improve the code below...
			RdfFileContent content = new RdfFileContent(RDFFormat.TRIG);
			NanopubUtils.propagateToHandler(nanopub, content);
			content = RdfPreprocessor.run(content, nanopub.getUri());

			RdfFileContent contentWithoutSignature = new RdfFileContent(RDFFormat.TRIG);
			content.propagate(new SignatureRemover(contentWithoutSignature, nanopub.getPubinfoUri()));
			MessageDigest digest = RdfHasher.digest(contentWithoutSignature.getStatements());
			dsa.update(digest.digest());
			byte[] signatureBytes = dsa.sign();
			String signature = DatatypeConverter.printBase64Binary(signatureBytes);
			String signatureShort = TrustyUriUtils.getBase64(signatureBytes).substring(0, 16);

			RdfFileContent signatureContent = new RdfFileContent(RDFFormat.TRIG);
			URI signatureElUri = new URIImpl(nanopub.getUri() + "signature." + signatureShort);
			signatureContent.startRDF();
			signatureContent.handleNamespace("npx", "http://purl.org/nanopub/x/");
			URI npUri = nanopub.getUri();
			URI piUri = nanopub.getPubinfoUri();
			signatureContent.handleStatement(new ContextStatementImpl(npUri, HAS_SIGNATURE_ELEMENT, signatureElUri, piUri));
			String publicKeyString = DatatypeConverter.printBase64Binary(key.getPublic().getEncoded()).replaceAll("\\s", "");
			Literal publicKeyLiteral = new LiteralImpl(publicKeyString);
			signatureContent.handleStatement(new ContextStatementImpl(signatureElUri, HAS_PUBLIC_KEY, publicKeyLiteral, piUri));
			Literal signatureLiteral = new LiteralImpl(signature);
			signatureContent.handleStatement(new ContextStatementImpl(signatureElUri, HAS_SIGNATURE, signatureLiteral, piUri));
			if (signer != null) {
				signatureContent.handleStatement(new ContextStatementImpl(signatureElUri, SIGNED_BY, signer, piUri));
			}
			signatureContent.endRDF();
			signatureContent = RdfPreprocessor.run(signatureContent, nanopub.getUri());

			RdfFileContent signedContent = new RdfFileContent(RDFFormat.TRIG);
			signedContent.startRDF();
			content.propagate(signedContent, false);
			signatureContent.propagate(signedContent, false);
			signedContent.endRDF();
			NanopubRdfHandler nanopubHandler = new NanopubRdfHandler();
			TransformRdf.transformPreprocessed(signedContent, nanopub.getUri(), nanopubHandler);
			np = nanopubHandler.getNanopub();
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new RuntimeException(ex);
		}
		return np;
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

	public static Nanopub writeAsSignedTrustyNanopub(Nanopub np, RDFFormat format, KeyPair key, OutputStream out)
			throws RDFHandlerException, TrustyUriException, InvalidKeyException, SignatureException {
		np = signAndTransform(np, key);
		RDFWriter w = Rio.createWriter(format, out);
		NanopubUtils.propagateToHandler(np, w);
		return np;
	}

	public static KeyPair loadKey(String keyFilename) throws NoSuchAlgorithmException, IOException, InvalidKeySpecException {
		keyFilename = keyFilename.replaceFirst("^~", System.getProperty("user.home"));
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
