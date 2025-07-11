package org.nanopub;

import java.util.List;
import java.util.Map;

/**
 * This extension of the interface represents nanopub objects that store prefix/namespace mappings.
 *
 * @author Tobias Kuhn
 */
public interface NanopubWithNs extends Nanopub {


    /**
     * Returns a list of all namespace prefixes used in this nanopub.
     *
     * @return a list of namespace prefixes
     */
    public List<String> getNsPrefixes();

    /**
     * Returns a map of all namespace prefixes and their corresponding URIs used in this nanopub.
     *
     * @return a map of namespace prefixes and URIs
     */
    public Map<String, String> getNs();

    /**
     * Returns the URI of a namespace for a given prefix.
     *
     * @param prefix the namespace prefix
     * @return the namespace URI, or null if the prefix is not defined
     */
    public String getNamespace(String prefix);


    /**
     * Removes all namespace prefixes that are not used in any statement of this nanopub.
     */
    public void removeUnusedPrefixes();
}
