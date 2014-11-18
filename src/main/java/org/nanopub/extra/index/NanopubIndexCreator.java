package org.nanopub.extra.index;

import java.util.HashMap;
import java.util.Map;

import org.nanopub.Nanopub;
import org.nanopub.NanopubCreator;
import org.openrdf.model.URI;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.rio.turtle.TurtleUtil;

public abstract class NanopubIndexCreator {

	private URI completeIndexUri = null;
	private boolean finalized = false;

	private NanopubCreator npCreator;
	private int itemCount;
	private Map<String,Boolean> elementNs;
	private int elementNsCount;

	public NanopubIndexCreator() {
	}

	public void addElement(Nanopub np) {
		addElement(np.getUri());
	}

	public void addElement(URI npUri) {
		if (finalized) throw new RuntimeException("Already finalized");
		if (npCreator == null || itemCount >= NanopubIndex.MAX_SIZE) {
			newNpCreator();
		}
		itemCount++;
		npCreator.addAssertionStatement(npCreator.getNanopubUri(), NanopubIndex.INCLUDES_ELEMENT_URI, npUri);
		int nsSplit = TurtleUtil.findURISplitIndex(npUri.toString());
		if (nsSplit > 0) {
			String ns = npUri.toString().substring(0, nsSplit);
			if (!elementNs.containsKey(ns)) {
				elementNsCount++;
				npCreator.addNamespace("ns" + elementNsCount, ns);
				elementNs.put(ns, true);
			}
		}
	}

	public void addSubIndex(NanopubIndex npc) {
		addSubIndex(npc.getUri());
	}

	public void addSubIndex(URI npcUri) {
		if (finalized) throw new RuntimeException("Already finalized");
		if (npCreator == null || itemCount >= NanopubIndex.MAX_SIZE) {
			newNpCreator();
		}
		itemCount++;
		npCreator.addAssertionStatement(npCreator.getNanopubUri(), NanopubIndex.INCLUDES_SUBINDEX_URI, npcUri);
	}

	public void finalizeNanopub() {
		if (finalized) throw new RuntimeException("Already finalized");
		enrichCompleteIndex(npCreator);
		try {
			Nanopub np = npCreator.finalizeTrustyNanopub(true);
			completeIndexUri = np.getUri();
			handleCompleteIndex(IndexUtils.castToIndex(np));
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
		finalized = true;
	}

	public URI getCompleteIndexUri() {
		return completeIndexUri;
	}

	/**
	 * This method should return the base URI of the nanopublication index. This should ideally
	 * be the URL of a nanopub server, such as http://np.inn.ac/, which makes the resulting
	 * trusty URIs resolvable.
	 *
	 * @return The base URI as a string.
	 */
	public abstract String getBaseUri();

	/**
	 * This method gives access to the creation of "incomplete" index nanopublications before they
	 * are finalized (and thereby made immutable). This method is useful to add publication info
	 * and provenance triples. Incomplete indexes are the ones that are appended by other indexes,
	 * and for that reason do not stand for a meaningful set on their own. Provenance and
	 * publication information is less important for incomplete indexes than for complete ones,
	 * and this method can also be ignored completely (i.e. left empty).
	 *
	 * @param npCreator Access to a partially created incomplete nanopublication in the form of
	 *     a NanopubCreator object.
	 */
	public abstract void enrichIncompleteIndex(NanopubCreator npCreator);

	/**
	 * This method gives access to the creation of the "complete" index nanopublications before it
	 * is finalized (and thereby made immutable). This method is useful to add publication info
	 * and provenance triples. Complete indexes are the ones that stand for a meaningful set on
	 * their own and are typically not appended by other indexes. As the nanopublications
	 * themselves have their own provenance and publication information, it can be sufficient
	 * give only minimal additional information here.
	 *
	 * @param npCreator Access to the partially created complete nanopublication in the form of
	 *     a NanopubCreator object.
	 */
	public abstract void enrichCompleteIndex(NanopubCreator npCreator);

	/**
	 * With this method, newly created "incomplete" nanopublication indexes are announced.
	 * Incomplete indexes are appended by other (incomplete or complete) indexes. This method
	 * can forward them or write them to a file.
	 *
	 * @param npi The newly created incomplete nanopublication index.
	 */
	public abstract void handleIncompleteIndex(NanopubIndex npi);

	/**
	 * With this method, the newly created "complete" nanopublication indexe is announced.
	 * This complete index is the final one standing for the entire set. This method can forward
	 * it or write it to a file. Note that all incomplete indexes need to be available in order
	 * to make sense of a complete index.
	 *
	 * @param npi The newly created complete nanopublication index.
	 */
	public abstract void handleCompleteIndex(NanopubIndex npi);

	private void newNpCreator() {
		// Finalize existing index nanopub:
		URI previousIndexUri = null;
		if (npCreator != null) {
			npCreator.addPubinfoStatement(RDF.TYPE, NanopubIndex.INCOMPLETE_INDEX_URI);
			enrichIncompleteIndex(npCreator);
			try {
				Nanopub np = npCreator.finalizeTrustyNanopub(true);
				previousIndexUri = np.getUri();
				handleIncompleteIndex(IndexUtils.castToIndex(np));
			} catch (Exception ex) {
				throw new RuntimeException(ex);
			}
		}
		// Initialize new index nanopub:
		elementNs = new HashMap<>();
		elementNsCount = 0;
		itemCount = 0;
		npCreator = new NanopubCreator(getBaseUri());
		npCreator.addNamespace("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
		npCreator.addNamespace("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
		npCreator.addNamespace("xsd", "http://www.w3.org/2001/XMLSchema#");
		npCreator.addNamespace("owl", "http://www.w3.org/2002/07/owl#");
		npCreator.addNamespace("dc", "http://purl.org/dc/terms/");
		npCreator.addNamespace("pav", "http://purl.org/pav/");
		npCreator.addNamespace("np", "http://www.nanopub.org/nschema#");
		npCreator.addNamespace("npx", "http://purl.org/nanopub/x/");
		npCreator.addProvenanceStatement(RDF.TYPE, NanopubIndex.INDEX_ASSERTION_URI);
		npCreator.addPubinfoStatement(RDF.TYPE, NanopubIndex.NANOPUB_INDEX_URI);
		if (previousIndexUri != null) {
			npCreator.addAssertionStatement(npCreator.getNanopubUri(), NanopubIndex.APPENDS_INDEX_URI, previousIndexUri);
		}
	}

}
