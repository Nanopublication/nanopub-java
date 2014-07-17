package org.nanopub.extra.index;

import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.nanopub.MalformedNanopubException;
import org.nanopub.Nanopub;
import org.nanopub.NanopubWithNs;
import org.openrdf.model.Literal;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.vocabulary.DC;
import org.openrdf.model.vocabulary.DCTERMS;
import org.openrdf.model.vocabulary.RDF;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

public class NanopubIndexImpl implements NanopubIndex, NanopubWithNs {

	private final Nanopub np;
	private final Set<URI> elementSet;
	private final Set<URI> subIndexSet;
	private final URI appendedIndex;
	private boolean isIncompleteIndex = false;

	protected NanopubIndexImpl(Nanopub npIndex) throws MalformedNanopubException {
		this.np = npIndex;
		if (!IndexUtils.isIndex(np)) {
			throw new MalformedNanopubException("Nanopub is not a nanopub index");
		}
		URI appendedIndex = null;
		Set<URI> elementSet = new HashSet<URI>();
		Set<URI> subIndexSet = new HashSet<URI>();
		for (Statement st : np.getAssertion()) {
			if (!st.getSubject().equals(np.getUri())) continue;
			if (st.getPredicate().equals(NanopubIndex.APPENDS_INDEX_URI)) {
				if (appendedIndex != null) {
					throw new MalformedNanopubException("Multiple appends-statements found for index");
				}
				if (!(st.getObject() instanceof URI)) {
					throw new MalformedNanopubException("URI expected for object of appends-statement");
				}
				appendedIndex = (URI) st.getObject();
			} else if (st.getPredicate().equals(NanopubIndex.INCLUDES_ELEMENT_URI)) {
				if (!(st.getObject() instanceof URI)) {
					throw new MalformedNanopubException("Element has to be a URI");
				}
				elementSet.add((URI) st.getObject());
			} else if (st.getPredicate().equals(NanopubIndex.INCLUDES_SUBINDEX_URI)) {
				if (!(st.getObject() instanceof URI)) {
					throw new MalformedNanopubException("Sub-index has to be a URI");
				}
				subIndexSet.add((URI) st.getObject());
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
	public URI getUri() {
		return np.getUri();
	}

	@Override
	public URI getHeadUri() {
		return np.getHeadUri();
	}

	@Override
	public Set<Statement> getHead() {
		return np.getHead();
	}

	@Override
	public URI getAssertionUri() {
		return np.getAssertionUri();
	}

	@Override
	public Set<Statement> getAssertion() {
		return np.getAssertion();
	}

	@Override
	public URI getProvenanceUri() {
		return np.getProvenanceUri();
	}

	@Override
	public Set<Statement> getProvenance() {
		return np.getProvenance();
	}

	@Override
	public URI getPubinfoUri() {
		return np.getPubinfoUri();
	}

	@Override
	public Set<Statement> getPubinfo() {
		return np.getPubinfo();
	}

	@Override
	public Set<URI> getGraphUris() {
		return np.getGraphUris();
	}

	@Override
	public Calendar getCreationTime() {
		return np.getCreationTime();
	}

	@Override
	public Set<URI> getAuthors() {
		return np.getAuthors();
	}

	@Override
	public Set<URI> getCreators() {
		return np.getCreators();
	}

	@Override
	public Set<URI> getElements() {
		return elementSet;
	}

	@Override
	public Set<URI> getSubIndexes() {
		return subIndexSet;
	}

	@Override
	public URI getAppendedIndex() {
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

	@Override
	public void removeUnusedPrefixes() {
		if (np instanceof NanopubWithNs) {
			((NanopubWithNs) np).removeUnusedPrefixes();
		}
	}

}
