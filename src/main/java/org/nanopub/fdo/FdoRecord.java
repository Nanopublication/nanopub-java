package org.nanopub.fdo;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.PROV;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.nanopub.Nanopub;
import org.nanopub.NanopubAlreadyFinalizedException;
import org.nanopub.NanopubCreator;
import org.nanopub.extra.security.MalformedCryptoElementException;
import org.nanopub.extra.security.SignatureUtils;
import org.nanopub.extra.security.TransformContext;
import org.nanopub.vocabulary.FDOF;
import org.nanopub.vocabulary.HDL;
import org.nanopub.vocabulary.NPX;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * This class stores a changeable record of an FDO. It can come from an existing Handle-based FDO,
 * a nanopub-based one, or of an FDO that is still being created. The record may be viewed as a set of
 * RDF Statements (corresponding to  the assertion graph of an FDO nanopub). Internally it's represented as a
 * Map of tuples (IRI, Value) where the IRI is the predicate and the Value is the object.
 */
public class FdoRecord implements Serializable {

    private static final ValueFactory vf = SimpleValueFactory.getInstance();
    final static String SCHEMA_ID = "21.T11966/JsonSchema";

    private IRI id = null;
    private final HashMap<IRI, Value> tuples = new HashMap<>();
    private final Set<IRI> aggregates = new HashSet<>();
    private final Set<IRI> derivesFrom = new HashSet<>();

    /**
     * When the FdoRecord is created out of a Nanopub, we store the originalNanopub, so we can supersed it.
     */
    private Nanopub originalNanopub = null;

    /**
     * Constructor for building an FDO Record
     *
     * @param profile required, use complete url not only id
     * @param label   optional
     * @param dataRef optional
     */
    public FdoRecord(IRI profile, String label, IRI dataRef) {
        this.setAttribute(RDF.TYPE, FDOF.FAIR_DIGITAL_OBJECT);
        this.setAttribute(DCTERMS.CONFORMS_TO, profile);
        if (label != null) {
            this.setAttribute(RDFS.LABEL, vf.createLiteral(label));
        }
        if (dataRef != null) {
            this.setAttribute(FDOF.IS_MATERIALIZED_BY, dataRef);
        }
    }

    /**
     * Constructor for building an FDO Record from a Nanopub.
     *
     * @param np the Nanopub to create the FdoRecord from
     */
    public FdoRecord(Nanopub np) {
        Statement anyAssertion = np.getAssertion().iterator().next();
        this.id = vf.createIRI(anyAssertion.getSubject().stringValue());
        for (Statement st : np.getAssertion()) {
            tuples.put(st.getPredicate(), st.getObject());
        }
        this.originalNanopub = np;
    }

    /**
     * Build statements out of tuples.
     *
     * @return a Set of RDF Statements representing this FdoRecord
     */
    public Set<Statement> buildStatements() {
        if (id == null) {
            throw new RuntimeException("FdoRecord id (fdoIri) must be set before building statements");
        }
        Set<Statement> statements = new HashSet<>();
        for (var entry : tuples.entrySet()) {
            statements.add(vf.createStatement(this.id, entry.getKey(), entry.getValue()));
        }
        if (tuples.containsKey(FDOF.IS_MATERIALIZED_BY) && !aggregates.isEmpty()) {
            throw new RuntimeException("Complex FDOs cannot have DATA_REF");
        }
        for (IRI aggregate : aggregates) {
            statements.add(vf.createStatement(this.id, DCTERMS.HAS_PART, aggregate));
        }
        for (IRI derive : derivesFrom) {
            statements.add(vf.createStatement(this.id, PROV.WAS_DERIVED_FROM, derive));
        }
        return statements;
    }


    /**
     * Get the value of the attribute with the given IRI.
     *
     * @param iri the IRI of the attribute to get, must not be null
     * @return the value of the attribute, or null
     */
    public Value getAttribute(IRI iri) {
        return tuples.get(iri);
    }

    /**
     * Set the attribute. If the key (iri) was already there, the old value is removed.
     *
     * @param iri the IRI of the attribute to set, must not be null
     * @param val the Value to set for the attribute, must not be null
     * @return the FdoRecord for chaining
     */
    public FdoRecord setAttribute(IRI iri, Value val) {
        tuples.put(iri, val);
        return this;
    }

    /**
     * Remove the attribute.
     *
     * @param iri the IRI of the attribute to remove
     * @return the FdoRecord for chaining
     */
    public FdoRecord removeAttribute(IRI iri) {
        tuples.remove(iri);
        return this;
    }

