package org.nanopub.extra.index;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.DC;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.nanopub.MalformedNanopubException;
import org.nanopub.Nanopub;
import org.nanopub.NanopubWithNs;

import java.util.*;

/**
 * Implementation of the NanopubIndex interface, representing a nanopublication index.
 */
public class NanopubIndexImpl implements NanopubIndex, NanopubWithNs {

    private final Nanopub np;
    private final Set<IRI> elementSet;
    private final Set<IRI> subIndexSet;
    private final IRI appendedIndex;
    private boolean isIncompleteIndex = false;

    /**
     * Creates a new NanopubIndexImpl instance from a given Nanopub.
     *
     * @param npIndex the Nanopub to be used as an index
     * @throws MalformedNanopubException if the Nanopub does not conform to the expected structure of an index
     */
    protected NanopubIndexImpl(Nanopub npIndex) throws MalformedNanopubException {
        this.np = npIndex;
        if (!IndexUtils.isIndex(np)) {
            throw new MalformedNanopubException("Nanopub is not a nanopub index");
        }
        IRI appendedIndex = null;
        Set<IRI> elementSet = new HashSet<>();
        Set<IRI> subIndexSet = new HashSet<>();
        for (Statement st : np.getAssertion()) {
            if (!st.getSubject().equals(np.getUri())) continue;
            if (st.getPredicate().equals(NanopubIndex.APPENDS_INDEX_URI)) {
                if (appendedIndex != null) {
                    throw new MalformedNanopubException("Multiple appends-statements found for index");
                }
                if (!(st.getObject() instanceof IRI)) {
                    throw new MalformedNanopubException("URI expected for object of appends-statement");
                }
                appendedIndex = (IRI) st.getObject();
            } else if (st.getPredicate().equals(NanopubIndex.INCLUDES_ELEMENT_URI)) {
                if (!(st.getObject() instanceof IRI)) {
                    throw new MalformedNanopubException("Element has to be a URI");
                }
                elementSet.add((IRI) st.getObject());
            } else if (st.getPredicate().equals(NanopubIndex.INCLUDES_SUBINDEX_URI)) {
                if (!(st.getObject() instanceof IRI)) {
                    throw new MalformedNanopubException("Sub-index has to be a URI");
                }
                subIndexSet.add((IRI) st.getObject());
            }
        }
        for (Statement st : np.getPubinfo()) {
            if (!st.getSubject().equals(np.getUri())) continue;
            if (!st.getPredicate().equals(RDF.TYPE)) continue;
            if (st.getObject().equals(NanopubIndex.INCOMPLETE_INDEX_URI)) {
                isIncompleteIndex = true;
            }
        }
        this.appendedIndex = appendedIndex;
        if (elementSet.size() + subIndexSet.size() > MAX_SIZE) {
            throw new MalformedNanopubException("Nanopub index exceeds maximum size");
        }
        this.elementSet = ImmutableSet.copyOf(elementSet);
        this.subIndexSet = ImmutableSet.copyOf(subIndexSet);
    }

    /**
     * Returns the URI of the nanopublication index.
     *
     * @return the URI of the nanopublication index
     */
    @Override
    public IRI getUri() {
        return np.getUri();
    }

    /**
     * Returns the URI of the head of the nanopublication.
     *
     * @return the URI of the head
     */
    @Override
    public IRI getHeadUri() {
        return np.getHeadUri();
    }

    /**
     * Returns the set of statements that form the head of the nanopublication.
     *
     * @return the set of statements in the head
     */
    @Override
    public Set<Statement> getHead() {
        return np.getHead();
    }

    /**
     * Returns the URI of the assertion information of the nanopublication.
     *
     * @return the URI of the assertion information
     */
    @Override
    public IRI getAssertionUri() {
        return np.getAssertionUri();
    }

    /**
     * Returns the set of statements that form the assertion of the nanopublication.
     *
     * @return the set of assertion statements
     */
    @Override
    public Set<Statement> getAssertion() {
        return np.getAssertion();
    }

    /**
     * Returns the URI of the provenance information of the nanopublication.
     *
     * @return the URI of the provenance information
     */
    @Override
    public IRI getProvenanceUri() {
        return np.getProvenanceUri();
    }

    /**
     * Returns the set of statements that provide provenance information about the nanopublication.
     *
     * @return the set of provenance statements
     */
    @Override
    public Set<Statement> getProvenance() {
        return np.getProvenance();
    }

    /**
     * Returns the URI of the publication information of the nanopublication.
     *
     * @return the URI of the publication information
     */
    @Override
    public IRI getPubinfoUri() {
        return np.getPubinfoUri();
    }

    /**
     * Returns the set of statements that provide publication information about the nanopublication.
     *
     * @return the set of publication information statements
     */
    @Override
    public Set<Statement> getPubinfo() {
        return np.getPubinfo();
    }

    @Override
    public Set<IRI> getGraphUris() {
        return np.getGraphUris();
    }

