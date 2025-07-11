package org.nanopub.trusty;

import java.net.MalformedURLException;
import java.net.URL;

import net.trustyuri.TrustyUriUtils;

import org.nanopub.Nanopub;
import org.nanopub.NanopubPattern;

/**
 * A nanopublication pattern that checks if a nanopublication has a valid Trusty URI.
 */
public class TrustyNanopubPattern implements NanopubPattern {

    /**
     * Default constructor.
     */
    public TrustyNanopubPattern() {
    }

    /**
     * Returns the name of this nanopublication pattern.
     *
     * @return the name of the pattern
     */
    @Override
    public String getName() {
        return "Trusty nanopublication";
    }

    /**
     * Checks if this nanopublication pattern applies to the given nanopublication.
     *
     * @param nanopub The nanopublication
     * @return true if the nanopublication has a potential Trusty URI, false otherwise
     */
    @Override
    public boolean appliesTo(Nanopub nanopub) {
        return TrustyUriUtils.isPotentialTrustyUri(nanopub.getUri());
    }

    /**
     * Checks if this nanopublication pattern is correctly used by the given nanopublication.
     *
     * @param nanopub The nanopublication
     * @return true if the nanopublication has a valid Trusty URI, false otherwise
     */
    @Override
    public boolean isCorrectlyUsedBy(Nanopub nanopub) {
        return TrustyNanopubUtils.isValidTrustyNanopub(nanopub);
    }

    /**
     * Returns the description of this nanopublication pattern.
     *
     * @param nanopub The nanopublication
     * @return a description of the nanopublication pattern
     */
    @Override
    public String getDescriptionFor(Nanopub nanopub) {
        if (TrustyNanopubUtils.isValidTrustyNanopub(nanopub)) {
            return "This nanopublication has a valid Trusty URI.";
        } else {
            return "The Trusty URI of this nanopublication is not valid.";
        }
    }

    /**
     * Returns the URL where more information about this nanopublication pattern can be found.
     *
     * @return the URL for more information
     * @throws MalformedURLException if the URL is malformed
     */
    @Override
    public URL getPatternInfoUrl() throws MalformedURLException {
        return new URL("http://trustyuri.net/");
    }

}
