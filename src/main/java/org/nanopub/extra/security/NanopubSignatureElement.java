package org.nanopub.extra.security;

import jakarta.xml.bind.DatatypeConverter;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Statement;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Represents a signature element in a Nanopub.
 */
public class NanopubSignatureElement extends CryptoElement {

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
