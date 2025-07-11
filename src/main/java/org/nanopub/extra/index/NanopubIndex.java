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
 * can define a first incomplete index with 1000 nanopubs, then a second incomplete one that appends
 * the first and refers to the next 1000 nanopubs, and finally a complete index that appends the
 * second and adds the remaining 500 nanopubs, thereby containing in total 2500 of them.
 * <p>
 * See the following paper for a description of the general approach:
 * <a href="http://arxiv.org/pdf/1411.2749">http://arxiv.org/pdf/1411.2749</a>
 *
 * @author Tobias Kuhn
 */
public interface NanopubIndex extends Nanopub {

    /**
     * The URI of the nanopublication index type.
     */
    public static final IRI NANOPUB_INDEX_URI = SimpleValueFactory.getInstance().createIRI("http://purl.org/nanopub/x/NanopubIndex");

    /**
     * The URI of the nanopublication index incomplete type.
     */
    public static final IRI INCOMPLETE_INDEX_URI = SimpleValueFactory.getInstance().createIRI("http://purl.org/nanopub/x/IncompleteIndex");

    /**
     * The URI of the nanopublication index assertion.
     */
    public static final IRI INDEX_ASSERTION_URI = SimpleValueFactory.getInstance().createIRI("http://purl.org/nanopub/x/IndexAssertion");

    /**
     * The URI of the nanopublication index includes element.
     */
    public static final IRI INCLUDES_ELEMENT_URI = SimpleValueFactory.getInstance().createIRI("http://purl.org/nanopub/x/includesElement");

    /**
     * The URI of the nanopublication index includes sub-index.
     */
    public static final IRI INCLUDES_SUBINDEX_URI = SimpleValueFactory.getInstance().createIRI("http://purl.org/nanopub/x/includesSubindex");

    /**
     * The URI of the nanopublication index appends index.
     */
    public static final IRI APPENDS_INDEX_URI = SimpleValueFactory.getInstance().createIRI("http://purl.org/nanopub/x/appendsIndex");

    /**
     * The maximum number of elements.
     */
    public static final int MAX_SIZE = 1000;

    /**
     * Returns the set of IRIs that are included as elements in this index.
     *
     * @return a set of IRIs representing the elements of this index
     */
    public Set<IRI> getElements();

    /**
     * Returns the set of IRIs that are included as sub-indexes in this index.
     *
     * @return a set of IRIs representing the sub-indexes of this index
     */
    public Set<IRI> getSubIndexes();

    /**
     * Returns the IRI of the index that is appended by this index.
     *
     * @return the IRI of the appended index, or null if this index does not append another index
     */
    public IRI getAppendedIndex();

    /**
     * Checks if this index is complete.
     *
     * @return true if this index is complete, false if it is incomplete
     */
    public boolean isIncomplete();

    /**
     * Returns the name of the nanopublication index.
     *
     * @return the name of the index
     */
    public String getName();

    /**
     * Returns the description of the nanopublication index.
     *
     * @return the description of the index
     */
    public String getDescription();

    /**
     * Returns the seeAlso URIs of the nanopublication index.
     *
     * @return a set of IRI objects representing the seeAlso URIs
     */
    public Set<IRI> getSeeAlsoUris();

}
