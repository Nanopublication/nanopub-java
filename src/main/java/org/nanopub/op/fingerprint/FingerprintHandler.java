package org.nanopub.op.fingerprint;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.nanopub.Nanopub;

/**
 * Interface that represents a fingerprint handler for Nanopubs.
 */
public interface FingerprintHandler {

    /**
     * IRI placeholder for the nanopub URI.
     */
    public static final IRI nanopubUriPlaceholder = SimpleValueFactory.getInstance().createIRI("http://purl.org/nanopub/placeholders/nanopuburi");

    /**
     * IRI placeholder for the nanopub head URI.
     */
    public static final IRI headUriPlaceholder = SimpleValueFactory.getInstance().createIRI("http://purl.org/nanopub/placeholders/head");

    /**
     * IRI placeholder for the nanopub assertion URI.
     */
    public static final IRI assertionUriPlaceholder = SimpleValueFactory.getInstance().createIRI("http://purl.org/nanopub/placeholders/assertion");

    /**
     * IRI placeholder for the nanopub provenance URI.
     */
    public static final IRI provUriPlaceholder = SimpleValueFactory.getInstance().createIRI("http://purl.org/nanopub/placeholders/provenance");

    /**
     * IRI placeholder for the nanopub pubinfo URI.
     */
    public static final IRI pubinfoUriPlaceholder = SimpleValueFactory.getInstance().createIRI("http://purl.org/nanopub/placeholders/pubinfo");

    /**
     * IRI placeholder for nanopub timestamp.
     */
    public static final IRI timestampPlaceholder = SimpleValueFactory.getInstance().createIRI("http://purl.org/nanopub/placeholders/timestamp");

    /**
     * Returns a fingerprint for the given Nanopub.
     *
     * @param np the Nanopub for which to generate a fingerprint
     * @return a fingerprint string for the Nanopub
     */
    public String getFingerprint(Nanopub np);

}