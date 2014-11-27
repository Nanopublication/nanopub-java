package org.nanopub.extra.security;

import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;

public class NanopubSignature {

	private NanopubSignature() {}  // no instances allowed

	public static final URI SIGNATURE_ELEMENT = new URIImpl("http://purl.org/nanopub/x/NanopubSignatureElement");
	public static final URI HAS_SIGNATURE_ELEMENT = new URIImpl("http://purl.org/nanopub/x/hasSignatureElement");
	public static final URI HAS_PUBLIC_KEY = new URIImpl("http://purl.org/nanopub/x/hasPublicKey");
	public static final URI HAS_SIGNATURE = new URIImpl("http://purl.org/nanopub/x/hasSignature");
	public static final URI SIGNED_BY = new URIImpl("http://purl.org/nanopub/x/signedBy");

}
