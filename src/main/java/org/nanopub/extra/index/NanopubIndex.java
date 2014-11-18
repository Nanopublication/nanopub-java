package org.nanopub.extra.index;

import java.util.Set;

import org.nanopub.Nanopub;
import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;

/**
 * A nanopublication index is a nanopublication that refers to the URIs of other nanopublications,
 * thereby defining a set or collection of nanopubs. A single index can only contain references to
 * up to 1000 nanopublications (or sub-indexes), but indexes can refer to sub-indexes or append
 * existing indexes and thereby define sets of arbitrary size. Indexes that are appended by other
 * indexes and do not define a meaningful set on their own are called "incomplete". Indexes that
 * are not appended by other indexes or despite being appended define a meaningful set of nanopubs
 * on their own, are called "complete". For example, to define a set of 2500 nanopublications, one
 * can define a first incomlete index with 1000 nanopubs, then a second incomplete one that appends
 * the first and refers to the next 1000 nanopubs, and finally a complete index that appends the
 * second and adds the remaining 500 nanopubs, thereby containing in total 2500 of them.
 *
 * See the following paper for a description of the general approach: http://arxiv.org/pdf/1411.2749
 *
 * @author Tobias Kuhn
 */
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
