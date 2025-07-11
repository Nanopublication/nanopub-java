package org.nanopub.extra.security;

import jakarta.xml.bind.DatatypeConverter;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Represents a signature element in a Nanopub.
 */
public class NanopubSignatureElement extends CryptoElement {

    /**
     * The IRI for the signature element in a Nanopub.
     */
    public static final IRI SIGNATURE_ELEMENT = SimpleValueFactory.getInstance().createIRI("http://purl.org/nanopub/x/NanopubSignatureElement");

    /**
     * The IRIs for the signature target property.
     */
    public static final IRI HAS_SIGNATURE_TARGET = SimpleValueFactory.getInstance().createIRI("http://purl.org/nanopub/x/hasSignatureTarget");

    /**
     * The IRIs for the has signature property.
     */
    public static final IRI HAS_SIGNATURE = SimpleValueFactory.getInstance().createIRI("http://purl.org/nanopub/x/hasSignature");

    /**
     * The IRI for the signed by property, indicating who signed the Nanopub.
     */
    public static final IRI SIGNED_BY = SimpleValueFactory.getInstance().createIRI("http://purl.org/nanopub/x/signedBy");

    /**
     * The IRI for the has signature element property.
     */
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

    /**
     * Returns the IRI of the target Nanopub that this signature is associated with.
     *
     * @return the IRI of the target Nanopub
     */
    public IRI getTargetNanopubUri() {
        return targetNanopubUri;
    }

    void setSignatureLiteral(Literal signatureLiteral) throws MalformedCryptoElementException {
        if (signature != null) {
            throw new MalformedCryptoElementException("Two signatures found for signature element");
        }
        signature = DatatypeConverter.parseBase64Binary(signatureLiteral.getLabel());
    }

    /**
     * Returns the signature as a byte array.
     *
     * @return the signature byte array
     */
    public byte[] getSignature() {
        return signature;
    }

    void addSigner(IRI signer) {
        signers.add(signer);
    }

    /**
     * Returns the set of IRIs representing the signers of this Nanopub signature.
     *
     * @return a set of IRIs of signers
     */
    public Set<IRI> getSigners() {
        return signers;
    }

    void addTargetStatement(Statement st) {
        targetStatements.add(st);
    }

    /**
     * Returns the list of target statements that are part of this signature element.
     *
     * @return a list of target statements
     */
    public List<Statement> getTargetStatements() {
        return targetStatements;
    }

}
