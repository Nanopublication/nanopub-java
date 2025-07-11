package org.nanopub;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * This interface defines a Nanopublication Pattern, describing a certain type
 * of nanopublication.
 *
 * @author Tobias Kuhn
 */
public interface NanopubPattern extends Serializable {

    /**
     * This method should return the name of the pattern, as to be shown to the
     * user.
     *
     * @return The name of the pattern
     */
    public String getName();

    /**
     * This method should return true if this pattern applies to the given
     * nanopublication. This can be because the creators of the pattern think
     * that the given nanopublication should use it, or because the
     * nanopublication seems to be using the pattern, but not necessarily in a
     * correct and valid manner.
     *
     * @param nanopub The nanopublication
     * @return true if the pattern applies to the nanopublication
     */
    public boolean appliesTo(Nanopub nanopub);

    /**
     * This method should return true if the given nanopublication uses this
     * pattern in a correct and valid manner.
     *
     * @param nanopub The nanopublication
     * @return true if this pattern is used in a valid manner
     */
    public boolean isCorrectlyUsedBy(Nanopub nanopub);

    /**
     * This method can optionally return a short description of the given
     * nanopublication, such as information about the relevant elements of
     * this pattern (if valid) or the errors (if invalid).
     *
     * @param nanopub The nanopublication
     * @return A short description of the nanopublication with respect to
     * the pattern
     */
    public String getDescriptionFor(Nanopub nanopub);

    /**
     * This method should return a URL with additional information about the
     * given pattern.
     *
     * @return A URL with additional information
     * @throws MalformedURLException If the URL is malformed
     */
    public URL getPatternInfoUrl() throws MalformedURLException;

}
