package org.nanopub.extra.index;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.nanopub.Nanopub;

import java.util.Set;

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
 * <p>
 * See the following paper for a description of the general approach:
 * <a href="http://arxiv.org/pdf/1411.2749"></a>...</a>
 *
 * @author Tobias Kuhn
 */
public interface NanopubIndex extends Nanopub {

	public static final IRI NANOPUB_INDEX_URI = SimpleValueFactory.getInstance().createIRI("http://purl.org/nanopub/x/NanopubIndex");
	public static final IRI INCOMPLETE_INDEX_URI = SimpleValueFactory.getInstance().createIRI("http://purl.org/nanopub/x/IncompleteIndex");
	public static final IRI INDEX_ASSERTION_URI = SimpleValueFactory.getInstance().createIRI("http://purl.org/nanopub/x/IndexAssertion");
	public static final IRI INCLUDES_ELEMENT_URI = SimpleValueFactory.getInstance().createIRI("http://purl.org/nanopub/x/includesElement");
	public static final IRI INCLUDES_SUBINDEX_URI = SimpleValueFactory.getInstance().createIRI("http://purl.org/nanopub/x/includesSubindex");
	public static final IRI APPENDS_INDEX_URI = SimpleValueFactory.getInstance().createIRI("http://purl.org/nanopub/x/appendsIndex");

	public static final int MAX_SIZE = 1000;

	public Set<IRI> getElements();

	public Set<IRI> getSubIndexes();

	public IRI getAppendedIndex();

	public boolean isIncomplete();

	public String getName();

	public String getDescription();

	public Set<IRI> getSeeAlsoUris();

}
