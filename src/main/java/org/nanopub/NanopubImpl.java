package org.nanopub;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.activation.MimetypesFileTypeMap;
import javax.xml.bind.DatatypeConverter;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.openrdf.OpenRDFException;
import org.openrdf.model.Literal;
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
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.RDFWriterRegistry;
import org.openrdf.rio.helpers.RDFHandlerBase;

import com.google.common.collect.ImmutableSet;

/**
 * Implementation of the Nanopub interface.
 *
 * @author Tobias Kuhn
 * @author Eelke van der Horst
 */
public class NanopubImpl implements NanopubWithNs, Serializable {

	private static final long serialVersionUID = -1514452524339132128L;

	static {
		RDFWriterRegistry.getInstance().add(new CustomTrigWriterFactory());
	}

	private static final MimetypesFileTypeMap mimeMap = new MimetypesFileTypeMap();

	private URI nanopubUri;
	private URI headUri, assertionUri, provenanceUri, pubinfoUri;
	private Set<URI> graphUris;
	private Set<Statement> head, assertion, provenance, pubinfo;

	private List<Statement> statements = new ArrayList<>();
	private List<String> nsPrefixes = new ArrayList<>();
	private Map<String,String> ns = new HashMap<>();
	private boolean unusedPrefixesRemoved = false;

	public NanopubImpl(Collection<Statement> statements, List<String> nsPrefixes, Map<String,String> ns) throws MalformedNanopubException {
		this.statements.addAll(statements);
		this.nsPrefixes.addAll(nsPrefixes);
		this.ns.putAll(ns);
		init();
	}

	public NanopubImpl(Collection<Statement> statements) throws MalformedNanopubException {
		this.statements.addAll(statements);
		init();
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
			"  } " +
			"  graph ?G { ?S ?P ?O } " +
			"}";

	public NanopubImpl(Repository repo, URI nanopubUri)
			throws MalformedNanopubException, RepositoryException {
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
		init();
	}

	public NanopubImpl(File file, RDFFormat format)
			throws MalformedNanopubException, OpenRDFException, IOException {
		readStatements(new FileInputStream(file), format, "");
		init();
	}

	public NanopubImpl(File file) throws MalformedNanopubException, OpenRDFException, IOException {
		String n = file.getName();
		RDFFormat format = RDFFormat.forMIMEType(mimeMap.getContentType(n));
		if (format == null || !format.supportsContexts()) {
			format = RDFFormat.forFileName(n, RDFFormat.TRIG);
		}
		if (!format.supportsContexts()) {
			format = RDFFormat.TRIG;
		}
		readStatements(new FileInputStream(file), format, "");
		init();
	}

	public NanopubImpl(URL url, RDFFormat format) throws MalformedNanopubException, OpenRDFException, IOException {
		HttpResponse response = getNanopub(url);
		readStatements(response.getEntity().getContent(), format, "");
		init();
	}

	public NanopubImpl(URL url) throws MalformedNanopubException, OpenRDFException, IOException {
		HttpResponse response = getNanopub(url);
		Header contentTypeHeader = response.getFirstHeader("Content-Type");
		RDFFormat format = RDFFormat.TRIG;
		if (contentTypeHeader != null) {
			format = RDFFormat.forMIMEType(contentTypeHeader.getValue());
		}
		if (format == null || !format.supportsContexts()) {
			format = RDFFormat.forFileName(url.toString(), RDFFormat.TRIG);
		}
		if (!format.supportsContexts()) {
			format = RDFFormat.TRIG;
		}
		readStatements(response.getEntity().getContent(), format, "");
		init();
	}

	private HttpResponse getNanopub(URL url) throws IOException {
		HttpGet get = new HttpGet(url.toString());
		get.setHeader("Accept", "application/trig; q=1, application/x-trig; q=1, text/x-nquads; q=0.1, application/trix; q=0.1");
		HttpResponse response = HttpClientBuilder.create().build().execute(get);
		int statusCode = response.getStatusLine().getStatusCode();
		if (statusCode == 404 || statusCode == 410) {
			throw new FileNotFoundException(response.getStatusLine().getReasonPhrase());
		}
		if (statusCode < 200 || statusCode > 299) {
			throw new IOException("HTTP error " + statusCode + ": " + response.getStatusLine().getReasonPhrase());
		}
		return response;
	}

	public NanopubImpl(InputStream in, RDFFormat format, String baseUri)
			throws MalformedNanopubException, OpenRDFException, IOException {
		readStatements(in, format, baseUri);
		init();
	}

	public NanopubImpl(InputStream in, RDFFormat format) throws MalformedNanopubException, OpenRDFException, IOException {
		this(in, format, "");
	}

	public NanopubImpl(String utf8, RDFFormat format, String baseUri) throws MalformedNanopubException, OpenRDFException {
		try {
			readStatements(new ByteArrayInputStream(utf8.getBytes("UTF-8")), format, baseUri);
		} catch (IOException ex) {
			// We do not expect an IOException here (no file system IO taking place)
			throw new RuntimeException("Unexptected IOException", ex);
		}
		init();
	}

	public NanopubImpl(String utf8, RDFFormat format) throws MalformedNanopubException, OpenRDFException {
		this(utf8, format, "");
	}

	// TODO Is the baseURI really needed? Shouldn't the input stream contain all needed data?
	private void readStatements(InputStream in, RDFFormat format, String baseUri)
			throws MalformedNanopubException, OpenRDFException, IOException {
		RDFParser p = NanopubUtils.getParser(format);
		p.setRDFHandler(new RDFHandlerBase() {

			@Override
			public void handleNamespace(String prefix, String uri) throws RDFHandlerException {
				nsPrefixes.add(prefix);
				ns.put(prefix, uri);
			}

			@Override
			public void handleStatement(Statement st) throws RDFHandlerException {
				statements.add(st);
			}

		});
		try {
			p.parse(in, baseUri);
		} finally {
			in.close();
		}
	}

	private void init() throws MalformedNanopubException {
		if (statements.isEmpty()) {
			throw new MalformedNanopubException("No content received for nanopub");
		}
		collectNanopubUri(statements);
		if (nanopubUri == null || headUri == null) {
			throw new MalformedNanopubException("No nanopub URI found");
		}
		collectGraphs(statements);
		collectStatements(statements);
		checkAssertion();
		checkProvenance();
		checkPubinfo();
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
			if (st.getContext().equals(headUri)) {
				Resource s = st.getSubject();
				URI p = st.getPredicate();
				if (s.equals(nanopubUri) && p.equals(Nanopub.HAS_ASSERTION_URI)) {
					if (assertionUri != null) {
						throw new MalformedNanopubException("Two assertion URIs found");
					}
					assertionUri = (URI) st.getObject();
				} else if (s.equals(nanopubUri) && p.equals(Nanopub.HAS_PROVENANCE_URI)) {
					if (provenanceUri != null) {
						throw new MalformedNanopubException("Two provenance URIs found");
					}
					provenanceUri = (URI) st.getObject();
				} else if (s.equals(nanopubUri) && p.equals(Nanopub.HAS_PUBINFO_URI)) {
					if (pubinfoUri != null) {
						throw new MalformedNanopubException("Two publication info URIs found");
					}
					pubinfoUri = (URI) st.getObject();
				}
			}
		}
		if (assertionUri == null) {
			throw new MalformedNanopubException("No assertion URI found");
		}
		if (provenanceUri == null) {
			throw new MalformedNanopubException("No provenance URI found");
		}
		if (pubinfoUri == null) {
			throw new MalformedNanopubException("No publication info URI found");
		}
		graphUris = new HashSet<>();
		addGraphUri(headUri);
		addGraphUri(assertionUri);
		addGraphUri(provenanceUri);
		addGraphUri(pubinfoUri);
		if (graphUris.contains(nanopubUri)) {
			throw new MalformedNanopubException("Nanopub URI cannot be identical to one of the graph URIs: " + nanopubUri);
		}
		this.graphUris = ImmutableSet.copyOf(graphUris);
	}

