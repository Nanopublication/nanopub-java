package org.nanopub.extra.security;

import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.xml.bind.DatatypeConverter;

import org.openrdf.model.Literal;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;

import net.trustyuri.TrustyUriUtils;
import net.trustyuri.rdf.RdfHasher;
import net.trustyuri.rdf.RdfPreprocessor;

// TODO: nanopub signatures are being updated...
// Some of this code is not yet connected to the actual signing methods.

// TODO: Add algorithm info (see here: https://docs.oracle.com/javase/8/docs/technotes/guides/security/StandardNames.html#Signature)
// TODO: Add possiblity for public key fingerprint

public class NanopubSignatureElement {

	public static final URI SIGNATURE_ELEMENT = new URIImpl("http://purl.org/nanopub/x/NanopubSignatureElement");
	public static final URI HAS_SIGNATURE_TARGET = new URIImpl("http://purl.org/nanopub/x/hasSignatureTarget");
	public static final URI HAS_ALGORITHM = new URIImpl("http://purl.org/nanopub/x/hasAlgorithm");
	public static final URI HAS_PUBLIC_KEY = new URIImpl("http://purl.org/nanopub/x/hasPublicKey");
	public static final URI HAS_SIGNATURE = new URIImpl("http://purl.org/nanopub/x/hasSignature");
	public static final URI SIGNED_BY = new URIImpl("http://purl.org/nanopub/x/signedBy");

	// Will be @Deprecated
	public static final URI HAS_SIGNATURE_ELEMENT = new URIImpl("http://purl.org/nanopub/x/hasSignatureElement");

	private URI uri;
	private URI targetNanopubUri;
	private String publicKeyString;
	private String algorithm;
	private PublicKey publicKey;
	private byte[] signature;
	private Set<URI> signers = new LinkedHashSet<>();
	private List<Statement> targetStatements = new ArrayList<>();

	NanopubSignatureElement(URI targetNanopubUri, URI uri) {
		this.targetNanopubUri = targetNanopubUri;
		this.uri = uri;
	}

	public URI getUri() {
		return uri;
	}

	public URI getTargetNanopubUri() {
		return targetNanopubUri;
	}

	void setPublicKeyLiteral(Literal publicKeyLiteral) throws MalformedSignatureException {
		if (publicKeyString != null) {
			throw new MalformedSignatureException("Two public keys found for signature element");
		}
		publicKeyString = publicKeyLiteral.getLabel();
	}

	public PublicKey getPublicKey() throws MalformedSignatureException {
		if (publicKey == null) {
			if (publicKeyString == null) return null;
			KeySpec publicSpec = new X509EncodedKeySpec(DatatypeConverter.parseBase64Binary(publicKeyString));
			try {
				publicKey = getKeyFactory().generatePublic(publicSpec);
			} catch (InvalidKeySpecException ex) {
				throw new MalformedSignatureException(ex);
			}
		}
		return publicKey;
	}

	void setSignatureLiteral(Literal signatureLiteral) throws MalformedSignatureException {
		if (signature != null) {
			throw new MalformedSignatureException("Two signatures found for signature element");
		}
		signature = DatatypeConverter.parseBase64Binary(signatureLiteral.getLabel());
	}

	public byte[] getSignature() {
		return signature;
	}

	void setAlgorithm(Literal algorithmLiteral) throws MalformedSignatureException {
		if (algorithm != null) {
			throw new MalformedSignatureException("Two algorithms found for signature element");
		}
		if (!algorithmLiteral.getLabel().contains("with")) {
			throw new MalformedSignatureException("Algorithm strings does not follow '[DIGEST]with[ENCRYPTION]' pattern: " + algorithmLiteral.getLabel());
		}
		algorithm = algorithmLiteral.getLabel();
	}

	/**
	 * Algorithm string of the form "[DIGEST]with[ENCRYPTION]", for example "SHA256withDSA".
	 * See https://docs.oracle.com/javase/8/docs/technotes/guides/security/StandardNames.html#Signature
	 */
	public String getAlgorithm() {
		return algorithm;
	}

	void addSigner(URI signer) {
		signers.add(signer);
	}

	public Set<URI> getSigners() {
		return signers;
	}

	void addTargetStatement(Statement st) {
		targetStatements.add(st);
	}

	public List<Statement> getTargetStatements() {
		return targetStatements;
	}

	public boolean hasValidSignature() throws GeneralSecurityException {
		String artifactCode = TrustyUriUtils.getArtifactCode(targetNanopubUri.toString());
		List<Statement> statements = RdfPreprocessor.run(targetStatements, artifactCode);
		MessageDigest digest = RdfHasher.digest(statements);
		Signature dsa = Signature.getInstance("SHA1withDSA", "SUN");
		dsa.initVerify(publicKey);
		dsa.update(digest.digest());
		return dsa.verify(signature);
	}

	private KeyFactory getKeyFactory() {
		try {
			return KeyFactory.getInstance(getAlgorithm().replaceFirst(".*with", ""));
		} catch (NoSuchAlgorithmException ex) {
			throw new RuntimeException(ex);
		}
	}

}
