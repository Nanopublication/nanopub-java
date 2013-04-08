package ch.tkuhn.nanopub;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.ContextStatementImpl;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.query.BindingSet;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sparql.SPARQLRepository;

import com.google.common.collect.ImmutableSet;

public class NanopubImpl implements Nanopub {

	private URI nanopubUri;
	private URI headUri, assertionUri, provenanceUri, pubinfoUri;
	private Set<Statement> head, assertion, provenance, pubinfo;

	public NanopubImpl(Collection<Statement> statements) throws MalformedNanopubException {
		init(statements);
	}

	private static final String nanopubViaSPARQLQuery =
			"prefix np: <http://www.nanopub.org/nschema#> " +
			"prefix this: <@> " +
			"select ?G ?S ?P ?O where { " +
			"  { " +
			"    graph ?G { this: np:hasAssertion ?A } " +
			"  } union { " +
			"    graph ?H { { this: np:hasAssertion ?G } union { this: np:hasProvenance ?G } " +
			"        union { this: np:hasPublicationInfo ?G } } " +
			"  } " +
			"  graph ?G { ?S ?P ?O } " +
			"}";

	public NanopubImpl(String sparqlEndpointUrl, URI nanopubUri)
			throws MalformedNanopubException, RepositoryException {
		List<Statement> statements = new ArrayList<Statement>();
		try {
			SPARQLRepository repo = new SPARQLRepository(sparqlEndpointUrl);
			repo.initialize();
			RepositoryConnection connection = repo.getConnection();
			try {
				String q = nanopubViaSPARQLQuery.replaceAll("@", nanopubUri.toString());
				TupleQuery tupleQuery = connection.prepareTupleQuery(QueryLanguage.SPARQL, q);
				TupleQueryResult result = tupleQuery.evaluate();
				try {
					while (result.hasNext()) {
						BindingSet bs = result.next();
						Resource g = (Resource) bs.getBinding("G").getValue();
						Resource s = (Resource) bs.getBinding("S").getValue();
						URI p = (URI) bs.getBinding("P").getValue();
						Value o = bs.getBinding("O").getValue();
						Statement st = new ContextStatementImpl(s, p, o, g);
						statements.add(st);
					}
				} finally {
					result.close();
				}
			} finally {
				connection.close();
			}
		} catch (MalformedQueryException ex) {
			ex.printStackTrace();
		} catch (QueryEvaluationException ex) {
			ex.printStackTrace();
		}
		init(statements);
	}

	public void init(Collection<Statement> statements) throws MalformedNanopubException {
		collectNanopubUri(statements);
		if (nanopubUri == null || headUri == null) {
			throw new MalformedNanopubException("No nanopub URI found");
		}
		collectGraphs(statements);
		if (assertionUri == null) {
			throw new MalformedNanopubException("No assertion URI found");
		}
		if (provenanceUri == null) {
			throw new MalformedNanopubException("No provenance URI found");
		}
		if (pubinfo == null) {
			throw new MalformedNanopubException("No publication info URI found");
		}
		collectStatements(statements);
	}

	private void collectNanopubUri(Collection<Statement> statements) throws MalformedNanopubException {
		for (Statement st : statements) {
			if (st.getPredicate().equals(RDF.TYPE) && st.getObject().equals(Nanopub.NANOPUB_TYPE_URI)) {
				if (nanopubUri != null) {
					throw new MalformedNanopubException("Two nanopub URIs found");
				}
				nanopubUri = (URI) st.getSubject();
				headUri = (URI) st.getContext();
			}
		}
	}

	private void collectGraphs(Collection<Statement> statements) throws MalformedNanopubException {
		for (Statement st : statements) {
			if (st.getSubject().equals(nanopubUri)) {
				if (st.getPredicate().equals(Nanopub.HAS_ASSERTION_URI)) {
					if (assertionUri != null) {
						throw new MalformedNanopubException("Two assertion URIs found");
					}
					assertionUri = (URI) st.getObject();
				} else if (st.getPredicate().equals(Nanopub.HAS_PROVENANCE_URI)) {
					if (provenanceUri != null) {
						throw new MalformedNanopubException("Two provenance URIs found");
					}
					provenanceUri = (URI) st.getObject();
				} else if (st.getPredicate().equals(Nanopub.HAS_PUBINFO_URI)) {
					if (pubinfoUri != null) {
						throw new MalformedNanopubException("Two publication info URIs found");
					}
					pubinfoUri = (URI) st.getObject();
				}
			}
		}
	}

	private void collectStatements(Collection<Statement> statements) throws MalformedNanopubException {
		head = new HashSet<>();
		assertion = new HashSet<>();
		provenance = new HashSet<>();
		pubinfo = new HashSet<>();
		for (Statement st : statements) {
			if (st.getContext().equals(headUri)) {
				head.add(st);
			} else if (st.getContext().equals(assertionUri)) {
				assertion.add(st);
			} else if (st.getContext().equals(provenanceUri)) {
				provenance.add(st);
			} else if (st.getContext().equals(pubinfoUri)) {
				pubinfo.add(st);
			}
		}
	}

	@Override
	public URI getUri() {
		return nanopubUri;
	}

	@Override
	public URI getHeadUri() {
		return headUri;
	}

	@Override
	public Set<Statement> getHead() {
		return ImmutableSet.copyOf(head);
	}

	@Override
	public URI getAssertionUri() {
		return assertionUri;
	}

	@Override
	public Set<Statement> getAssertion() {
		return ImmutableSet.copyOf(assertion);
	}

	@Override
	public URI getProvenanceUri() {
		return provenanceUri;
	}

	@Override
	public Set<Statement> getProvenance() {
		return ImmutableSet.copyOf(provenance);
	}

	@Override
	public URI getPubinfoUri() {
		return pubinfoUri;
	}

	@Override
	public Set<Statement> getPubinfo() {
		return ImmutableSet.copyOf(pubinfo);
	}

}
