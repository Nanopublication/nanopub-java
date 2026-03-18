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

    /**
     * Creates an ArtifactCode from the given string. Returns null if the string is not a valid artifact code.
     *
     * @param code the string to create the ArtifactCode from
     * @return the ArtifactCode, or null if the string is not a valid artifact code
     */
    static ArtifactCode of(String code) {
        if (code == null) {
            return null;
        }
        try {
            return new ArtifactCodeImpl(code);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

}
