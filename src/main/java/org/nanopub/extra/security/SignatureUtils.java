package org.nanopub.extra.security;

import static org.nanopub.extra.security.NanopubSignatureElement.HAS_SIGNATURE;
import static org.nanopub.extra.security.NanopubSignatureElement.HAS_SIGNATURE_TARGET;
import static org.nanopub.extra.security.NanopubSignatureElement.SIGNED_BY;

import java.security.GeneralSecurityException;
import java.security.KeyFactory;
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

	public static Nanopub createSignedNanopub(Nanopub preNanopub, TransformContext c)
			throws GeneralSecurityException, RDFHandlerException, TrustyUriException, MalformedNanopubException {
		// TODO: Test this more

		String u = preNanopub.getUri().stringValue();
		if (!preNanopub.getHeadUri().stringValue().startsWith(u) ||
				!preNanopub.getAssertionUri().stringValue().startsWith(u) ||
				!preNanopub.getProvenanceUri().stringValue().startsWith(u) ||
				!preNanopub.getPubinfoUri().stringValue().startsWith(u)) {
			throw new TrustyUriException("Graph URIs need have the nanopub URI as prefix: " + u + "...");
		}

		RdfFileContent r = new RdfFileContent(RDFFormat.TRIG);
		IRI npUri;
		Map<Resource,IRI> tempUriReplacerMap = null;
		if (TempUriReplacer.hasTempUri(preNanopub)) {
			npUri = vf.createIRI(TempUriReplacer.normUri);
			tempUriReplacerMap = new HashMap<>();
			NanopubUtils.propagateToHandler(preNanopub, new TempUriReplacer(preNanopub, r, tempUriReplacerMap));
		} else {
			npUri = preNanopub.getUri();
			NanopubUtils.propagateToHandler(preNanopub, r);
		}
		r = c.resolveCrossRefs(r);
		preNanopub = new NanopubImpl(r.getStatements(), r.getNamespaces());
		c.mergeTransformMap(tempUriReplacerMap);

		Signature signature = Signature.getInstance("SHA256with" + c.getSignatureAlgorithm().name());
		signature.initSign(c.getKey().getPrivate());

		List<Statement> preStatements = NanopubUtils.getStatements(preNanopub);
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
		String publicKeyString = DatatypeConverter.printBase64Binary(c.getKey().getPublic().getEncoded()).replaceAll("\\s", "");
		Literal publicKeyLiteral = vf.createLiteral(publicKeyString);
		preStatements.add(vf.createStatement(signatureElUri, HAS_SIGNATURE_TARGET, npUri, piUri));
		preStatements.add(vf.createStatement(signatureElUri, CryptoElement.HAS_PUBLIC_KEY, publicKeyLiteral, piUri));
		Literal algorithmLiteral = vf.createLiteral(c.getSignatureAlgorithm().name());
		preStatements.add(vf.createStatement(signatureElUri, CryptoElement.HAS_ALGORITHM, algorithmLiteral, piUri));
		if (c.getSigner() != null) {
			preStatements.add(vf.createStatement(signatureElUri, SIGNED_BY, c.getSigner(), piUri));
		}

		// Preprocess statements that are covered by signature:
		RdfFileContent preContent = new RdfFileContent(RDFFormat.TRIG);
		preContent.startRDF();
		for (Statement st : preStatements) preContent.handleStatement(st);
		preContent.endRDF();
		RdfFileContent preprocessedContent = new RdfFileContent(RDFFormat.TRIG);
		RdfPreprocessor rp = new RdfPreprocessor(preprocessedContent, npUri);
		try {
			preContent.propagate(rp);
		} catch (RDFHandlerException ex) {
			throw new TrustyUriException(ex);
		}

		// Create signature:
		signature.update(RdfHasher.getDigestString(preprocessedContent.getStatements()).getBytes());
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
		for (Statement st : preprocessedContent.getStatements()) {
			signedContent.handleStatement(st);
		}
		signedContent.handleStatement(preprocessedSigStatement);
		signedContent.endRDF();

		// Create nanopub object:
		NanopubRdfHandler nanopubHandler = new NanopubRdfHandler();
		IRI trustyUri = TransformRdf.transformPreprocessed(signedContent, npUri, nanopubHandler);
		Map<Resource,IRI> transformMap = TransformRdf.finalizeTransformMap(rp.getTransformMap(), TrustyUriUtils.getArtifactCode(trustyUri.toString()));
		c.mergeTransformMap(transformMap);
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
