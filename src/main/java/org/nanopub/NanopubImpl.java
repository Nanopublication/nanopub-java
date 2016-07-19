package org.nanopub;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.activation.MimetypesFileTypeMap;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.openrdf.OpenRDFException;
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
import org.openrdf.rio.RDFParserFactory;
import org.openrdf.rio.RDFParserRegistry;
import org.openrdf.rio.RDFWriterFactory;
import org.openrdf.rio.RDFWriterRegistry;
import org.openrdf.rio.Rio;
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
		tryToLoadParserFactory("org.openrdf.rio.trig.TriGParserFactory");
		RDFWriterRegistry.getInstance().add(new CustomTrigWriterFactory());
		tryToLoadParserFactory("org.openrdf.rio.nquads.NQuadsParserFactory");
		tryToLoadWriterFactory("org.openrdf.rio.nquads.NQuadsWriterFactory");
		tryToLoadParserFactory("org.openrdf.rio.trix.TriXParserFactory");
		tryToLoadWriterFactory("org.openrdf.rio.trix.TriXWriterFactory");
		tryToLoadParserFactory("org.openrdf.rio.jsonld.JSONLDParserFactory;");
		tryToLoadWriterFactory("org.openrdf.rio.jsonld.JSONLDWriterFactory");
	}

	private static void tryToLoadParserFactory(String className) {
		try {
			RDFParserFactory pf = (RDFParserFactory) Class.forName(className).newInstance();
			RDFParserRegistry.getInstance().add(pf);
		} catch (ClassNotFoundException ex) {
		} catch (IllegalAccessException ex) {
		} catch (InstantiationException ex) {
		};
	}

	private static void tryToLoadWriterFactory(String className) {
		try {
			RDFWriterFactory wf = (RDFWriterFactory) Class.forName(className).newInstance();
			RDFWriterRegistry.getInstance().add(wf);
		} catch (ClassNotFoundException ex) {
		} catch (IllegalAccessException ex) {
		} catch (InstantiationException ex) {
		};
	}

	public static void ensureLoaded() {
		// ensure class is loaded; nothing left to be done
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

	private int tripleCount;
	private long byteCount;

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

	public NanopubImpl(Repository repo, URI nanopubUri, List<String> nsPrefixes, Map<String,String> ns)
			throws MalformedNanopubException, RepositoryException {
		if (nsPrefixes != null) this.nsPrefixes.addAll(nsPrefixes);
		if (ns != null) this.ns.putAll(ns);
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

	public NanopubImpl(Repository repo, URI nanopubUri)
			throws MalformedNanopubException, RepositoryException {
		this(repo, nanopubUri, null, null);
	}

	public NanopubImpl(File file, RDFFormat format)
			throws MalformedNanopubException, OpenRDFException, IOException {
		readStatements(new FileInputStream(file), format, "");
		init();
	}

	public NanopubImpl(File file) throws MalformedNanopubException, OpenRDFException, IOException {
		String n = file.getName();
		RDFFormat format = Rio.getParserFormatForMIMEType(mimeMap.getContentType(n));
		if (format == null || !format.supportsContexts()) {
			format = Rio.getParserFormatForFileName(n, RDFFormat.TRIG);
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
			format = Rio.getParserFormatForMIMEType(contentTypeHeader.getValue());
		}
		if (format == null || !format.supportsContexts()) {
			format = Rio.getParserFormatForFileName(url.toString(), RDFFormat.TRIG);
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
		try {
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
			p.parse(new InputStreamReader(in, Charset.forName("UTF-8")), baseUri);
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
						throw new MalformedNanopubException("Two assertion URIs found: " +
								assertionUri + " and " + st.getObject());
					}
					assertionUri = (URI) st.getObject();
				} else if (s.equals(nanopubUri) && p.equals(Nanopub.HAS_PROVENANCE_URI)) {
					if (provenanceUri != null) {
						throw new MalformedNanopubException("Two provenance URIs found: " +
								provenanceUri + " and " + st.getObject());
					}
					provenanceUri = (URI) st.getObject();
				} else if (s.equals(nanopubUri) && p.equals(Nanopub.HAS_PUBINFO_URI)) {
					if (pubinfoUri != null) {
						throw new MalformedNanopubException("Two publication info URIs found: " +
								pubinfoUri + " and " + st.getObject());
					}
					pubinfoUri = (URI) st.getObject();
				}
			}
		}
		if (assertionUri == null) {
			throw new MalformedNanopubException("No assertion URI found for " + nanopubUri);
		}
		if (provenanceUri == null) {
			throw new MalformedNanopubException("No provenance URI found for " + nanopubUri);
		}
		if (pubinfoUri == null) {
			throw new MalformedNanopubException("No publication info URI found for " + nanopubUri);
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
		tripleCount = 0;
		byteCount = 0;
		Set<Statement> head = new HashSet<>();
		Set<Statement> assertion = new HashSet<>();
		Set<Statement> provenance = new HashSet<>();
		Set<Statement> pubinfo = new HashSet<>();
		for (Statement st : statements) {
			checkStatement(st);
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
			tripleCount++;
			byteCount += st.getContext().stringValue().length();
			byteCount += st.getSubject().stringValue().length();
			byteCount += st.getPredicate().stringValue().length();
			byteCount += st.getObject().stringValue().length();
			if (tripleCount < 0) tripleCount = Integer.MAX_VALUE;
			if (byteCount < 0) byteCount = Long.MAX_VALUE;
		}
		this.head = ImmutableSet.copyOf(head);
		this.assertion = ImmutableSet.copyOf(assertion);
		this.provenance = ImmutableSet.copyOf(provenance);
		this.pubinfo = ImmutableSet.copyOf(pubinfo);
	}

	private void checkStatement(Statement st) throws MalformedNanopubException {
		String uriString = null;
		try {
			// Throw exceptions if not well-formed:
			uriString = st.getContext().stringValue();
			new java.net.URI(uriString);
			uriString = st.getSubject().stringValue();
			new java.net.URI(uriString);
			uriString = st.getPredicate().stringValue();
			new java.net.URI(uriString);
			if (st.getObject() instanceof URI) {
				uriString = st.getObject().stringValue();
				new java.net.URI(uriString);
			}
		} catch (URISyntaxException ex) {
			throw new MalformedNanopubException("Malformed URI: " + uriString);
		}
	}

	private void checkAssertion() throws MalformedNanopubException {
		if (assertion.isEmpty()) {
			throw new MalformedNanopubException("Empty assertion graph: " + assertionUri);
		}
	}

	private void checkProvenance() throws MalformedNanopubException {
		if (provenance.isEmpty()) {
			throw new MalformedNanopubException("Empty provenance graph: " + provenanceUri);
		}
		for (Statement st : provenance) {
			if (assertionUri.equals(st.getSubject())) return;
			if (assertionUri.equals(st.getObject())) return;
		}
		throw new MalformedNanopubException("Provenance does not refer to assertion: " + provenanceUri);
	}

	private void checkPubinfo() throws MalformedNanopubException {
		if (pubinfo.isEmpty()) {
			throw new MalformedNanopubException("Empty publication info graph: " + pubinfoUri);
		}
		for (Statement st : pubinfo) {
			if (nanopubUri.equals(st.getSubject())) return;
			if (nanopubUri.equals(st.getObject())) return;
		}
		throw new MalformedNanopubException("Publication info does not refer to nanopublication URI: " + pubinfoUri);
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
		return SimpleTimestampPattern.getCreationTime(this);
	}

	@Override
	public Set<URI> getAuthors() {
		return SimpleCreatorPattern.getAuthors(this);
	}

	@Override
	public Set<URI> getCreators() {
		return SimpleCreatorPattern.getCreators(this);
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

	@Override
	public int getTripleCount() {
		return tripleCount;
	}

	@Override
	public long getByteCount() {
		return byteCount;
	}

}
