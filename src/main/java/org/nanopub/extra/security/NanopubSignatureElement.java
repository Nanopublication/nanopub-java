package org.nanopub.extra.security;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.xml.bind.DatatypeConverter;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

public class NanopubSignatureElement extends CryptoElement {

	private static final long serialVersionUID = 1L;

	public static final IRI SIGNATURE_ELEMENT = SimpleValueFactory.getInstance().createIRI("http://purl.org/nanopub/x/NanopubSignatureElement");
	public static final IRI HAS_SIGNATURE_TARGET = SimpleValueFactory.getInstance().createIRI("http://purl.org/nanopub/x/hasSignatureTarget");
	public static final IRI HAS_SIGNATURE = SimpleValueFactory.getInstance().createIRI("http://purl.org/nanopub/x/hasSignature");
	public static final IRI SIGNED_BY = SimpleValueFactory.getInstance().createIRI("http://purl.org/nanopub/x/signedBy");

	// Deprecated; used for legacy signatures
	public static final IRI HAS_SIGNATURE_ELEMENT = SimpleValueFactory.getInstance().createIRI("http://purl.org/nanopub/x/hasSignatureElement");

	private IRI targetNanopubUri;
	private byte[] signature;
	private Set<IRI> signers = new LinkedHashSet<>();
	private List<Statement> targetStatements = new ArrayList<>();

	NanopubSignatureElement(IRI targetNanopubUri, IRI uri) {
		super(uri);
		this.targetNanopubUri = targetNanopubUri;
	}

	public IRI getTargetNanopubUri() {
		return targetNanopubUri;
	}

	void setSignatureLiteral(Literal signatureLiteral) throws MalformedCryptoElementException {
		if (signature != null) {
			throw new MalformedCryptoElementException("Two signatures found for signature element");
		}
		signature = DatatypeConverter.parseBase64Binary(signatureLiteral.getLabel());
	}

	public byte[] getSignature() {
		return signature;
	}

	void addSigner(IRI signer) {
		signers.add(signer);
	}

	public Set<IRI> getSigners() {
		return signers;
	}

	void addTargetStatement(Statement st) {
		targetStatements.add(st);
	}

	public List<Statement> getTargetStatements() {
		return targetStatements;
	}

}
