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

public class NanopubIndexImpl implements NanopubIndex, NanopubWithNs {

	private final Nanopub np;
	private final Set<IRI> elementSet;
	private final Set<IRI> subIndexSet;
	private final IRI appendedIndex;
	private boolean isIncompleteIndex = false;

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

	@Override
	public IRI getUri() {
		return np.getUri();
	}

	@Override
	public IRI getHeadUri() {
		return np.getHeadUri();
	}

	@Override
	public Set<Statement> getHead() {
		return np.getHead();
	}

	@Override
	public IRI getAssertionUri() {
		return np.getAssertionUri();
	}

	@Override
	public Set<Statement> getAssertion() {
		return np.getAssertion();
	}

	@Override
	public IRI getProvenanceUri() {
		return np.getProvenanceUri();
	}

	@Override
	public Set<Statement> getProvenance() {
		return np.getProvenance();
	}

	@Override
	public IRI getPubinfoUri() {
		return np.getPubinfoUri();
	}

	@Override
	public Set<Statement> getPubinfo() {
		return np.getPubinfo();
	}

	@Override
	public Set<IRI> getGraphUris() {
		return np.getGraphUris();
	}

	@Override
	public Calendar getCreationTime() {
		return np.getCreationTime();
	}

	@Override
	public Set<IRI> getAuthors() {
		return np.getAuthors();
	}

	@Override
	public Set<IRI> getCreators() {
		return np.getCreators();
	}

	@Override
	public int getTripleCount() {
		return np.getTripleCount();
	}

	@Override
	public long getByteCount() {
		return np.getByteCount();
	}

    @Override
	public Set<IRI> getElements() {
		return elementSet;
	}

	@Override
	public Set<IRI> getSubIndexes() {
		return subIndexSet;
	}

	@Override
	public IRI getAppendedIndex() {
		return appendedIndex;
	}

	@Override
	public boolean isIncomplete() {
		return isIncompleteIndex;
	}

	@Override
	public List<String> getNsPrefixes() {
		if (np instanceof NanopubWithNs) {
			return ((NanopubWithNs) np).getNsPrefixes();
		} else {
			return ImmutableList.of();
		}
	}

	@Override
	public String getNamespace(String prefix) {
		if (np instanceof NanopubWithNs) {
			return ((NanopubWithNs) np).getNamespace(prefix);
		} else {
			return null;
		}
	}

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

	@Override
	public void removeUnusedPrefixes() {
		if (np instanceof NanopubWithNs) {
			((NanopubWithNs) np).removeUnusedPrefixes();
		}
	}

	@Override
	public Map<String, String> getNs() {
		if (np instanceof NanopubWithNs) {
			return ((NanopubWithNs) np).getNs();
		} else {
			return ImmutableMap.of();
		}
	}
}
