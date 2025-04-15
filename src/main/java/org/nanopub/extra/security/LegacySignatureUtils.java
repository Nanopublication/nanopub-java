package org.nanopub.extra.security;

import static org.nanopub.extra.security.NanopubSignatureElement.HAS_SIGNATURE;
import static org.nanopub.extra.security.NanopubSignatureElement.HAS_SIGNATURE_ELEMENT;
import static org.nanopub.extra.security.NanopubSignatureElement.SIGNED_BY;

import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.KeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.List;

import javax.xml.bind.DatatypeConverter;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.nanopub.MalformedNanopubException;
import org.nanopub.Nanopub;
import org.nanopub.NanopubRdfHandler;
import org.nanopub.NanopubUtils;
import org.nanopub.trusty.TrustyNanopubUtils;

import net.trustyuri.TrustyUriException;
import net.trustyuri.TrustyUriUtils;
import net.trustyuri.rdf.RdfFileContent;
import net.trustyuri.rdf.RdfHasher;
import net.trustyuri.rdf.RdfPreprocessor;
import net.trustyuri.rdf.TransformRdf;

public class LegacySignatureUtils {

	private LegacySignatureUtils() {}  // no instances allowed

	public static NanopubSignatureElement getSignatureElement(Nanopub nanopub) throws MalformedCryptoElementException {
		IRI signatureUri = getSignatureElementUri(nanopub);
		if (signatureUri == null) return null;
		NanopubSignatureElement se = new NanopubSignatureElement(nanopub.getUri(), signatureUri);

		for (Statement st : nanopub.getHead()) se.addTargetStatement(st);
		for (Statement st : nanopub.getAssertion()) se.addTargetStatement(st);
		for (Statement st : nanopub.getProvenance()) se.addTargetStatement(st);

		for (Statement st : nanopub.getPubinfo()) {
			if (!st.getSubject().equals(signatureUri)) {
				if (!st.getPredicate().equals(NanopubSignatureElement.HAS_SIGNATURE_ELEMENT)) {
					se.addTargetStatement(st);
				}
				continue;
			}
			if (st.getPredicate().equals(NanopubSignatureElement.HAS_SIGNATURE)) {
				if (!(st.getObject() instanceof Literal)) {
					throw new MalformedCryptoElementException("Literal expected as signature: " + st.getObject());
				}
				se.setSignatureLiteral((Literal) st.getObject());
			} else if (st.getPredicate().equals(CryptoElement.HAS_PUBLIC_KEY)) {
				if (!(st.getObject() instanceof Literal)) {
					throw new MalformedCryptoElementException("Literal expected as public key: " + st.getObject());
				}
				se.setPublicKeyLiteral((Literal) st.getObject());
			} else if (st.getPredicate().equals(NanopubSignatureElement.SIGNED_BY)) {
				if (!(st.getObject() instanceof IRI)) {
					throw new MalformedCryptoElementException("URI expected as signer: " + st.getObject());
				}
				se.addSigner((IRI) st.getObject());
			} else {
				se.addTargetStatement(st);
			}
		}
		if (se.getSignature() == null) {
			throw new MalformedCryptoElementException("Signature element without signature");
		}
		if (se.getPublicKeyString() == null) {
			throw new MalformedCryptoElementException("Signature element without public key");
		}
		se.setAlgorithm(SignatureAlgorithm.DSA);
		return se;
	}

	public static boolean hasValidSignature(NanopubSignatureElement se) throws GeneralSecurityException {
		String artifactCode = TrustyUriUtils.getArtifactCode(se.getTargetNanopubUri().toString());
		List<Statement> statements = RdfPreprocessor.run(se.getTargetStatements(), artifactCode);
		Signature signature = Signature.getInstance("SHA1withDSA");
		KeySpec publicSpec = new X509EncodedKeySpec(DatatypeConverter.parseBase64Binary(se.getPublicKeyString()));
		PublicKey publicKey = KeyFactory.getInstance("DSA").generatePublic(publicSpec);
		signature.initVerify(publicKey);
		// Legacy signatures apply double digesting:
		signature.update(RdfHasher.digest(statements).digest());
		return signature.verify(se.getSignature());
	}

