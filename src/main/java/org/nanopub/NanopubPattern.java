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
	 * This method should return true if the given nanopublication uses or
	 * seems to use this pattern (but not necessarily in a correct manner).
	 *
	 * @param nanopub The nanopublication
	 * @return true if the nanopublication seems to use the pattern
	 */
	public boolean isUsedBy(Nanopub nanopub);

	/**
	 * This method should return true if the given nanopublication uses this
	 * pattern in a correct manner.
	 *
	 * @param nanopub The nanopublication
	 * @return true if this pattern is used in a correct manner
	 */
	public boolean isCorrectlyUsedBy(Nanopub nanopub);

	/**
	 * This method can optionally return a short description of the given
	 * nanopublication, such as information about the relevant elements of
	 * this pattern (if valid) or the errors (if invalid).
	 *
	 * @param nanopub The nanopublication
	 * @return A short description of the nanopublication with respect to
	 *   the pattern
	 */
	public String getDescriptionFor(Nanopub nanopub);

	/**
	 * This method can optionally return a URL with additional information
	 * about the given pattern.
	 *
	 * @return A URL with additional information
	 */
	public URL getPatternInfoUrl() throws MalformedURLException;

}
