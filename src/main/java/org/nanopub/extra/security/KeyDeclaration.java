package org.nanopub.extra.security;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Represents a declaration of a cryptographic key.
 */
public class KeyDeclaration extends CryptoElement {

    /**
     * The IRI for the property that indicates who declared the key.
     */
    public static final IRI DECLARED_BY = SimpleValueFactory.getInstance().createIRI("http://purl.org/nanopub/x/declaredBy");

    /**
     * The IRI for the property that indicates the location of the key.
     */
    public static final IRI HAS_KEY_LOCATION = SimpleValueFactory.getInstance().createIRI("http://purl.org/nanopub/x/hasKeyLocation");

    // TODO: Shouldn't there be only one declarer?
    private Set<IRI> declarers = new LinkedHashSet<>();
    private IRI keyLocation;

    /**
     * Creates a new KeyDeclaration with the specified URI.
     *
     * @param uri the IRI of the key declaration
     */
    public KeyDeclaration(IRI uri) {
        super(uri);
    }

    /**
     * Adds a declarer to this KeyDeclaration.
     *
     * @param declarer the IRI of the declarer
     */
    public void addDeclarer(IRI declarer) {
        declarers.add(declarer);
    }

    /**
     * Returns the set of declarers for this KeyDeclaration.
     *
     * @return a Set of IRI representing the declarers
     */
    public Set<IRI> getDeclarers() {
        return declarers;
    }

    /**
     * Checks if this KeyDeclaration has a specific declarer.
     *
     * @param declarer the IRI of the declarer to check
     * @return true if the declarer is present, false otherwise
     */
    public boolean hasDeclarer(IRI declarer) {
        return declarers.contains(declarer);
    }

    /**
     * Sets the location of the key associated with this KeyDeclaration.
     *
     * @param keyLocation the IRI representing the location of the key
     */
    public void setKeyLocation(IRI keyLocation) {
        this.keyLocation = keyLocation;
    }

    /**
     * Returns the location of the key associated with this KeyDeclaration.
     *
     * @return the IRI representing the key location, or null if not set
     */
    public IRI getKeyLocation() {
        return keyLocation;
    }

}
