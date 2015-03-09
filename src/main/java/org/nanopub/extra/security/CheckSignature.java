package org.nanopub.extra.security;

import java.io.File;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.List;

import net.trustyuri.TrustyUriResource;
import net.trustyuri.TrustyUriUtils;
import net.trustyuri.rdf.RdfHasher;
import net.trustyuri.rdf.RdfPreprocessor;

import org.nanopub.MultiNanopubRdfHandler;
import org.nanopub.MultiNanopubRdfHandler.NanopubHandler;
import org.nanopub.Nanopub;
import org.nanopub.NanopubUtils;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.helpers.RDFHandlerBase;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

public class CheckSignature {

	@com.beust.jcommander.Parameter(description = "input-nanopubs", required = true)
	private List<File> inputNanopubs = new ArrayList<File>();

	public static void main(String[] args) throws IOException {
		CheckSignature obj = new CheckSignature();
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

	private CheckSignature() {
	}

	private void run() throws Exception {
		for (File inputFile : inputNanopubs) {
			final RDFFormat format = new TrustyUriResource(inputFile).getFormat(RDFFormat.TRIG);
			MultiNanopubRdfHandler.process(format, inputFile, new NanopubHandler() {

				@Override
				public void handleNanopub(Nanopub np) {
					boolean npIsGood = false;
					if (!hasSignature(np)) {
						System.out.println("Nanopub has no signature");
						return;
					}
					try {
						npIsGood = hasValidSignatures(np);
					} catch (Exception ex) {
						System.out.println("Error checking signatures");
						ex.printStackTrace();
					}
					if (npIsGood) {
						System.out.println("Success. Nanopub is signed with valid signature(s)");
					} else {
						System.out.println("SIGNATURE IS NOT VALID");
					}
				}

			});
		}
	}

	public static boolean hasSignature(Nanopub nanopub) {
		for (Statement st : nanopub.getPubinfo()) {
			if (st.getPredicate().equals(NanopubSignature.HAS_SIGNATURE_ELEMENT)) return true;
		}
		return false;
	}

	public static boolean hasValidSignatures(Nanopub nanopub) {
		String artifactCode = TrustyUriUtils.getArtifactCode(nanopub.getUri().toString());
		if (artifactCode == null) return false;
		List<Statement> statements = NanopubUtils.getStatements(nanopub);
		statements = RdfPreprocessor.run(statements, artifactCode);
		String ac = RdfHasher.makeArtifactCode(statements);
		boolean hasValidTrustyUri = ac.equals(artifactCode);
		if (!hasValidTrustyUri) return false;

		// Remove signature and get public key
		URI pubinfoUri = new URIImpl(nanopub.getPubinfoUri().toString().replace(ac, " "));
		final List<Statement> statementsWithoutSig = new ArrayList<>();
		SignatureRemover sigRemover = new SignatureRemover(new RDFHandlerBase() {

			@Override
			public void handleStatement(Statement st) throws RDFHandlerException {
				statementsWithoutSig.add(st);
			}

		}, pubinfoUri);
		try {
			sigRemover.startRDF();
			for (Statement st : statements) {
				sigRemover.handleStatement(st);
			}
			sigRemover.endRDF();
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new RuntimeException(ex);
		}

		MessageDigest digest = RdfHasher.digest(statementsWithoutSig);

		if (sigRemover.getSignatureElements().isEmpty()) {
			return false;
		}

		// Check signatures
		for (Resource sigElement : sigRemover.getSignatureElements()) {
			byte[] signature = sigRemover.getSignature(sigElement);
			PublicKey publicKey = sigRemover.getPublicKey(sigElement);
			Signature dsa;
			boolean signatureIsGood;
			try {
				dsa = Signature.getInstance("SHA1withDSA", "SUN");
				dsa.initVerify(publicKey);
				dsa.update(digest.digest());
				signatureIsGood = dsa.verify(signature);
			} catch (NoSuchAlgorithmException ex) {
				throw new RuntimeException(ex);
			} catch (NoSuchProviderException ex) {
				throw new RuntimeException(ex);
			} catch (InvalidKeyException ex) {
				throw new RuntimeException(ex);
			} catch (SignatureException ex) {
				throw new RuntimeException(ex);
			}
			if (!signatureIsGood) {
				return false;
			}
		}
		return true;
	}

}
