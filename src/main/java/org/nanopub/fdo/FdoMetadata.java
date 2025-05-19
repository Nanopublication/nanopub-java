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

/**
 * This class stores a changeable metadata record of an FDO. It can come from an existing Handle-based FDO,
 * a nanopub-based one, or of an FDO that is still being created. The metadata record is stored as a set of
 * RDF Statements (corresponding to  the assertion graph of an FDO nanopub).
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
			if (st.getPredicate().equals(FdoUtils.RDF_FDO_PROFILE)) { // this assertion must be there
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

	public IRI getProfile() {
		for (var entry: tuples.entrySet()) {
			if (entry.getKey().equals(FdoUtils.RDF_FDO_PROFILE)) {
				return vf.createIRI(entry.getValue().stringValue());
			}
		}
		return null;
	}

	public String getLabel() {
		for (var entry: tuples.entrySet()) {
			if (entry.getKey().equals(RDFS.LABEL)) {
				return entry.getValue().stringValue();
			}
		}
		return null;
	}

	public String getId() {
		return id;
	}

	public FdoNanopub createFdoNanopub() throws MalformedNanopubException {
		NanopubCreator creator = FdoNanopubCreator.createWithMetadata(this);
		Nanopub np = creator.finalizeNanopub(true);
		return new FdoNanopub(np);
	}

}
