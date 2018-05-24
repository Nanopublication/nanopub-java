package org.nanopub.extra.security;

import org.nanopub.Nanopub;
import org.openrdf.model.Literal;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.impl.LiteralImpl;

// TODO: nanopub signatures are being updated...
// This code is not yet connected to the actual signing methods.

public class SignatureUtils {

	private SignatureUtils() {}  // no instances allowed

	public static NanopubSignatureElement getSignatureElement(Nanopub nanopub) throws MalformedSignatureException {
		URI signatureUri = getSignatureElementUri(nanopub);
		if (signatureUri == null) return null;
		NanopubSignatureElement se = new NanopubSignatureElement(nanopub.getUri(), signatureUri);
		
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
		if (se.getPublicKey() == null) {
			// We require a full public key for now, but plan to support public key fingerprints as an alternative.
			throw new MalformedSignatureException("Signature element without public key");
		}
		return se;
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

	public static NanopubSignatureElement getLegacySignatureElement(Nanopub nanopub) throws MalformedSignatureException {
		URI signatureUri = getLegacySignatureElementUri(nanopub);
		if (signatureUri == null) return null;
		NanopubSignatureElement se = new NanopubSignatureElement(nanopub.getUri(), signatureUri);
		
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
		if (se.getPublicKey() == null) {
			throw new MalformedSignatureException("Signature element without public key");
		}
		se.setAlgorithm(new LiteralImpl("SHA1withDSA"));
		return se;
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
