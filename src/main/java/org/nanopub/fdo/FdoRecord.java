package org.nanopub.fdo;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.nanopub.Nanopub;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import static org.nanopub.fdo.FdoNanopubCreator.FDO_TYPE_PREFIX;
import static org.nanopub.fdo.FdoUtils.DATA_REF_IRI;

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
	}

	/** Build statements out of tuples, requires the id (fdoIri) to be set */
	public Set<Statement> buildStatements() {
		Set<Statement> statements = new HashSet<>();
		for (var entry: tuples.entrySet()) {
			statements.add(vf.createStatement(this.id, entry.getKey(), entry.getValue()));
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
		Value schemaEntry = tuples.get(vf.createIRI(FDO_TYPE_PREFIX + "21.T11966/JsonSchema"));
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

}
