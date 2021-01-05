package org.nanopub.extra.security;

import static org.nanopub.extra.security.NanopubSignatureElement.HAS_SIGNATURE;
import static org.nanopub.extra.security.NanopubSignatureElement.HAS_SIGNATURE_TARGET;
import static org.nanopub.extra.security.NanopubSignatureElement.SIGNED_BY;

import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.KeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.DatatypeConverter;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.nanopub.MalformedNanopubException;
import org.nanopub.Nanopub;
import org.nanopub.NanopubImpl;
import org.nanopub.NanopubRdfHandler;
import org.nanopub.NanopubUtils;
import org.nanopub.NanopubWithNs;
import org.nanopub.trusty.TempUriReplacer;
import org.nanopub.trusty.TrustyNanopubUtils;

import net.trustyuri.TrustyUriException;
import net.trustyuri.TrustyUriUtils;
import net.trustyuri.rdf.RdfFileContent;
import net.trustyuri.rdf.RdfHasher;
import net.trustyuri.rdf.RdfPreprocessor;
import net.trustyuri.rdf.TransformRdf;

public class SignatureUtils {

	private SignatureUtils() {}  // no instances allowed

	public static NanopubSignatureElement getSignatureElement(Nanopub nanopub) throws MalformedCryptoElementException {
		IRI signatureUri = getSignatureElementUri(nanopub);
		if (signatureUri == null) return null;
		NanopubSignatureElement se = new NanopubSignatureElement(nanopub.getUri(), signatureUri);

		for (Statement st : nanopub.getHead()) se.addTargetStatement(st);
		for (Statement st : nanopub.getAssertion()) se.addTargetStatement(st);
		for (Statement st : nanopub.getProvenance()) se.addTargetStatement(st);

		for (Statement st : nanopub.getPubinfo()) {
			if (!st.getSubject().equals(signatureUri)) {
				se.addTargetStatement(st);
				continue;
			}
			if (st.getPredicate().equals(NanopubSignatureElement.HAS_SIGNATURE)) {
				// This statement is the only one that is *not* added as a target statement
				if (!(st.getObject() instanceof Literal)) {
					throw new MalformedCryptoElementException("Literal expected as signature: " + st.getObject());
				}
				se.setSignatureLiteral((Literal) st.getObject());
			} else {
				se.addTargetStatement(st);
				if (st.getPredicate().equals(CryptoElement.HAS_PUBLIC_KEY)) {
					if (!(st.getObject() instanceof Literal)) {
						throw new MalformedCryptoElementException("Literal expected as public key: " + st.getObject());
					}
					se.setPublicKeyLiteral((Literal) st.getObject());
				} else if (st.getPredicate().equals(CryptoElement.HAS_ALGORITHM)) {
					if (!(st.getObject() instanceof Literal)) {
						throw new MalformedCryptoElementException("Literal expected as algorithm: " + st.getObject());
					}
					se.setAlgorithm((Literal) st.getObject());
				} else if (st.getPredicate().equals(NanopubSignatureElement.SIGNED_BY)) {
					if (!(st.getObject() instanceof IRI)) {
						throw new MalformedCryptoElementException("URI expected as signer: " + st.getObject());
					}
					se.addSigner((IRI) st.getObject());
				}
				// We ignore other type of information at this point, but can consider it in the future.
			}
		}
		if (se.getSignature() == null) {
			throw new MalformedCryptoElementException("Signature element without signature");
		}
		if (se.getAlgorithm() == null) {
			throw new MalformedCryptoElementException("Signature element without algorithm");
		}
		if (se.getPublicKeyString() == null) {
			// We require a full public key for now, but plan to support public key fingerprints as an alternative.
			throw new MalformedCryptoElementException("Signature element without public key");
		}
		return se;
	}