	private void addGraphUri(URI uri) throws MalformedNanopubException {
		if (graphUris.contains(uri)) {
			throw new MalformedNanopubException("Each graph needs a unique URI: " + uri);
		}
		graphUris.add(uri);
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
			} else {
				throw new MalformedNanopubException("Disconnected graph: " + g);
			}
		}
		this.head = ImmutableSet.copyOf(head);
		this.assertion = ImmutableSet.copyOf(assertion);
		this.provenance = ImmutableSet.copyOf(provenance);
		this.pubinfo = ImmutableSet.copyOf(pubinfo);
	}

	private void checkAssertion() throws MalformedNanopubException {
		if (assertion.isEmpty()) {
			throw new MalformedNanopubException("Empty assertion graph");
		}
	}

	private void checkProvenance() throws MalformedNanopubException {
		if (provenance.isEmpty()) {
			throw new MalformedNanopubException("Empty provenance graph");
		}
		for (Statement st : provenance) {
			if (assertionUri.equals(st.getSubject())) return;
			if (assertionUri.equals(st.getObject())) return;
		}
		throw new MalformedNanopubException("Provenance does not refer to assertion");
	}

	private void checkPubinfo() throws MalformedNanopubException {
		if (pubinfo.isEmpty()) {
			throw new MalformedNanopubException("Empty publication info graph");
		}
		for (Statement st : pubinfo) {
			if (nanopubUri.equals(st.getSubject())) return;
			if (nanopubUri.equals(st.getObject())) return;
		}
		throw new MalformedNanopubException("Publication info does not refer to nanopublication URI");
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
			if (!NanopubVocab.isCreationTimeProperty(st.getPredicate())) continue;
			if (!(st.getObject() instanceof Literal)) continue;
			Literal l = (Literal) st.getObject();
			if (!l.getDatatype().equals(NanopubVocab.XSD_DATETIME)) continue;
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
			if (!NanopubVocab.isAuthorProperty(st.getPredicate())) continue;
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
			if (!NanopubVocab.isCreatorProperty(st.getPredicate())) continue;
			if (!(st.getObject() instanceof URI)) continue;
			authors.add((URI) st.getObject());
		}
		return authors;
	}

	@Override
	public List<String> getNsPrefixes() {
		return new ArrayList<>(nsPrefixes);
	}

	@Override
	public String getNamespace(String prefix) {
		return ns.get(prefix);
	}

	@Override
	public void removeUnusedPrefixes() {
		if (unusedPrefixesRemoved) return;
		Set<String> usedPrefixes = NanopubUtils.getUsedPrefixes(this);
		for (String prefix : new ArrayList<>(nsPrefixes)) {
			if (!usedPrefixes.contains(prefix)) {
				nsPrefixes.remove(prefix);
				ns.remove(prefix);
			}
		}
		unusedPrefixesRemoved = true;
	}

}
