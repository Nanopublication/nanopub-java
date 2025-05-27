package org.nanopub.fdo;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.nanopub.MalformedNanopubException;
import org.nanopub.Nanopub;
import org.nanopub.NanopubCreator;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import static org.nanopub.fdo.FdoNanopubCreator.FDO_TYPE_PREFIX;

/**
 * This class stores a changeable metadata record of an FDO. It can come from an existing Handle-based FDO,
 * a nanopub-based one, or of an FDO that is still being created. The metadata record may be viewed as a set of
 * RDF Statements (corresponding to  the assertion graph of an FDO nanopub). Internally it's represented as a
 * Map of tuples <IRI, Value>
 */
public class FdoMetadata implements Serializable {

	private static final long serialVersionUID = 1L;
	private static final ValueFactory vf = SimpleValueFactory.getInstance();

	private String id = null;
	private final HashMap<IRI, Value> tuples = new HashMap<>();

	public FdoMetadata() {
	}

	public FdoMetadata(Nanopub np) {
		Statement anyAssertion = np.getAssertion().iterator().next();
		this.id = FdoUtils.extractHandle(anyAssertion.getSubject());
		for (Statement st: np.getAssertion()) {
			tuples.put(st.getPredicate(), st.getObject());
		}
	}

	public FdoMetadata(Set<Statement> statements) {
		for (Statement st: statements) {
			tuples.put(st.getPredicate(), st.getObject());
			if (st.getPredicate().equals(FdoUtils.PROFILE_IRI)) { // a profile must be there
				id = FdoUtils.extractHandle(st.getSubject());
			}
		}
	}

	public Set<Statement> getStatements() {
		Set<Statement> statements = new HashSet<>();
		for (var entry: tuples.entrySet()) {
			statements.add(vf.createStatement(FdoUtils.createIri(this.id), entry.getKey(), entry.getValue()));
		}
		return statements;
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

	public String getId() {
		return id;
	}

	public void setDataRef(String dataRef) {
		tuples.put(FdoUtils.DATA_REF_IRI, vf.createIRI(dataRef));
	}

	public FdoNanopub createFdoNanopub() throws MalformedNanopubException {
		NanopubCreator creator = FdoNanopubCreator.createWithMetadata(this);
		Nanopub np = creator.finalizeNanopub(true);
		return new FdoNanopub(np);
	}

}
