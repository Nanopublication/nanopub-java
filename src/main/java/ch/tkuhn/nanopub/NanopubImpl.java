package ch.tkuhn.nanopub;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.activation.MimetypesFileTypeMap;

import org.openrdf.OpenRDFException;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.ContextStatementImpl;
import org.openrdf.model.impl.URIImpl;
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
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.Rio;
import org.openrdf.rio.helpers.RDFHandlerBase;

import com.google.common.collect.ImmutableSet;

public class NanopubImpl implements Nanopub, Serializable {

	private static final long serialVersionUID = -1514452524339132128L;

	private static final URI SUB_GRAPH_OF = new URIImpl("http://www.w3.org/2004/03/trix/rdfg-1/subGraphOf");

	private static final MimetypesFileTypeMap mimeMap = new MimetypesFileTypeMap();

	private URI nanopubUri;
	private URI headUri, assertionUri, provenanceUri, pubinfoUri;
	private Set<URI> assertionSubUris, provenanceSubUris, pubinfoSubUris;
	private Set<Statement> head, assertion, provenance, pubinfo;

	public NanopubImpl(Collection<Statement> statements) throws MalformedNanopubException {
		init(statements);
	}

	private static final String nanopubViaSPARQLQuery =
			"prefix np: <http://www.nanopub.org/nschema#> " +
			"prefix rdfg: <http://www.w3.org/2004/03/trix/rdfg-1/> " +
			"prefix this: <@> " +
			"select ?G ?S ?P ?O where { " +
			"  { " +
			"    graph ?G { this: a np:Nanopublication } " +
			"  } union { " +
			"    graph ?H { this: a np:Nanopublication } . " +
			"    graph ?H { { this: np:hasAssertion ?G } union { this: np:hasProvenance ?G } " +
			"        union { this: np:hasPublicationInfo ?G } } " +
			"  } union { " +
			"    graph ?H { this: a np:Nanopublication . ?G rdfg:subGraphOf ?I } . " +
			"    graph ?H { { this: np:hasAssertion ?I } union { this: np:hasProvenance ?I } " +
			"        union { this: np:hasPublicationInfo ?I } } " +
			"  } " +
			"  graph ?G { ?S ?P ?O } " +
			"}";

	public NanopubImpl(SPARQLRepository repo, URI nanopubUri)
			throws MalformedNanopubException, RepositoryException {
		List<Statement> statements = new ArrayList<Statement>();
		try {
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

	public NanopubImpl(File file) throws MalformedNanopubException, OpenRDFException, IOException {
		String n = file.getName();
		RDFFormat format = RDFFormat.forMIMEType(mimeMap.getContentType(n));
		if (format == null) {
			format = RDFFormat.forFileName(n, RDFFormat.TRIG);
		}
		init(readStatements(new FileInputStream(file), format));
	}

	public NanopubImpl(URL url) throws MalformedNanopubException, OpenRDFException, IOException {
		URLConnection conn = url.openConnection();
		RDFFormat format = RDFFormat.forMIMEType(conn.getContentType());
		if (format == null) {
			format = RDFFormat.forFileName(url.toString(), RDFFormat.TRIG);
		}
		init(readStatements(conn.getInputStream(), format));
	}

	public NanopubImpl(InputStream in, RDFFormat format)
			throws MalformedNanopubException, OpenRDFException, IOException {
		init(readStatements(in, format));
	}

	private static List<Statement> readStatements(InputStream in, RDFFormat format)
			throws MalformedNanopubException, OpenRDFException, IOException {
		final List<Statement> statements = new ArrayList<Statement>();
		RDFParser p = Rio.createParser(format);
		p.setRDFHandler(new RDFHandlerBase() {
			@Override
			public void handleStatement(Statement st) throws RDFHandlerException {
				statements.add(st);
			}
		});
		try {
			p.parse(in, "");
		} finally {
			in.close();
		}
		return statements;
	}

	private void init(Collection<Statement> statements) throws MalformedNanopubException {
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
		if (pubinfoUri == null) {
			throw new MalformedNanopubException("No publication info URI found");
		}
		collectSubGraphs(statements);
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
			if (st.getContext().equals(headUri) && st.getSubject().equals(nanopubUri)) {
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

	private void collectSubGraphs(Collection<Statement> statements) throws MalformedNanopubException {
		assertionSubUris = new HashSet<>();
		provenanceSubUris = new HashSet<>();
		pubinfoSubUris = new HashSet<>();
		for (Statement st : statements) {
			if (st.getContext().equals(headUri) && st.getPredicate().equals(SUB_GRAPH_OF)) {
				if (st.getObject().equals(assertionUri)) {
					assertionSubUris.add((URI) st.getSubject());
				} else if (st.getObject().equals(provenanceUri)) {
					provenanceSubUris.add((URI) st.getSubject());
				} else if (st.getObject().equals(pubinfoUri)) {
					pubinfoSubUris.add((URI) st.getSubject());
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
			Resource g = st.getContext();
			if (g.equals(headUri)) {
				head.add(st);
			} else if (g.equals(assertionUri)) {
				assertion.add(st);
			} else if (g.equals(provenanceUri)) {
				provenance.add(st);
			} else if (g.equals(pubinfoUri)) {
				pubinfo.add(st);
			}
			if (assertionSubUris.contains(g)) {
				assertion.add(st);
			}
			if (provenanceSubUris.contains(g)) {
				provenance.add(st);
			}
			if (pubinfoSubUris.contains(g)) {
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
