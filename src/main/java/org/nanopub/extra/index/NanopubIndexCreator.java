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
		npCreator.addAssertionStatement(npCreator.getNanopubUri(), NanopubIndex.INCLUDES_URI, npUri);
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
		npCreator.addAssertionStatement(npCreator.getNanopubUri(), NanopubIndex.INCLUDES_ALL_URI, npcUri);
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

	public abstract String getBaseUri();

	public abstract void enrichIncompleteIndex(NanopubCreator npCreator);

	public abstract void enrichCompleteIndex(NanopubCreator npCreator);

	public abstract void handleIncompleteIndex(NanopubIndex npc);

	public abstract void handleCompleteIndex(NanopubIndex npc);

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
			npCreator.addAssertionStatement(npCreator.getNanopubUri(), NanopubIndex.APPENDS_URI, previousIndexUri);
		}
	}

}
