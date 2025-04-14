package org.nanopub;

import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.eclipse.rdf4j.common.exception.RDF4JException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.*;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.rio.*;
import org.eclipse.rdf4j.rio.helpers.AbstractRDFHandler;

import javax.activation.MimetypesFileTypeMap;
import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.*;

/**
 * Implementation of the Nanopub interface.
 *
 * @author Tobias Kuhn
 * @author Eelke van der Horst
 */
public class NanopubImpl implements NanopubWithNs, Serializable {

	private static final long serialVersionUID = -1514452524339132128L;

	static {
		tryToLoadParserFactory("org.eclipse.rdf4j.rio.trig.TriGParserFactory");
		RDFWriterRegistry.getInstance().add(new CustomTrigWriterFactory());
		tryToLoadParserFactory("org.eclipse.rdf4j.rio.nquads.NQuadsParserFactory");
		tryToLoadWriterFactory("org.eclipse.rdf4j.rio.nquads.NQuadsWriterFactory");
		tryToLoadParserFactory("org.eclipse.rdf4j.rio.trix.TriXParserFactory");
		tryToLoadWriterFactory("org.eclipse.rdf4j.rio.trix.TriXWriterFactory");
		tryToLoadParserFactory("org.eclipse.rdf4j.rio.jsonld.JSONLDParserFactory");
		tryToLoadWriterFactory("org.eclipse.rdf4j.rio.jsonld.JSONLDWriterFactory");
	}

	private static void tryToLoadParserFactory(String className) {
		try {
			RDFParserFactory pf = (RDFParserFactory) Class.forName(className).getConstructor().newInstance();
			RDFParserRegistry.getInstance().add(pf);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		};
	}

	private static void tryToLoadWriterFactory(String className) {
		try {
			RDFWriterFactory wf = (RDFWriterFactory) Class.forName(className).getConstructor().newInstance();
			RDFWriterRegistry.getInstance().add(wf);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		};
	}

	/**
	 * Just ensures the class to be loaded. Probably unnecessary.
	 */
	public static void ensureLoaded() {
		// ensure class is loaded; nothing left to be done
	}

	private static final MimetypesFileTypeMap mimeMap = new MimetypesFileTypeMap();

	private IRI nanopubUri;
	private IRI headUri, assertionUri, provenanceUri, pubinfoUri;
	private Set<IRI> graphUris;
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

