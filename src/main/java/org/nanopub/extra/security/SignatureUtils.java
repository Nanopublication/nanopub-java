package org.nanopub.extra.security;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.nanopub.Nanopub;
import org.openrdf.model.Literal;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;

// TODO: nanopub signatures are being updated...
// This code is not yet connected to the actual signing methods.

public class SignatureUtils {

	private SignatureUtils() {}  // no instances allowed

	public static Set<URI> getSignatureElementUris(Nanopub nanopub) throws MalformedSignatureException {
		Set<URI> signatureUris = new LinkedHashSet<>();
		for (Statement st : nanopub.getPubinfo()) {
			if (!st.getSubject().equals(nanopub.getUri())) continue;
			if (!st.getPredicate().equals(NanopubSignatureElement.HAS_SIGNATURE_ELEMENT)) continue;
			if (!(st.getObject() instanceof URI)) {
				throw new MalformedSignatureException("Signature element must be identified by URI");
			}
			signatureUris.add((URI) st.getObject());
		}
		return signatureUris;
	}

	public static Map<URI,NanopubSignatureElement> getSignatureElements(Nanopub nanopub) throws MalformedSignatureException {
		return getSignatureElements(nanopub, getSignatureElementUris(nanopub));
	}

	public static Map<URI,NanopubSignatureElement> getSignatureElements(Nanopub nanopub, Set<URI> signatureUris) throws MalformedSignatureException {
		Map<URI,NanopubSignatureElement> seMap = new HashMap<>();
		for (URI signatureUri : signatureUris) {
			NanopubSignatureElement el = new NanopubSignatureElement(signatureUri);
			seMap.put(signatureUri, el);
		}
		
		for (Statement st : nanopub.getPubinfo()) {
			if (seMap.containsKey(st.getPredicate())) {
				throw new MalformedSignatureException("Illegal use of signature URI as predicate: " + st.getPredicate());
			}
			URI subjUri = (URI) st.getSubject();
			Value objValue = st.getObject();
			URI objUri = null;
			if (objValue instanceof URI) objUri = (URI) objValue;
			if (seMap.containsKey(subjUri)) {
				if (objUri != null && seMap.containsKey(objUri) && !(subjUri.equals(objUri))) {
					throw new MalformedSignatureException("Signature elements overlap: " + st);
				}
				processSignatureInfo(seMap.get(subjUri), st);
			} else if (objUri != null && seMap.containsKey(objUri)) {
				processSignatureInfo(seMap.get(objUri), st);
			}
		}
		for (NanopubSignatureElement se : seMap.values()) {
			if (se.getSignature() == null) {
				throw new MalformedSignatureException("Signature element without signature: " + se.getUri());
			}
		}
		return seMap;
	}

	private static void processSignatureInfo(NanopubSignatureElement se, Statement st) throws MalformedSignatureException {
		if (st.getSubject().equals(se.getUri())) {
			if (st.getPredicate().equals(NanopubSignatureElement.HAS_SIGNATURE)) {
				if (!(st.getObject() instanceof Literal)) {
					throw new MalformedSignatureException("Literal expected as signature: " + st.getObject());
				}
				if (se.getSignature() != null) {
					throw new MalformedSignatureException("Two signatures found for: " + se.getUri());
				}
				se.setSignatureLiteral((Literal) st.getObject());
			} else if (st.getPredicate().equals(NanopubSignatureElement.HAS_PUBLIC_KEY)) {
				if (!(st.getObject() instanceof Literal)) {
					throw new MalformedSignatureException("Literal expected as public key: " + st.getObject());
				}
				if (se.getPublicKey() != null) {
					throw new MalformedSignatureException("Two public keys found for: " + se.getUri());
				}
				se.setPublicKeyLiteral((Literal) st.getObject());
			} else if (st.getPredicate().equals(NanopubSignatureElement.SIGNED_BY)) {
				if (!(st.getObject() instanceof URI)) {
					throw new MalformedSignatureException("URI expected as signer: " + st.getObject());
				}
				se.addSigner((URI) st.getObject());
			}
			// We ignore other type of information at this point, but can consider it in the future.
		} else if (st.getObject().equals(se.getUri())) {
			// Only HAS_SIGNATURE_ELEMENT is currently pointing to signature elements in object position,
			// which we can ignore here.
		}
	}

}