    /**
     * Returns the creation time of the nanopublication.
     *
     * @return a Calendar object representing the creation time
     */
    @Override
    public Calendar getCreationTime() {
        return np.getCreationTime();
    }

    /**
     * Returns a set of URIs that are defined as authors in the nanopublication.
     *
     * @return a set of IRI objects representing the authors
     */
    @Override
    public Set<IRI> getAuthors() {
        return np.getAuthors();
    }

    /**
     * Returns a set of URIs that are defined as creators in the nanopublication.
     *
     * @return a set of IRI objects representing the creators
     */
    @Override
    public Set<IRI> getCreators() {
        return np.getCreators();
    }

    /**
     * Returns the count of triples in the nanopublication.
     *
     * @return the number of triples in the nanopublication
     */
    @Override
    public int getTripleCount() {
        return np.getTripleCount();
    }

    @Override
    public long getByteCount() {
        return np.getByteCount();
    }

    /**
     * Returns a set of URIs that are included as elements in the nanopublication index.
     *
     * @return a set of IRI objects representing the elements
     */
    @Override
    public Set<IRI> getElements() {
        return elementSet;
    }

    /**
     * Returns a set of sub-indexes that this nanopublication index includes.
     *
     * @return a set of IRI objects representing the sub-indexes
     */
    @Override
    public Set<IRI> getSubIndexes() {
        return subIndexSet;
    }

    /**
     * Returns the URI of the index that this nanopublication appends to.
     *
     * @return the URI of the appended index, or null if this index does not append to another
     */
    @Override
    public IRI getAppendedIndex() {
        return appendedIndex;
    }

    /**
     * Checks if the nanopublication index is incomplete.
     *
     * @return true if the index is incomplete, false otherwise
     */
    @Override
    public boolean isIncomplete() {
        return isIncompleteIndex;
    }

    /**
     * Returns a list of namespace prefixes used in the nanopublication.
     *
     * @return a list of namespace prefixes as Strings, or an empty list if none are defined
     */
    @Override
    public List<String> getNsPrefixes() {
        if (np instanceof NanopubWithNs) {
            return ((NanopubWithNs) np).getNsPrefixes();
        } else {
            return ImmutableList.of();
        }
    }

    /**
     * Returns the namespace URI for a given prefix.
     *
     * @param prefix the prefix for which the namespace URI is requested
     * @return the namespace URI as a String, or null if the prefix is not defined
     */
    @Override
    public String getNamespace(String prefix) {
        if (np instanceof NanopubWithNs) {
            return ((NanopubWithNs) np).getNamespace(prefix);
        } else {
            return null;
        }
    }

    /**
     * Returns the name of the nanopublication, which is typically derived from the title statement in the pubinfo.
     *
     * @return the name as a String, or null if not found
     */
    @Override
    public String getName() {
        for (Statement st : np.getPubinfo()) {
            if (!st.getSubject().equals(np.getUri())) continue;
            if (!st.getPredicate().equals(DC.TITLE) || st.getPredicate().equals(DCTERMS.TITLE)) continue;
            if (!(st.getObject() instanceof Literal)) continue;
            return ((Literal) st.getObject()).getLabel();
        }
        return null;
    }

    /**
     * Returns the description of the nanopublication.
     *
     * @return the description as a String, or null if not found
     */
    @Override
    public String getDescription() {
        for (Statement st : np.getPubinfo()) {
            if (!st.getSubject().equals(np.getUri())) continue;
            if (!st.getPredicate().equals(DC.DESCRIPTION) || st.getPredicate().equals(DCTERMS.DESCRIPTION)) continue;
            if (!(st.getObject() instanceof Literal)) continue;
            return ((Literal) st.getObject()).getLabel();
        }
        return null;
    }

    /**
     * Returns a set of URIs that are referenced in the "seeAlso" statements of the nanopublication index.
     *
     * @return a set of IRI objects representing the "seeAlso" URIs
     */
    public Set<IRI> getSeeAlsoUris() {
        Set<IRI> seeAlsoUris = new HashSet<>();
        for (Statement st : getPubinfo()) {
            if (!st.getSubject().equals(getUri())) continue;
            if (!st.getPredicate().equals(RDFS.SEEALSO)) continue;
            if (!(st.getObject() instanceof IRI)) continue;
            seeAlsoUris.add((IRI) st.getObject());
        }
        return seeAlsoUris;
    }

    /**
     * Removes unused namespace prefixes from the nanopublication.
     */
    @Override
    public void removeUnusedPrefixes() {
        if (np instanceof NanopubWithNs) {
            ((NanopubWithNs) np).removeUnusedPrefixes();
        }
    }

    /**
     * Returns a map of namespace prefixes to their corresponding URIs used in the nanopublication.
     *
     * @return a map where keys are namespace prefixes and values are their URIs, or an empty map if none are defined
     */
    @Override
    public Map<String, String> getNs() {
        if (np instanceof NanopubWithNs) {
            return ((NanopubWithNs) np).getNs();
        } else {
            return ImmutableMap.of();
        }
    }
}
