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
import org.nanopub.vocabulary.NPX;

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
     * @throws org.nanopub.MalformedNanopubException if the Nanopub does not conform to the expected structure of an index
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
            if (st.getPredicate().equals(NPX.APPENDS_INDEX)) {
                if (appendedIndex != null) {
                    throw new MalformedNanopubException("Multiple appends-statements found for index");
                }
                if (!(st.getObject() instanceof IRI)) {
                    throw new MalformedNanopubException("URI expected for object of appends-statement");
                }
                appendedIndex = (IRI) st.getObject();
            } else if (st.getPredicate().equals(NPX.INCLUDES_ELEMENT)) {
                if (!(st.getObject() instanceof IRI)) {
                    throw new MalformedNanopubException("Element has to be a URI");
                }
                elementSet.add((IRI) st.getObject());
            } else if (st.getPredicate().equals(NPX.INCLUDES_SUBINDEX)) {
                if (!(st.getObject() instanceof IRI)) {
                    throw new MalformedNanopubException("Sub-index has to be a URI");
                }
                subIndexSet.add((IRI) st.getObject());
            }
        }
        for (Statement st : np.getPubinfo()) {
            if (!st.getSubject().equals(np.getUri())) continue;
            if (!st.getPredicate().equals(RDF.TYPE)) continue;
            if (st.getObject().equals(NPX.INCOMPLETE_INDEX)) {
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
     * {@inheritDoc}
     * <p>
     * Returns the URI of the nanopublication index.
     */
    @Override
    public IRI getUri() {
        return np.getUri();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns the URI of the head of the nanopublication.
     */
    @Override
    public IRI getHeadUri() {
        return np.getHeadUri();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns the set of statements that form the head of the nanopublication.
     */
    @Override
    public Set<Statement> getHead() {
        return np.getHead();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns the URI of the assertion information of the nanopublication.
     */
    @Override
    public IRI getAssertionUri() {
        return np.getAssertionUri();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns the set of statements that form the assertion of the nanopublication.
     */
    @Override
    public Set<Statement> getAssertion() {
        return np.getAssertion();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns the URI of the provenance information of the nanopublication.
     */
    @Override
    public IRI getProvenanceUri() {
        return np.getProvenanceUri();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns the set of statements that provide provenance information about the nanopublication.
     */
    @Override
    public Set<Statement> getProvenance() {
        return np.getProvenance();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns the URI of the publication information of the nanopublication.
     */
    @Override
    public IRI getPubinfoUri() {
        return np.getPubinfoUri();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns the set of statements that provide publication information about the nanopublication.
     */
    @Override
    public Set<Statement> getPubinfo() {
        return np.getPubinfo();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<IRI> getGraphUris() {
        return np.getGraphUris();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns the creation time of the nanopublication.
     */
    @Override
    public Calendar getCreationTime() {
        return np.getCreationTime();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns a set of URIs that are defined as authors in the nanopublication.
     */
    @Override
    public Set<IRI> getAuthors() {
        return np.getAuthors();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns a set of URIs that are defined as creators in the nanopublication.
     */
    @Override
    public Set<IRI> getCreators() {
        return np.getCreators();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns the count of triples in the nanopublication.
     */
    @Override
    public int getTripleCount() {
        return np.getTripleCount();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getByteCount() {
        return np.getByteCount();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns a set of URIs that are included as elements in the nanopublication index.
     */
    @Override
    public Set<IRI> getElements() {
        return elementSet;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns a set of sub-indexes that this nanopublication index includes.
     */
    @Override
    public Set<IRI> getSubIndexes() {
        return subIndexSet;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns the URI of the index that this nanopublication appends to.
     */
    @Override
    public IRI getAppendedIndex() {
        return appendedIndex;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Checks if the nanopublication index is incomplete.
     */
    @Override
    public boolean isIncomplete() {
        return isIncompleteIndex;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns a list of namespace prefixes used in the nanopublication.
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
     * {@inheritDoc}
     * <p>
     * Returns the namespace URI for a given prefix.
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
     * {@inheritDoc}
     * <p>
     * Returns the name of the nanopublication, which is typically derived from the title statement in the pubinfo.
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
     * {@inheritDoc}
     * <p>
     * Returns the description of the nanopublication.
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
     * {@inheritDoc}
     * <p>
     * Removes unused namespace prefixes from the nanopublication.
     */
    @Override
    public void removeUnusedPrefixes() {
        if (np instanceof NanopubWithNs) {
            ((NanopubWithNs) np).removeUnusedPrefixes();
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns a map of namespace prefixes to their corresponding URIs used in the nanopublication.
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
