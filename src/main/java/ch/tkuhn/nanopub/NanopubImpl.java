package ch.tkuhn.nanopub;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.activation.MimetypesFileTypeMap;
import javax.xml.bind.DatatypeConverter;

import org.openrdf.OpenRDFException;
import org.openrdf.model.Literal;
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
import nl.rajaram.nanopub.CheckURI;
import nl.rajaram.nanopub.URIs;

/**
 * 
 * @author Tobias, Rajaram
 * @version 1.1
 */

public class NanopubImpl implements Nanopub, Serializable {

	private static final long serialVersionUID = -1514452524339132128L;

	private static final URI SUB_GRAPH_OF = new URIImpl("http://www.w3.org/2004/03/trix/rdfg-1/subGraphOf");
	private static final URI CREATION_TIME = new URIImpl("http://purl.org/dc/terms/created");
	private static final URI DATETIME_TYPE = new URIImpl("http://www.w3.org/2001/XMLSchema#dateTime");
	private static final URI HAS_AUTHOR = new URIImpl("http://swan.mindinformatics.org/ontologies/1.2/pav/authoredBy");
	private static final URI HAS_CREATOR = new URIImpl("http://swan.mindinformatics.org/ontologies/1.2/pav/createdBy");

	private static final MimetypesFileTypeMap mimeMap = new MimetypesFileTypeMap();

	private URI nanopubUri;
	private URI headUri, assertionUri, provenanceUri, pubinfoUri;
	private Set<URI> assertionSubUris, provenanceSubUris, pubinfoSubUris;
	private Set<URI> graphUris;
	private Set<Statement> head, assertion, provenance, pubinfo;
        private List<URIs> uris = new ArrayList<URIs>();

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

	public NanopubImpl(String utf8, RDFFormat format)
			throws MalformedNanopubException, OpenRDFException, IOException {
		this(new ByteArrayInputStream(utf8.getBytes("UTF-8")), format);
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
                uris.add(new URIs(headUri, "Head uri"));
                uris.add(new URIs(nanopubUri, "Nanopublication uri"));
                uris.add(new URIs(assertionUri, "Assertion uri"));
                uris.add(new URIs(provenanceUri, "Provenance uri"));
                uris.add(new URIs(pubinfoUri, "Publication Info uri"));
                CheckURI.checkDuplicates(uris);
		collectSubGraphs(statements);
		collectStatements(statements);
	}

	private void collectNanopubUri(Collection<Statement> statements) throws MalformedNanopubException {
		for (Statement st : statements) {
			if (st.getContext() == null) {
				throw new MalformedNanopubException("Null value for context URI found.");
			}
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
		Set<URI> graphUris = new HashSet<>();
		graphUris.add(headUri);
		graphUris.add(assertionUri);
		graphUris.add(provenanceUri);
		graphUris.add(pubinfoUri);
		Set<URI> assertionSubUris = new HashSet<>();
		Set<URI> provenanceSubUris = new HashSet<>();
		Set<URI> pubinfoSubUris = new HashSet<>();
		for (Statement st : statements) {
			if (st.getContext().equals(headUri) && st.getPredicate().equals(SUB_GRAPH_OF)) {
				if (st.getObject().equals(assertionUri)) {
					URI g = (URI) st.getSubject();
					graphUris.add(g);
					assertionSubUris.add(g);
				} else if (st.getObject().equals(provenanceUri)) {
					URI g = (URI) st.getSubject();
					graphUris.add(g);
					provenanceSubUris.add(g);
				} else if (st.getObject().equals(pubinfoUri)) {
					URI g = (URI) st.getSubject();
					graphUris.add(g);
					pubinfoSubUris.add(g);
				}
			}
		}
		this.graphUris = ImmutableSet.copyOf(graphUris);
		this.assertionSubUris = ImmutableSet.copyOf(assertionSubUris);
		this.provenanceSubUris = ImmutableSet.copyOf(provenanceSubUris);
		this.pubinfoSubUris = ImmutableSet.copyOf(pubinfoSubUris);
	}

	private void collectStatements(Collection<Statement> statements) throws MalformedNanopubException {
		Set<Statement> head = new HashSet<>();
		Set<Statement> assertion = new HashSet<>();
		Set<Statement> provenance = new HashSet<>();
		Set<Statement> pubinfo = new HashSet<>();
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
			} else if (assertionSubUris.contains(g)) {
				assertion.add(st);
			} else if (provenanceSubUris.contains(g)) {
				provenance.add(st);
			} else if (pubinfoSubUris.contains(g)) {
				pubinfo.add(st);
			} else {
				throw new MalformedNanopubException("Disconnected graph: " + g);
			}
		}
		this.head = ImmutableSet.copyOf(head);
		this.assertion = ImmutableSet.copyOf(assertion);
		this.provenance = ImmutableSet.copyOf(provenance);
		this.pubinfo = ImmutableSet.copyOf(pubinfo);
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
		return head;
	}

	@Override
	public URI getAssertionUri() {
		return assertionUri;
	}

	@Override
	public Set<Statement> getAssertion() {
		return assertion;
	}

	@Override
	public URI getProvenanceUri() {
		return provenanceUri;
	}

	@Override
	public Set<Statement> getProvenance() {
		return provenance;
	}

	@Override
	public URI getPubinfoUri() {
		return pubinfoUri;
	}

	@Override
	public Set<Statement> getPubinfo() {
		return pubinfo;
	}

	@Override
	public Set<URI> getGraphUris() {
		return graphUris;
	}

	@Override
	public Calendar getCreationTime() {
		String s = null;
		for (Statement st : pubinfo) {
			if (!st.getSubject().equals(nanopubUri)) continue;
			if (!st.getPredicate().equals(CREATION_TIME)) continue;
			if (!(st.getObject() instanceof Literal)) continue;
			Literal l = (Literal) st.getObject();
			if (!l.getDatatype().equals(DATETIME_TYPE)) continue;
			s = l.stringValue();
			break;
		}
		if (s == null) return null;
		return DatatypeConverter.parseDateTime(s);
	}

	@Override
	public Set<URI> getAuthors() {
		Set<URI> authors = new HashSet<>();
		for (Statement st : pubinfo) {
			if (!st.getSubject().equals(nanopubUri)) continue;
			if (!st.getPredicate().equals(HAS_AUTHOR)) continue;
			if (!(st.getObject() instanceof URI)) continue;
			authors.add((URI) st.getObject());
		}
		return authors;
	}

	@Override
	public Set<URI> getCreators() {
		Set<URI> authors = new HashSet<>();
		for (Statement st : pubinfo) {
			if (!st.getSubject().equals(nanopubUri)) continue;
			if (!st.getPredicate().equals(HAS_CREATOR)) continue;
			if (!(st.getObject() instanceof URI)) continue;
			authors.add((URI) st.getObject());
		}
		return authors;
	}

}
