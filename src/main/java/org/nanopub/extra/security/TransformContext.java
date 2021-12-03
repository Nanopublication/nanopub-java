package org.nanopub.extra.security;

import java.security.KeyPair;

import org.eclipse.rdf4j.model.IRI;

public class TransformContext {

	// TODO: Use this also for MakeTrustyNanopub

	private SignatureAlgorithm algorithm;
	private KeyPair key;
	private IRI signer;

	public TransformContext(SignatureAlgorithm algorithm, KeyPair key, IRI signer) {
		this.algorithm = algorithm;
		this.key = key;
		this.signer = signer;
	}

	public SignatureAlgorithm getSignatureAlgorithm() {
		return algorithm;
	}

	public KeyPair getKey() {
		return key;
	}

	public IRI getSigner() {
		return signer;
	}

}
