package org.nanopub.extra.server;

import org.eclipse.rdf4j.model.IRI;

/**
 * Exception thrown when a registry refuses a nanopublication on its content rather than being
 * temporarily unable to take it: the status is a 4xx other than 408 or 429.
 * <p>
 * Every registry applies the same rules — triple and byte limits, a creation time that is not in
 * the future, covered types, protected nanopublications — so another registry would answer the
 * same, and publishing this nanopublication stops instead of spending the rest of the registry
 * list on it.
 *
 * @author Tobias Kuhn
 */
public class NanopubRejectedException extends RuntimeException {

    private final IRI nanopubUri;
    private final String registryUrl;
    private final int statusCode;
    private final String reasonPhrase;

    /**
     * Constructs a new NanopubRejectedException.
     *
     * @param nanopubUri   the URI of the nanopublication that was refused
     * @param registryUrl  the URL of the registry that refused it
     * @param statusCode   the HTTP status code the registry answered with
     * @param reasonPhrase the HTTP reason phrase, which is where a registry says why
     */
    public NanopubRejectedException(IRI nanopubUri, String registryUrl, int statusCode, String reasonPhrase) {
        super("Registry " + registryUrl + " rejected nanopublication " + nanopubUri + " with HTTP "
              + statusCode + (reasonPhrase == null || reasonPhrase.isBlank() ? "" : " " + reasonPhrase)
              + ". Other registries apply the same rules, so this nanopublication was not published.");
        this.nanopubUri = nanopubUri;
        this.registryUrl = registryUrl;
        this.statusCode = statusCode;
        this.reasonPhrase = reasonPhrase;
    }

    /**
     * Returns the URI of the nanopublication that was refused.
     *
     * @return the nanopublication URI
     */
    public IRI getNanopubUri() {
        return nanopubUri;
    }

    /**
     * Returns the URL of the registry that refused the nanopublication.
     *
     * @return the registry URL
     */
    public String getRegistryUrl() {
        return registryUrl;
    }

    /**
     * Returns the HTTP status code the registry answered with.
     *
     * @return the HTTP status code
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * Returns the HTTP reason phrase the registry answered with.
     *
     * @return the HTTP reason phrase, which may be null or empty
     */
    public String getReasonPhrase() {
        return reasonPhrase;
    }

}