	public static boolean hasValidSignature(NanopubSignatureElement se) throws GeneralSecurityException {
		String artifactCode = TrustyUriUtils.getArtifactCode(se.getTargetNanopubUri().toString());
		List<Statement> statements = RdfPreprocessor.run(se.getTargetStatements(), artifactCode);
		Signature signature = Signature.getInstance("SHA256with" + se.getAlgorithm().name());
		KeySpec publicSpec = new X509EncodedKeySpec(DatatypeConverter.parseBase64Binary(se.getPublicKeyString()));
		PublicKey publicKey = KeyFactory.getInstance(se.getAlgorithm().name()).generatePublic(publicSpec);
		signature.initVerify(publicKey);

//		System.err.println("SIGNATURE INPUT: ---");
//		System.err.print(RdfHasher.getDigestString(statements));
//		System.err.println("---");

		signature.update(RdfHasher.getDigestString(statements).getBytes());
		return signature.verify(se.getSignature());
	}

	public static Nanopub createSignedNanopub(Nanopub preNanopub, SignatureAlgorithm algorithm, KeyPair key, IRI signer)
			throws GeneralSecurityException, RDFHandlerException, TrustyUriException, MalformedNanopubException {
		// TODO: Test this more

		RdfFileContent r = new RdfFileContent(RDFFormat.TRIG);
		if (TempUriReplacer.hasTempUri(preNanopub)) {
			NanopubUtils.propagateToHandler(preNanopub, new TempUriReplacer(preNanopub, r, null));
			preNanopub = new NanopubImpl(r.getStatements(), r.getNamespaces());
		}

		Signature signature = Signature.getInstance("SHA256with" + algorithm.name());
		signature.initSign(key.getPrivate());

		List<Statement> preStatements = NanopubUtils.getStatements(preNanopub);
		IRI npUri = preNanopub.getUri();
		IRI piUri = preNanopub.getPubinfoUri();
		Map<String,String> nsMap = new HashMap<>();
		if (preNanopub instanceof NanopubWithNs) {
			NanopubWithNs preNanopubNs = (NanopubWithNs) preNanopub;
			for (String prefix : preNanopubNs.getNsPrefixes()) {
				nsMap.put(prefix, preNanopubNs.getNamespace(prefix));
			}
		}

		// Removing trusty URI if one is already present:
		if (TrustyNanopubUtils.isValidTrustyNanopub(preNanopub)) {
			String ac = TrustyUriUtils.getArtifactCode(preNanopub.getUri().toString());
			preStatements = removeArtifactCode(preStatements, ac);
			npUri = (IRI) removeArtifactCode(npUri, ac);
			piUri = (IRI) removeArtifactCode(piUri, ac);
			for (String prefix : nsMap.keySet()) {
				nsMap.put(prefix, removeArtifactCode(nsMap.get(prefix), ac));
			}
		}

		// Adding signature element:
		IRI signatureElUri = vf.createIRI(npUri + "sig");
		String publicKeyString = DatatypeConverter.printBase64Binary(key.getPublic().getEncoded()).replaceAll("\\s", "");
		Literal publicKeyLiteral = vf.createLiteral(publicKeyString);
		preStatements.add(vf.createStatement(signatureElUri, HAS_SIGNATURE_TARGET, npUri, piUri));
		preStatements.add(vf.createStatement(signatureElUri, CryptoElement.HAS_PUBLIC_KEY, publicKeyLiteral, piUri));
		Literal algorithmLiteral = vf.createLiteral(algorithm.name());
		preStatements.add(vf.createStatement(signatureElUri, CryptoElement.HAS_ALGORITHM, algorithmLiteral, piUri));
		if (signer != null) {
			preStatements.add(vf.createStatement(signatureElUri, SIGNED_BY, signer, piUri));
		}

		// Preprocess statements that are covered by signature:
		List<Statement> preprocessedStatements = RdfPreprocessor.run(preStatements, npUri);

		// Create signature:
		signature.update(RdfHasher.getDigestString(preprocessedStatements).getBytes());
		byte[] signatureBytes = signature.sign();
		Literal signatureLiteral = vf.createLiteral(DatatypeConverter.printBase64Binary(signatureBytes));

		// Preprocess signature statement:
		List<Statement> sigStatementList = new ArrayList<Statement>();
		sigStatementList.add(vf.createStatement(signatureElUri, HAS_SIGNATURE, signatureLiteral, piUri));
		Statement preprocessedSigStatement = RdfPreprocessor.run(sigStatementList, npUri).get(0);

		// Combine all statements:
		RdfFileContent signedContent = new RdfFileContent(RDFFormat.TRIG);
		signedContent.startRDF();
		for (String prefix : nsMap.keySet()) {
			signedContent.handleNamespace(prefix, nsMap.get(prefix));
		}
		signedContent.handleNamespace("npx", "http://purl.org/nanopub/x/");
		for (Statement st : preprocessedStatements) {
			signedContent.handleStatement(st);
		}
		signedContent.handleStatement(preprocessedSigStatement);
		signedContent.endRDF();

		// Create nanopub object:
		NanopubRdfHandler nanopubHandler = new NanopubRdfHandler();
		TransformRdf.transformPreprocessed(signedContent, npUri, nanopubHandler);
		return nanopubHandler.getNanopub();
	}

