package org.nanopub;

import java.util.List;
import java.util.Map;

/**
 * This extension of the interface represents nanopub objects that store prefix/namespace mappings.
 *
 * @author Tobias Kuhn
 */
public interface NanopubWithNs extends Nanopub {


	public List<String> getNsPrefixes();

	public Map<String, String> getNs();

	public String getNamespace(String prefix);

	public void removeUnusedPrefixes();
}