	public NanopubImpl(Collection<Statement> statements, List<Pair<String,String>> namespaces) throws MalformedNanopubException {
		this.statements.addAll(statements);
		for (Pair<String,String> p : namespaces) {
			nsPrefixes.add(p.getLeft());
			ns.put(p.getLeft(), p.getRight());
		}
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

	public NanopubImpl(Repository repo, IRI nanopubUri, List<String> nsPrefixes, Map<String,String> ns)
			throws MalformedNanopubException, RepositoryException {
		if (nsPrefixes != null) this.nsPrefixes.addAll(nsPrefixes);
		if (ns != null) this.ns.putAll(ns);
		try (RepositoryConnection connection = repo.getConnection()) {
			String q = nanopubViaSPARQLQuery.replaceAll("@", nanopubUri.toString());
			TupleQuery tupleQuery = connection.prepareTupleQuery(QueryLanguage.SPARQL, q);
			TupleQueryResult result = tupleQuery.evaluate();
			try {
				while (result.hasNext()) {
					BindingSet bs = result.next();
					Resource g = (Resource) bs.getBinding("G").getValue();
					Resource s = (Resource) bs.getBinding("S").getValue();
					IRI p = (IRI) bs.getBinding("P").getValue();
					Value o = bs.getBinding("O").getValue();
					Statement st = SimpleValueFactory.getInstance().createStatement(s, p, o, g);
					statements.add(st);
				}
			} finally {
				result.close();
			}
		} catch (MalformedQueryException ex) {
			ex.printStackTrace();
		} catch (QueryEvaluationException ex) {
			ex.printStackTrace();
		}
		init();
	}

	public NanopubImpl(Repository repo, IRI nanopubUri)
			throws MalformedNanopubException, RepositoryException {
		this(repo, nanopubUri, null, null);
	}

	public NanopubImpl(File file, RDFFormat format)
			throws MalformedNanopubException, RDF4JException, IOException {
		readStatements(new FileInputStream(file), format);
		init();
	}

	public NanopubImpl(File file) throws MalformedNanopubException, RDF4JException, IOException {
		String n = file.getName();
		Optional<RDFFormat> format = Rio.getParserFormatForMIMEType(mimeMap.getContentType(n));
		if (!format.isPresent() || !format.get().supportsContexts()) {
			format = Rio.getParserFormatForFileName(n);
		}
		RDFFormat f = format.get();
		if (!f.supportsContexts()) {
			f = RDFFormat.TRIG;
		}
		readStatements(new FileInputStream(file), f);
		init();
	}

	public NanopubImpl(URL url, RDFFormat format) throws MalformedNanopubException, RDF4JException, IOException {
		HttpResponse response = getNanopub(url);
		readStatements(response.getEntity().getContent(), format);
		init();
	}

	public NanopubImpl(URL url) throws MalformedNanopubException, RDF4JException, IOException {
		HttpResponse response = getNanopub(url);
		Header contentTypeHeader = response.getFirstHeader("Content-Type");
		Optional<RDFFormat> format = null;
		if (contentTypeHeader != null) {
			format = Rio.getParserFormatForMIMEType(contentTypeHeader.getValue());
		}
		if (format == null || !format.isPresent() || !format.get().supportsContexts()) {
			format = Rio.getParserFormatForFileName(url.toString());
		}
		RDFFormat f = format.get();
		if (!f.supportsContexts()) {
			f = RDFFormat.TRIG;
		}
		readStatements(response.getEntity().getContent(), f);
		init();
	}

	private HttpResponse getNanopub(URL url) throws IOException {
		HttpGet get = null;
		try {
			get = new HttpGet(url.toString());
		} catch (IllegalArgumentException ex) {
			throw new IOException("invalid URL: " + url);
		}
		get.setHeader("Accept", "application/trig; q=1, application/x-trig; q=1, text/x-nquads; q=0.1, application/trix; q=0.1");
		HttpResponse response = NanopubUtils.getHttpClient().execute(get);
		int statusCode = response.getStatusLine().getStatusCode();
		if (statusCode == 404 || statusCode == 410) {
			throw new FileNotFoundException(response.getStatusLine().getReasonPhrase());
		}
		if (statusCode < 200 || statusCode > 299) {
			throw new IOException("HTTP error " + statusCode + ": " + response.getStatusLine().getReasonPhrase());
		}
		return response;
	}

	public NanopubImpl(InputStream in, RDFFormat format) throws MalformedNanopubException, RDF4JException, IOException {
		readStatements(in, format);
		init();
	}

	public NanopubImpl(String utf8, RDFFormat format) throws MalformedNanopubException, RDF4JException {
		try {
			readStatements(new ByteArrayInputStream(utf8.getBytes("UTF-8")), format);
		} catch (IOException ex) {
			// We do not expect an IOException here (no file system IO taking place)
			throw new RuntimeException("Unexptected IOException", ex);
		}
		init();
	}

	private void readStatements(InputStream in, RDFFormat format) throws MalformedNanopubException, RDF4JException, IOException {
		try (in) {
			RDFParser p = NanopubUtils.getParser(format);
			p.setRDFHandler(new AbstractRDFHandler() {
	
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
			p.parse(new InputStreamReader(in, Charset.forName("UTF-8")));
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
				nanopubUri = (IRI) st.getSubject();
				headUri = (IRI) st.getContext();
			}
		}
	}

	private void collectGraphs(Collection<Statement> statements) throws MalformedNanopubException {
		for (Statement st : statements) {
			if (st.getContext().equals(headUri)) {
				Resource s = st.getSubject();
				IRI p = st.getPredicate();
				if (s.equals(nanopubUri) && p.equals(Nanopub.HAS_ASSERTION_URI)) {
					if (assertionUri != null) {
						throw new MalformedNanopubException("Two assertion URIs found: " +
								assertionUri + " and " + st.getObject());
					}
					assertionUri = (IRI) st.getObject();
				} else if (s.equals(nanopubUri) && p.equals(Nanopub.HAS_PROVENANCE_URI)) {
					if (provenanceUri != null) {
						throw new MalformedNanopubException("Two provenance URIs found: " +
								provenanceUri + " and " + st.getObject());
					}
					provenanceUri = (IRI) st.getObject();
				} else if (s.equals(nanopubUri) && p.equals(Nanopub.HAS_PUBINFO_URI)) {
					if (pubinfoUri != null) {
						throw new MalformedNanopubException("Two publication info URIs found: " +
								pubinfoUri + " and " + st.getObject());
					}
					pubinfoUri = (IRI) st.getObject();
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

	private void addGraphUri(IRI uri) throws MalformedNanopubException {
		if (graphUris.contains(uri)) {
			throw new MalformedNanopubException("Each graph needs a unique URI: " + uri);
		}
		graphUris.add(uri);
	}

	private void collectStatements(Collection<Statement> statements) throws MalformedNanopubException {
		tripleCount = 0;
		byteCount = 0;
		Set<Statement> head = new LinkedHashSet<>();
		Set<Statement> assertion = new LinkedHashSet<>();
		Set<Statement> provenance = new LinkedHashSet<>();
		Set<Statement> pubinfo = new LinkedHashSet<>();
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
			if (st.getObject() instanceof IRI) {
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
	public IRI getUri() {
		return nanopubUri;
	}

	@Override
	public IRI getHeadUri() {
		return headUri;
	}

	@Override
	public Set<Statement> getHead() {
		return head;
	}

	@Override
	public IRI getAssertionUri() {
		return assertionUri;
	}

	@Override
	public Set<Statement> getAssertion() {
		return assertion;
	}

	@Override
	public IRI getProvenanceUri() {
		return provenanceUri;
	}

	@Override
	public Set<Statement> getProvenance() {
		return provenance;
	}

	@Override
	public IRI getPubinfoUri() {
		return pubinfoUri;
	}

	@Override
	public Set<Statement> getPubinfo() {
		return pubinfo;
	}

	@Override
	public Set<IRI> getGraphUris() {
		return graphUris;
	}

	@Override
	public Calendar getCreationTime() {
		return SimpleTimestampPattern.getCreationTime(this);
	}

	@Override
	public Set<IRI> getAuthors() {
		return SimpleCreatorPattern.getAuthors(this);
	}

	@Override
	public Set<IRI> getCreators() {
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

	/**
	 * @return a copy of the namespaces map
	 */
	public Map<String, String> getNs() {
		return new HashMap<>(ns);
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
