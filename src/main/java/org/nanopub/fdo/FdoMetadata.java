package org.nanopub.fdo;

import java.io.Serializable;
import java.util.Set;

import org.eclipse.rdf4j.model.Statement;
import org.nanopub.Nanopub;

// TODO: This class stores a changeable metadata record of an FDO. It can come from an
//       existing Handle-based FDO, a nanopub-based one, or of an FDO that is still being
//       created. The metadata record is stored as a set of RDF Statements (corresponding to
//       the assertion graph of an FDO nanopub.
public class FdoMetadata implements Serializable {

	private static final long serialVersionUID = 1L;

	public FdoMetadata() {
		// TODO Creates empty metadata set
	}

	public FdoMetadata(Nanopub np) {
		// TODO Creates metadata from nanopub
	}

	public FdoMetadata(Set<Statement> statements) {
		// TODO Creates metadata from statements
	}

	public Set<Statement> getStatements() {
		// TODO
		return null;
	}

	public FdoNanopub createFdoNanopub() {
		// Provide this functionality here or somewhere else...
		return null;
	}

}
