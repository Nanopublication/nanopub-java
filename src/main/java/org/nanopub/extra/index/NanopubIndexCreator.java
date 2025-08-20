package org.nanopub.extra.index;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.*;
import org.eclipse.rdf4j.rio.turtle.TurtleUtil;
import org.nanopub.Nanopub;
import org.nanopub.NanopubCreator;
import org.nanopub.trusty.TempUriReplacer;
import org.nanopub.vocabulary.NP;
import org.nanopub.vocabulary.NPX;
import org.nanopub.vocabulary.PAV;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * This class is used to create nanopublication indexes.
 */
public abstract class NanopubIndexCreator {

    private IRI completeIndexUri = null;
    private boolean finalized = false;

    private NanopubCreator npCreator;
    private int itemCount;
    private Map<String, Integer> elementNs;
    private int elementNsCount;
    private IRI previousIndexUri;
    private IRI supersededIndexUri;
    private boolean makeTrusty;
    private Random random = new Random();

    /**
     * Creates a new NanopubIndexCreator with the option to make the nanopublications trusty.
     *
     * @param makeTrusty If true, the created nanopublications will be trusty.
     */
    public NanopubIndexCreator(boolean makeTrusty) {
        this(null, makeTrusty);
    }

    /**
     * Creates a new NanopubIndexCreator with the option to make the nanopublications trusty and
     * the previous index URI.
     *
     * @param previousIndexUri The URI of a previous index.
     * @param makeTrusty       If true, the created nanopublications will be trusty.
     */
    public NanopubIndexCreator(IRI previousIndexUri, boolean makeTrusty) {
        this.previousIndexUri = previousIndexUri;
        this.makeTrusty = makeTrusty;
    }

    /**
     * Adds a nanopublication.
     *
     * @param np The nanopublication to add.
     */
    public void addElement(Nanopub np) {
        addElement(np.getUri());
    }

    /**
     * Adds a nanopublication by its URI.
     *
     * @param npUri The URI of the nanopublication to add.
     */
    public void addElement(IRI npUri) {
        if (finalized) throw new RuntimeException("Already finalized");
        if (npCreator == null || itemCount >= NanopubIndex.MAX_SIZE) {
            newNpCreator();
        }
        itemCount++;
        npCreator.addAssertionStatement(npCreator.getNanopubUri(), NPX.INCLUDES_ELEMENT, npUri);
        int nsSplit = TurtleUtil.findURISplitIndex(npUri.toString());
        if (nsSplit > 0) {
            String ns = npUri.toString().substring(0, nsSplit);
            if (!elementNs.containsKey(ns)) {
                elementNs.put(ns, 1);
            } else if (elementNs.get(ns) == 1) {
                // only add namespace if at least 2 elements share it
                elementNsCount++;
                npCreator.addNamespace("ns" + elementNsCount, ns);
                elementNs.put(ns, 2);
            }
        }
    }

    /**
     * Adds a sub-index to the current index.
     *
     * @param npc The nanopublication index to add as a sub-index.
     */
    public void addSubIndex(NanopubIndex npc) {
        addSubIndex(npc.getUri());
    }

    /**
     * Adds a sub-index to the current index by its URI.
     *
     * @param npcUri The URI of the nanopublication index to add as a sub-index.
     */
    public void addSubIndex(IRI npcUri) {
        if (finalized) throw new RuntimeException("Already finalized");
        if (npCreator == null || itemCount >= NanopubIndex.MAX_SIZE) {
            newNpCreator();
        }
        itemCount++;
        npCreator.addAssertionStatement(npCreator.getNanopubUri(), NPX.INCLUDES_SUBINDEX, npcUri);
    }

    /**
     * Sets the superseded index URI for the current index.
     *
     * @param npc The nanopublication index that is superseded.
     */
    public void setSupersededIndex(NanopubIndex npc) {
        setSupersededIndex(npc.getUri());
    }

    /**
     * Sets the superseded index URI for the current index by its URI.
     *
     * @param npcUri The URI of the nanopublication index that is superseded.
     */
    public void setSupersededIndex(IRI npcUri) {
        if (finalized) throw new RuntimeException("Already finalized");
        supersededIndexUri = npcUri;
    }

    /**
     * Finalizes the nanopublication, making it immutable and ready for use.
     */
    public void finalizeNanopub() {
        if (finalized) throw new RuntimeException("Already finalized");
        if (npCreator == null) {
            newNpCreator();
        }
        if (supersededIndexUri != null) {
            npCreator.addPubinfoStatement(npCreator.getNanopubUri(), NPX.SUPERSEDES, supersededIndexUri);
        }
        enrichCompleteIndex(npCreator);
        try {
            Nanopub np;
            if (makeTrusty) {
                np = npCreator.finalizeTrustyNanopub(true);
            } else {
                np = npCreator.finalizeNanopub(true);
            }
            completeIndexUri = np.getUri();
            handleCompleteIndex(IndexUtils.castToIndex(np));
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        finalized = true;
    }

    /**
     * Returns the URI of the complete index nanopublication.
     *
     * @return The URI of the complete index nanopublication, or null if not finalized.
     */
    public IRI getCompleteIndexUri() {
        return completeIndexUri;
    }

    /**
     * This method should return the base URI of the nanopublication index. This should ideally
     * be the URL of a nanopub server, such as <a href="http://np.inn.ac/">...</a>, which makes the resulting
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
     *                  a NanopubCreator object.
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
     *                  a NanopubCreator object.
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

    /**
     * This method is called to create a new nanopublication index. It finalizes the existing
     * index nanopub (if any) and initializes a new one.
     */
    private void newNpCreator() {
        // Finalize existing index nanopub:
        if (npCreator != null) {
            npCreator.addPubinfoStatement(RDF.TYPE, NPX.INCOMPLETE_INDEX);
            enrichIncompleteIndex(npCreator);
            try {
                Nanopub np;
                if (makeTrusty) {
                    np = npCreator.finalizeTrustyNanopub(true);
                } else {
                    np = npCreator.finalizeNanopub(true);
                }
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
        String baseUri = getBaseUri();
        if (baseUri.startsWith(TempUriReplacer.tempUri)) {
            baseUri += Math.abs(random.nextLong()) + "/";
        }
        npCreator = new NanopubCreator(baseUri);
        npCreator.addNamespace("", baseUri);
        npCreator.addNamespace(RDF.NS);
        npCreator.addNamespace(RDFS.NS);
        npCreator.addNamespace(XSD.NS);
        npCreator.addNamespace(OWL.NS);
        npCreator.addNamespace("dct", DCTERMS.NAMESPACE);
        npCreator.addNamespace("dce", DC.NAMESPACE);
        npCreator.addNamespace(PAV.PREFIX, PAV.NAMESPACE);
        npCreator.addNamespace(NP.PREFIX, NP.NAMESPACE);
        npCreator.addNamespace(NPX.PREFIX, NPX.NAMESPACE);
        npCreator.addProvenanceStatement(RDF.TYPE, NPX.INDEX_ASSERTION);
        npCreator.addPubinfoStatement(RDF.TYPE, NPX.NANOPUB_INDEX);
        if (previousIndexUri != null) {
            npCreator.addAssertionStatement(npCreator.getNanopubUri(), NPX.APPENDS_INDEX, previousIndexUri);
        }
    }

}
