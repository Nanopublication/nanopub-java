package org.nanopub.extra.security;

import java.util.LinkedHashSet;
import java.util.Set;

import org.nanopub.Nanopub;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;

// TODO: nanopub signatures are being updated...
// This code is not yet connected to the actual signing methods.

public class SignatureUtils {

	private SignatureUtils() {}  // no instances allowed

	public Set<URI> getSignatureElementUris(Nanopub nanopub) throws MalformedSignatureException {
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

}
