package org.nanopub;

import net.trustyuri.TrustyUriModule;

/**
 * An interface for artifact codes.
 *
 */
public interface ArtifactCode {

    /**
     * Returns the artifact code as a string.
     *
     * @return the artifact code
     */
    String getCode();

    /**
     * Returns the module ID associated with the artifact code.
     *
     * @return the module ID
     */
    TrustyUriModule getModule();

}
