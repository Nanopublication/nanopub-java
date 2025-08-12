package org.nanopub.trusty;

import net.trustyuri.TrustyUriUtils;
import org.nanopub.Nanopub;
import org.nanopub.NanopubPattern;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

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
     * {@inheritDoc}
     * <p>
     * Returns the name of this nanopublication pattern.
     */
    @Override
    public String getName() {
        return "Trusty nanopublication";
    }

    /**
     * {@inheritDoc}
     * <p>
     * Checks if this nanopublication pattern applies to the given nanopublication.
     */
    @Override
    public boolean appliesTo(Nanopub nanopub) {
        return TrustyUriUtils.isPotentialTrustyUri(nanopub.getUri());
    }

    /**
     * {@inheritDoc}
     * <p>
     * Checks if this nanopublication pattern is correctly used by the given nanopublication.
     */
    @Override
    public boolean isCorrectlyUsedBy(Nanopub nanopub) {
        return TrustyNanopubUtils.isValidTrustyNanopub(nanopub);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns the description of this nanopublication pattern.
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
     * {@inheritDoc}
     * <p>
     * Returns the URL where more information about this nanopublication pattern can be found.
     */
    @Override
    public URL getPatternInfoUrl() throws MalformedURLException, URISyntaxException {
        return new URI("http://trustyuri.net/").toURL();
    }

}