    /**
     * Get the profile IRI of this FDO.
     *
     * @return the profile IRI as a String
     */
    public String getProfile() {
        return tuples.get(DCTERMS.CONFORMS_TO).stringValue();
    }

    /**
     * Get the label of this FDO record.
     *
     * @return the label as a String, or null if not set
     */
    public String getLabel() {
        Value label = tuples.get(RDFS.LABEL);
        if (label != null) {
            return label.stringValue();
        }
        return null;
    }

    /**
     * Get the URL of the schema for this FDO record.
     *
     * @return the schema URL as a String, or null if not set
     */
    public String getSchemaUrl() {
        Value schemaEntry = tuples.get(vf.createIRI(HDL.NAMESPACE + SCHEMA_ID));
        if (schemaEntry != null) {
            // TODO check if the schemaEntry is a valid JSON Schema reference
            // assume the entry looks like {"$ref": "https://the-url"}
            return schemaEntry.stringValue().substring(10, schemaEntry.stringValue().length() - 2);
        }
        return null;
    }

    /**
     * Get the IRI of this FDO record.
     *
     * @return the IRI of this FDO record, or null if not set
     */
    public IRI getId() {
        return id;
    }

    /**
     * Set the IRI of this FDO record.
     *
     * @param id the IRI to set, must not be null
     */
    public void setId(IRI id) {
        this.id = id;
    }

    /**
     * Set the data reference of this FDO record.
     *
     * @param dataRef the data reference IRI as a String, must not be null
     */
    public void setDataRef(String dataRef) {
        tuples.put(FDOF.IS_MATERIALIZED_BY, vf.createIRI(dataRef));
    }

    /**
     * Get the data reference Value of this FDO record.
     *
     * @return the data reference Value, or null if not set
     */
    public Value getDataRef() {
        return tuples.get(FDOF.IS_MATERIALIZED_BY);
    }

    /**
     * Add an aggregated FDO by its URI or Handle to this FDO record.
     *
     * @param uriOrHandle the URI or Handle of the aggregated FDO, must not be null
     */
    public void addAggregatedFdo(String uriOrHandle) {
        if (FdoUtils.looksLikeUrl(uriOrHandle)) {
            aggregates.add(vf.createIRI(uriOrHandle));
        } else if (FdoUtils.looksLikeHandle(uriOrHandle)) {
            aggregates.add(FdoUtils.toIri(uriOrHandle));
        } else {
            throw new RuntimeException("uriOrHandle is neither uri nor handle: " + uriOrHandle);
        }
    }

    /**
     * Add derived from FDO by its URI to this FDO record.
     *
     * @param fdoUri the URI of the FDO that this record derives from, must not be null
     */
    public void addDerivedFromFdo(IRI fdoUri) {
        derivesFrom.add(fdoUri);
    }

    /**
     * Create a new NanopubCreator for this FdoRecord, which can be used to create a new Nanopub.
     *
     * @return a NanopubCreator for this FdoRecord
     * @throws org.nanopub.extra.security.MalformedCryptoElementException if the original Nanopub is not set or does not match the public key
     */
    public NanopubCreator createUpdatedNanopub() throws MalformedCryptoElementException, NanopubAlreadyFinalizedException {
        return createUpdatedNanopub(TransformContext.makeDefault());
    }

    /**
     * Create a new NanopubCreator for this FdoRecord, which can be used to create a new Nanopub.
     *
     * @param tc the TransformContext to use for the Nanopub creation, must not be null
     * @return a NanopubCreator for this FdoRecord
     * @throws org.nanopub.extra.security.MalformedCryptoElementException if the original Nanopub is not set or does not match the public key
     */
    public NanopubCreator createUpdatedNanopub(TransformContext tc) throws MalformedCryptoElementException, NanopubAlreadyFinalizedException {
        if (originalNanopub == null) {
            throw new MalformedCryptoElementException("There is no original nanopub to update.");
        }
        SignatureUtils.assertMatchingPubkeys(tc, originalNanopub);
        NanopubCreator creator = FdoNanopubCreator.createWithFdoIri(this, this.getId());
        IRI assertionUri = creator.getAssertionUri();
        for (Statement st : originalNanopub.getProvenance()) {
            creator.addProvenanceStatement(assertionUri, st.getPredicate(), st.getObject());
        }
        creator.addPubinfoStatement(NPX.SUPERSEDES, originalNanopub.getUri());
        return creator;
    }

    /**
     * If this FdoRecord was created by a Nanopub, we return that Nanopub.
     * null otherwise.
     *
     * @return the original Nanopub, or null if this FdoRecord was not created from a Nanopub
     */
    public Nanopub getOriginalNanopub() {
        return originalNanopub;
    }

}
