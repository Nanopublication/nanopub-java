package org.nanopub.templates;

import org.nanopub.Nanopub;
import org.nanopub.extra.server.GetNanopub;

import java.io.Serializable;

/**
 * Retrieves the nanopublications a {@link Template} refers to: the template itself when it is
 * given by its ID, and the nanopublications that hold the possible values of its
 * restricted-choice placeholders.
 * <p>
 * Implementations decide where nanopublications come from (the network, a cache, local
 * files). They are serializable because a {@link Template} is.
 */
@FunctionalInterface
public interface NanopubResolver extends Serializable {

    /**
     * Retrieves nanopublications from the nanopublication network with
     * {@link GetNanopub#get(String)}. The nanopublication is found by the artifact code in
     * what is given, so an IRI within a nanopublication, such as the node of a template
     * (<code>.../RA.../template</code>), retrieves that nanopublication. Null is returned
     * for anything without an artifact code.
     */
    NanopubResolver DEFAULT = NanopubResolver::getFromNetwork;

    /**
     * Retrieves nothing. With this resolver a template is read from its own nanopublication
     * alone, without any network access.
     */
    NanopubResolver NONE = uriOrArtifactCode -> null;

    /**
     * Retrieves a nanopublication.
     *
     * @param uriOrArtifactCode the URI or the artifact code of the nanopublication, or an IRI
     *                          within it
     * @return the nanopublication, or null if it cannot be retrieved
     */
    Nanopub getNanopub(String uriOrArtifactCode);

    private static Nanopub getFromNetwork(String uriOrArtifactCode) {
        String artifactCode = LocalNanopubs.getArtifactCode(uriOrArtifactCode);
        return artifactCode == null ? null : GetNanopub.get(artifactCode);
    }

}