	// ----------
	// TODO: Move this into separate class?

	private static List<Statement> removeArtifactCode(List<Statement> in, String ac) {
		List<Statement> out = new ArrayList<>();
		for (Statement st : in) {
			out.add(removeArtifactCode(st, ac));
		}
		return out;
	}

	private static Statement removeArtifactCode(Statement st, String ac) {
		return vf.createStatement((Resource) removeArtifactCode(st.getSubject(), ac), (IRI) removeArtifactCode(st.getPredicate(), ac),
				removeArtifactCode(st.getObject(), ac), (Resource) removeArtifactCode(st.getContext(), ac));
	}

	private static Value removeArtifactCode(Value v, String ac) {
		if (v instanceof IRI) {
			return vf.createIRI(removeArtifactCode(v.stringValue(), ac));
		} else {
			return v;
		}
	}

	private static String removeArtifactCode(String s, String ac) {
		return s.replaceAll(ac + "[#/]?", "");
	}

	// ----------

	private static IRI getSignatureElementUri(Nanopub nanopub) throws MalformedCryptoElementException {
		IRI signatureElementUri = null;
		for (Statement st : nanopub.getPubinfo()) {
			if (!st.getPredicate().equals(NanopubSignatureElement.HAS_SIGNATURE_TARGET)) continue;
			if (!st.getObject().equals(nanopub.getUri())) continue;
			if (!(st.getSubject() instanceof IRI)) {
				throw new MalformedCryptoElementException("Signature element must be identified by URI");
			}
			if (signatureElementUri != null) {
				throw new MalformedCryptoElementException("Multiple signature elements found");
			}
			signatureElementUri = (IRI) st.getSubject();
		}
		return signatureElementUri;
	}

	/**
	 * This includes legacy signatures. Might include false positives.
	 */
	public static boolean seemsToHaveSignature(Nanopub nanopub) {
		for (Statement st : nanopub.getPubinfo()) {
			if (st.getPredicate().equals(NanopubSignatureElement.HAS_SIGNATURE_ELEMENT)) return true;
			if (st.getPredicate().equals(NanopubSignatureElement.HAS_SIGNATURE_TARGET)) return true;
			if (st.getPredicate().equals(NanopubSignatureElement.HAS_SIGNATURE)) return true;
			if (st.getPredicate().equals(CryptoElement.HAS_PUBLIC_KEY)) return true;
		}
		return false;
	}

	public static String getFullFilePath(String filename) {
		if (filename.startsWith("~")) {
			return System.getProperty("user.home") + "/" + filename.substring(1);
		}
		return filename;
	}

	private static ValueFactory vf = SimpleValueFactory.getInstance();

}
