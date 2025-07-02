package org.nanopub.fdo;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.nanopub.Nanopub;
import org.nanopub.NanopubCreator;
import org.nanopub.extra.security.MalformedCryptoElementException;
import org.nanopub.extra.security.SignatureUtils;
import org.nanopub.extra.security.TransformContext;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import static org.nanopub.Nanopub.SUPERSEDES;
import static org.nanopub.fdo.FdoUtils.DATA_REF_IRI;
import static org.nanopub.fdo.FdoUtils.FDO_URI_PREFIX;

/**
 * This class stores a changeable record of an FDO. It can come from an existing Handle-based FDO,
 * a nanopub-based one, or of an FDO that is still being created. The record may be viewed as a set of
 * RDF Statements (corresponding to  the assertion graph of an FDO nanopub). Internally it's represented as a
 * Map of tuples <IRI, Value>
 */
public class FdoRecord implements Serializable {

	private static final ValueFactory vf = SimpleValueFactory.getInstance();

	private IRI id = null;
	private final HashMap<IRI, Value> tuples = new HashMap<>();
	private final Set<IRI> aggregates = new HashSet<>();

	/** When teh FdoRecord is created out of a Nanopub, we store the originalNanopub, so we can supersed it. */
	private Nanopub originalNanopub = null;

	/**
	 * Constructor for building an FDO Record
	 * @param profile required, use complete url not only id
	 * @param label optional
	 * @param dataRef optional
	 */
	public FdoRecord (IRI profile, String label, IRI dataRef) {
		this.setAttribute(RDF.TYPE, FdoUtils.RDF_TYPE_FDO);
		this.setAttribute(FdoUtils.PROFILE_IRI, profile);
		if (label != null) {
			this.setAttribute(RDFS.LABEL, vf.createLiteral(label));
		}
		if (dataRef != null) {
			this.setAttribute(DATA_REF_IRI, dataRef);
		}
	}

	public FdoRecord(Nanopub np) {
		Statement anyAssertion = np.getAssertion().iterator().next();
		this.id = vf.createIRI(anyAssertion.getSubject().stringValue());
		for (Statement st: np.getAssertion()) {
			tuples.put(st.getPredicate(), st.getObject());
		}
		this.originalNanopub = np;
	}

	/** Build statements out of tuples, requires the id (fdoIri) to be set */
	public Set<Statement> buildStatements() {
		Set<Statement> statements = new HashSet<>();
		for (var entry: tuples.entrySet()) {
			statements.add(vf.createStatement(this.id, entry.getKey(), entry.getValue()));
		}
		if (tuples.containsKey(DATA_REF_IRI) && !aggregates.isEmpty()) {
			throw new RuntimeException("Complex FDOs cannot have DATA_REF");
		}
		for (IRI aggregate: aggregates) {
			statements.add(vf.createStatement(this.id, FdoUtils.FDO_HAS_PART, aggregate));
		}
		return statements;
	}


	/**
	 * @return the value of the attribute, or null
	 */
	public Value getAttribute(IRI iri) {
		return tuples.get(iri);
	}

	/**
	 * Set the attribute. If the key (iri) was already there, the old value is removed.
	 * @return the FdoRecord for chaining
	 */
	public FdoRecord setAttribute(IRI iri, Value val) {
		tuples.put(iri, val);
		return this;
	}

	/**
	 * Remove the attribute.
	 * @return the FdoRecord for chaining
	 */
	public FdoRecord removeAttribute(IRI iri) {
		tuples.remove(iri);
		return this;
	}

	public String getProfile() {
		String profile = tuples.get(FdoUtils.PROFILE_IRI).stringValue();
		return profile;
	}

	public String getLabel() {
		Value label = tuples.get(RDFS.LABEL);
		if (label != null) {
			return label.stringValue();
		}
		return null;
	}

	public String getSchemaUrl() {
		Value schemaEntry = tuples.get(vf.createIRI(FDO_URI_PREFIX + "21.T11966/JsonSchema"));
		if (schemaEntry != null) {
			// assume the entry looks like {"$ref": "https://the-url"}
			String url = schemaEntry.stringValue().substring(10, schemaEntry.stringValue().length() - 2);
			return url;
		}
		return null;
	}

	public IRI getId() {
		return id;
	}

	public void setId(IRI id) {
		this.id = id;
	}

	public void setDataRef(String dataRef) {
		tuples.put(FdoUtils.DATA_REF_IRI, vf.createIRI(dataRef));
	}

	public Value getDataRef() {
		return tuples.get(FdoUtils.DATA_REF_IRI);
	}

	public void addAggregatedFdo(IRI fdoUri) {
		aggregates.add(fdoUri);
	}

	public NanopubCreator createUpdatedNanopub() throws MalformedCryptoElementException {
		return createUpdatedNanopub(TransformContext.makeDefault());
	}

	public NanopubCreator createUpdatedNanopub(TransformContext tc) throws MalformedCryptoElementException {
		if (originalNanopub == null) {
			throw new MalformedCryptoElementException("There is no original nanopub to update.");
		}
		String oldPubKey = SignatureUtils.getSignatureElement(originalNanopub).getPublicKeyString();
		String newPubKey = SignatureUtils.encodePublicKey(tc.getKey().getPublic());
		if (!oldPubKey.equals(newPubKey)) {
			throw new MalformedCryptoElementException("The old public key does not match the new public key");
		}
		NanopubCreator creator = FdoNanopubCreator.createWithFdoIri(this, this.getId());
		IRI assertionUri = creator.getAssertionUri();
		for (Statement st : originalNanopub.getProvenance()) {
			creator.addProvenanceStatement(assertionUri, st.getPredicate(), st.getObject());
		}
		creator.addPubinfoStatement(SUPERSEDES, originalNanopub.getUri());
		return creator;
	}

	/**
	 * If this FdoRecord was created by a Nanopub, we return that Nanopub.
	 * null otherwise.
	 */
	public Nanopub getOriginalNanopub() {
		return originalNanopub;
	}

}
