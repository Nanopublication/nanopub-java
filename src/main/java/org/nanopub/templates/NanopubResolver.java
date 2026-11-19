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
     * {@link GetNanopub#get(String)}.
     */
    NanopubResolver DEFAULT = GetNanopub::get;

    /**
     * Retrieves nothing. With this resolver a template is read from its own nanopublication
     * alone, without any network access.
     */
    NanopubResolver NONE = uriOrArtifactCode -> null;

    /**
     * Retrieves a nanopublication.
     *
     * @param uriOrArtifactCode the URI or the artifact code of the nanopublication
     * @return the nanopublication, or null if it cannot be retrieved
     */
    Nanopub getNanopub(String uriOrArtifactCode);

}
