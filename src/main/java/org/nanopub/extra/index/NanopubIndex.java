package org.nanopub.extra.index;

import java.util.Set;

import org.nanopub.Nanopub;
import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;

public interface NanopubIndex extends Nanopub {

	public static final URI NANOPUB_INDEX_URI = new URIImpl("http://purl.org/nanopub/x/NanopubIndex");
	public static final URI INCOMPLETE_INDEX_URI = new URIImpl("http://purl.org/nanopub/x/IncompleteIndex");
	public static final URI INDEX_ASSERTION_URI = new URIImpl("http://purl.org/nanopub/x/IndexAssertion");
	public static final URI INCLUDES_ELEMENT_URI = new URIImpl("http://purl.org/nanopub/x/includesElement");
	public static final URI INCLUDES_SUBINDEX_URI = new URIImpl("http://purl.org/nanopub/x/includesSubindex");
	public static final URI APPENDS_INDEX_URI = new URIImpl("http://purl.org/nanopub/x/appendsIndex");

	public static final int MAX_SIZE = 1000;

	public Set<URI> getElements();

	public Set<URI> getSubIndexes();

	public URI getAppendedIndex();

	public boolean isIncomplete();

	public String getName();

	public String getDescription();

	public Set<URI> getSeeAlsoUris();

}