	public static Nanopub createSignedNanopub(Nanopub preNanopub, KeyPair key, IRI signer)
			throws GeneralSecurityException, RDFHandlerException, TrustyUriException, MalformedNanopubException {
		Signature dsaSignature = Signature.getInstance("SHA1withDSA");
		dsaSignature.initSign(key.getPrivate());

		RdfFileContent content = new RdfFileContent(RDFFormat.TRIG);
		NanopubUtils.propagateToHandler(preNanopub, content);
		content = RdfPreprocessor.run(content, preNanopub.getUri(), TrustyNanopubUtils.transformRdfSetting);

		// Legacy signatures apply double digesting:
		dsaSignature.update(RdfHasher.digest(content.getStatements()).digest());
		byte[] signatureBytes = dsaSignature.sign();
		String signatureString = DatatypeConverter.printBase64Binary(signatureBytes);

		ValueFactory vf = SimpleValueFactory.getInstance();
		RdfFileContent signatureContent = new RdfFileContent(RDFFormat.TRIG);
		IRI signatureElUri = vf.createIRI(preNanopub.getUri() + "sig");
		signatureContent.startRDF();
		signatureContent.handleNamespace("npx", "http://purl.org/nanopub/x/");
		IRI npUri = preNanopub.getUri();
		IRI piUri = preNanopub.getPubinfoUri();
		signatureContent.handleStatement(vf.createStatement(npUri, HAS_SIGNATURE_ELEMENT, signatureElUri, piUri));
		String publicKeyString = DatatypeConverter.printBase64Binary(key.getPublic().getEncoded()).replaceAll("\\s", "");
		Literal publicKeyLiteral = vf.createLiteral(publicKeyString);
		signatureContent.handleStatement(vf.createStatement(signatureElUri, CryptoElement.HAS_PUBLIC_KEY, publicKeyLiteral, piUri));
		Literal signatureLiteral = vf.createLiteral(signatureString);
		signatureContent.handleStatement(vf.createStatement(signatureElUri, HAS_SIGNATURE, signatureLiteral, piUri));
		if (signer != null) {
			signatureContent.handleStatement(vf.createStatement(signatureElUri, SIGNED_BY, signer, piUri));
		}
		signatureContent.endRDF();
		signatureContent = RdfPreprocessor.run(signatureContent, preNanopub.getUri(), TrustyNanopubUtils.transformRdfSetting);

		RdfFileContent signedContent = new RdfFileContent(RDFFormat.TRIG);
		signedContent.startRDF();
		content.propagate(signedContent, false);
		signatureContent.propagate(signedContent, false);
		signedContent.endRDF();
		NanopubRdfHandler nanopubHandler = new NanopubRdfHandler();
		TransformRdf.transformPreprocessed(signedContent, preNanopub.getUri(), nanopubHandler, TrustyNanopubUtils.transformRdfSetting);
		return nanopubHandler.getNanopub();
	}

	private static IRI getSignatureElementUri(Nanopub nanopub) throws MalformedCryptoElementException {
		IRI signatureElementUri = null;
		for (Statement st : nanopub.getPubinfo()) {
			if (!st.getSubject().equals(nanopub.getUri())) continue;
			if (!st.getPredicate().equals(NanopubSignatureElement.HAS_SIGNATURE_ELEMENT)) continue;
			if (!(st.getObject() instanceof IRI)) {
				throw new MalformedCryptoElementException("Signature element must be identified by URI");
			}
			if (signatureElementUri != null) {
				throw new MalformedCryptoElementException("Multiple signature elements found");
			}
			signatureElementUri = (IRI) st.getObject();
		}
		return signatureElementUri;
	}

}
