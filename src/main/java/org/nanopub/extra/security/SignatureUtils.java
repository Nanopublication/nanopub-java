package org.nanopub.extra.security;

import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.KeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.List;

import javax.xml.bind.DatatypeConverter;

import org.nanopub.Nanopub;
import org.openrdf.model.Literal;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;

import net.trustyuri.TrustyUriUtils;
import net.trustyuri.rdf.RdfHasher;
import net.trustyuri.rdf.RdfPreprocessor;

// TODO: nanopub signatures are being updated...

public class SignatureUtils {

	private SignatureUtils() {}  // no instances allowed

	public static NanopubSignatureElement getSignatureElement(Nanopub nanopub) throws MalformedSignatureException {
		URI signatureUri = getSignatureElementUri(nanopub);
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
					throw new MalformedSignatureException("Literal expected as signature: " + st.getObject());
				}
				se.setSignatureLiteral((Literal) st.getObject());
			} else {
				se.addTargetStatement(st);
				if (st.getPredicate().equals(NanopubSignatureElement.HAS_PUBLIC_KEY)) {
					if (!(st.getObject() instanceof Literal)) {
						throw new MalformedSignatureException("Literal expected as public key: " + st.getObject());
					}
					se.setPublicKeyLiteral((Literal) st.getObject());
				} else if (st.getPredicate().equals(NanopubSignatureElement.HAS_ALGORITHM)) {
					if (!(st.getObject() instanceof Literal)) {
						throw new MalformedSignatureException("Literal expected as algorithm: " + st.getObject());
					}
					se.setAlgorithm((Literal) st.getObject());
				} else if (st.getPredicate().equals(NanopubSignatureElement.SIGNED_BY)) {
					if (!(st.getObject() instanceof URI)) {
						throw new MalformedSignatureException("URI expected as signer: " + st.getObject());
					}
					se.addSigner((URI) st.getObject());
				}
				// We ignore other type of information at this point, but can consider it in the future.
			}
		}
		if (se.getSignature() == null) {
			throw new MalformedSignatureException("Signature element without signature");
		}
		if (se.getAlgorithm() == null) {
			throw new MalformedSignatureException("Signature element without algorithm");
		}
		if (se.getPublicKeyString() == null) {
			// We require a full public key for now, but plan to support public key fingerprints as an alternative.
			throw new MalformedSignatureException("Signature element without public key");
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
		signature.update(RdfHasher.getDigestString(statements).getBytes());
		return signature.verify(se.getSignature());
	}

	private static URI getSignatureElementUri(Nanopub nanopub) throws MalformedSignatureException {
		URI signatureElementUri = null;
		for (Statement st : nanopub.getPubinfo()) {
			if (!st.getPredicate().equals(NanopubSignatureElement.HAS_SIGNATURE_TARGET)) continue;
			if (!st.getObject().equals(nanopub.getUri())) continue;
			if (!(st.getSubject() instanceof URI)) {
				throw new MalformedSignatureException("Signature element must be identified by URI");
			}
			if (signatureElementUri != null) {
				throw new MalformedSignatureException("Multiple signature elements found");
			}
			signatureElementUri = (URI) st.getSubject();
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
			if (st.getPredicate().equals(NanopubSignatureElement.HAS_PUBLIC_KEY)) return true;
		}
		return false;
	}

	public static NanopubSignatureElement getLegacySignatureElement(Nanopub nanopub) throws MalformedSignatureException {
		URI signatureUri = getLegacySignatureElementUri(nanopub);
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
					throw new MalformedSignatureException("Literal expected as signature: " + st.getObject());
				}
				se.setSignatureLiteral((Literal) st.getObject());
			} else if (st.getPredicate().equals(NanopubSignatureElement.HAS_PUBLIC_KEY)) {
				if (!(st.getObject() instanceof Literal)) {
					throw new MalformedSignatureException("Literal expected as public key: " + st.getObject());
				}
				se.setPublicKeyLiteral((Literal) st.getObject());
			} else if (st.getPredicate().equals(NanopubSignatureElement.SIGNED_BY)) {
				if (!(st.getObject() instanceof URI)) {
					throw new MalformedSignatureException("URI expected as signer: " + st.getObject());
				}
				se.addSigner((URI) st.getObject());
			} else {
				se.addTargetStatement(st);
			}
		}
		if (se.getSignature() == null) {
			throw new MalformedSignatureException("Signature element without signature");
		}
		if (se.getPublicKeyString() == null) {
			throw new MalformedSignatureException("Signature element without public key");
		}
		se.setAlgorithm(NanopubSignatureElement.Algorithm.DSA);
		return se;
	}

	public static boolean hasValidLegacySignature(NanopubSignatureElement se) throws GeneralSecurityException {
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

	private static URI getLegacySignatureElementUri(Nanopub nanopub) throws MalformedSignatureException {
		URI signatureElementUri = null;
		for (Statement st : nanopub.getPubinfo()) {
			if (!st.getSubject().equals(nanopub.getUri())) continue;
			if (!st.getPredicate().equals(NanopubSignatureElement.HAS_SIGNATURE_ELEMENT)) continue;
			if (!(st.getObject() instanceof URI)) {
				throw new MalformedSignatureException("Signature element must be identified by URI");
			}
			if (signatureElementUri != null) {
				throw new MalformedSignatureException("Multiple signature elements found");
			}
			signatureElementUri = (URI) st.getObject();
		}
		return signatureElementUri;
	}

}
