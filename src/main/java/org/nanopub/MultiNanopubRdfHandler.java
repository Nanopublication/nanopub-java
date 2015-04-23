package org.nanopub;

import static org.nanopub.Nanopub.NANOPUB_TYPE_URI;
import static org.nanopub.Nanopub.HAS_ASSERTION_URI;
import static org.nanopub.Nanopub.HAS_PROVENANCE_URI;
import static org.nanopub.Nanopub.HAS_PUBINFO_URI;
import static org.nanopub.Nanopub.NANOPUBCOLL_TYPE_URI;
import static org.nanopub.Nanopub.HAS_ASSERTIONSET_URI;
import static org.nanopub.Nanopub.HAS_COLLPROVENANCE_URI;
import static org.nanopub.Nanopub.HAS_COLLPUBINFO_URI;
import static org.nanopub.Nanopub.HAS_MEMBER;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.impl.ContextStatementImpl;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.Rio;
import org.openrdf.rio.helpers.RDFHandlerBase;

/**
 * Handles files or streams with a sequence of nanopubs.
 *
 * @author Tobias Kuhn
 */
public class MultiNanopubRdfHandler extends RDFHandlerBase {

	public static void process(RDFFormat format, InputStream in, NanopubHandler npHandler)
			throws IOException, RDFParseException, RDFHandlerException, MalformedNanopubException {
		process(format, in, null, npHandler);
	}

	public static void process(RDFFormat format, File file, NanopubHandler npHandler)
			throws IOException, RDFParseException, RDFHandlerException, MalformedNanopubException {
		InputStream in;
		if (file.getName().matches(".*\\.(gz|gzip)")) {
			in = new GZIPInputStream(new BufferedInputStream(new FileInputStream(file)));
		} else {
			in = new BufferedInputStream(new FileInputStream(file));
		}
		process(format, in, file, npHandler);
	}

	public static void process(File file, NanopubHandler npHandler)
			throws IOException, RDFParseException, RDFHandlerException, MalformedNanopubException {
		RDFFormat format = Rio.getParserFormatForFileName(file.getName(), RDFFormat.TRIG);
		process(format, file, npHandler);
	}

	private static void process(RDFFormat format, InputStream in, File file, NanopubHandler npHandler)
			throws IOException, RDFParseException, RDFHandlerException, MalformedNanopubException {
		RDFParser p = NanopubUtils.getParser(format);
		p.setRDFHandler(new MultiNanopubRdfHandler(npHandler));
		try {
			p.parse(in, "");
		} catch (RuntimeException ex) {
			if ("wrapped MalformedNanopubException".equals(ex.getMessage()) &&
					ex.getCause() instanceof MalformedNanopubException) {
				throw (MalformedNanopubException) ex.getCause();
			}
		} finally {
			in.close();
		}
	}

	private NanopubHandler npHandler;

	private URI headUri = null;
	private URI nanopubUri = null;
	private URI nanopubCollUri = null;
	private URI nanopubCollAssertionUri = null;
	private URI nanopubAssertionSetUri = null;
	private URI nanopubCollProvenanceUri = null;
	private URI nanopubCollPubInfoUri = null;
	private boolean headComplete = false;
	private Map<URI,Boolean> graphs = new HashMap<>();
	private Map<URI,Boolean> assertionSet = new HashMap<>();
	private Set<Statement> statements = new HashSet<>();
	private Set<Statement> collAssertionStatements = new HashSet<>();
	private List<String> nsPrefixes = new ArrayList<>();
	private Map<String,String> ns = new HashMap<>();

	private List<String> newNsPrefixes = new ArrayList<>();
	private List<String> newNs = new ArrayList<>();

	public MultiNanopubRdfHandler(NanopubHandler npHandler) {
		this.npHandler = npHandler;
	}

	@Override
	public void handleStatement(Statement st) throws RDFHandlerException {
		if (!headComplete) {
			if (headUri == null) {
				headUri = (URI) st.getContext();
				graphs.put(headUri, true);
			}
			if (headUri.equals(st.getContext())) {
				URI p = st.getPredicate();
				if (p.equals(RDF.TYPE) && st.getObject().equals(NANOPUB_TYPE_URI)) {
					nanopubUri = (URI) st.getSubject();
				}
				if (p.equals(RDF.TYPE) && st.getObject().equals(NANOPUBCOLL_TYPE_URI)) {
					nanopubCollUri = (URI) st.getSubject();
				}
				if (p.equals(HAS_ASSERTION_URI) || p.equals(HAS_PROVENANCE_URI) || p.equals(HAS_PUBINFO_URI)) {
					graphs.put((URI) st.getObject(), true);
				}
				if (p.equals(HAS_ASSERTIONSET_URI)) {
					nanopubAssertionSetUri = (URI) st.getObject();
				}
				if (p.equals(HAS_COLLPROVENANCE_URI)) {
					nanopubCollProvenanceUri = (URI) st.getObject();
				}
				if (p.equals(HAS_COLLPUBINFO_URI)) {
					nanopubCollPubInfoUri = (URI) st.getObject();
				}
				if (p.equals(HAS_MEMBER) && nanopubAssertionSetUri != null) {
					// Only works when assertion set is introduced before its members...
					if (st.getSubject().equals(nanopubAssertionSetUri)) {
						addAssertionMember((URI) st.getObject());
					}
				}
			} else {
				if (nanopubUri == null && nanopubCollUri == null) {
					throwMalformed("No nanopub (collection) URI found");
				} else if (nanopubUri != null && nanopubCollUri != null) {
					throwMalformed("Nanopub URI and nanopub collection URI found");
				}
				headComplete = true;
			}
		}
		if (headComplete) {
			if (nanopubUri != null) {
				if (!graphs.containsKey(st.getContext())) {
					finishAndReset();
					handleStatement(st);
				} else {
					addNamespaces();
					statements.add(st);
				}
			} else if (nanopubCollUri != null) {
				URI c = (URI) st.getContext();
				if (assertionSet.containsKey(c)) {
					if (nanopubCollAssertionUri != null && !c.equals(nanopubCollAssertionUri)) {
						finishNanopubFromCollection();
					}
					nanopubCollAssertionUri = c;
					addNamespaces();
					collAssertionStatements.add(st);
				} else if (nanopubCollAssertionUri == null) {
					if (!c.equals(nanopubCollProvenanceUri) && !c.equals(nanopubCollPubInfoUri)) {
						throwMalformed("Nanopub URI and nanopub collection URI found");
					}
					addNamespaces();
					statements.add(st);
				} else {
					finishAndReset();
					handleStatement(st);
				}
			}
		} else {
			addNamespaces();
			statements.add(st);
		}
	}

	@Override
	public void handleNamespace(String prefix, String uri) throws RDFHandlerException {
		newNs.add(uri);
		newNsPrefixes.add(prefix);
	}

	public void addNamespaces() throws RDFHandlerException {
		for (int i = 0 ; i < newNs.size() ; i++) {
			String prefix = newNsPrefixes.get(i);
			String nsUri = newNs.get(i);
			nsPrefixes.remove(prefix);
			nsPrefixes.add(prefix);
			ns.put(prefix, nsUri);
		}
		newNs.clear();
		newNsPrefixes.clear();
	}

	@Override
	public void endRDF() throws RDFHandlerException {
		finishAndReset();
	}

	private void finishAndReset() {
		try {
			if (nanopubCollUri != null) {
				finishNanopubFromCollection();
			} else {
				npHandler.handleNanopub(new NanopubImpl(statements, nsPrefixes, ns));
			}
		} catch (MalformedNanopubException ex) {
			throwMalformed(ex);
		}
		clearAll();
	}

	private void finishNanopubFromCollection() {
		List<Statement> l = new ArrayList<>();
		String s = nanopubCollAssertionUri.toString().replaceFirst("assertion$", "").replaceFirst("a$", "");
		URI npUri = new URIImpl(s.replaceFirst("(#|\\.)$", ""));
		URI head = new URIImpl(s + "head");
		URI prov = new URIImpl(s + "prov");
		URI info = new URIImpl(s + "info");
		l.add(new ContextStatementImpl(npUri, RDF.TYPE, NANOPUB_TYPE_URI, head));
		l.add(new ContextStatementImpl(npUri, HAS_ASSERTION_URI, nanopubCollAssertionUri, head));
		l.add(new ContextStatementImpl(npUri, HAS_PROVENANCE_URI, prov, head));
		l.add(new ContextStatementImpl(npUri, HAS_PUBINFO_URI, info, head));
		l.addAll(collAssertionStatements);
		for (Statement st : statements) {
			Resource context = null;
			if (st.getContext().equals(nanopubCollProvenanceUri)) {
				context = prov;
			} else if (st.getContext().equals(nanopubCollPubInfoUri)) {
				context = info;
			} else if (st.getContext().equals(headUri)) {
				// ignore
			} else {
				throwMalformed("Unrecognized graph for statement: " + st);
			}
			if (context != null) {
				l.add(new ContextStatementImpl(st.getSubject(), st.getPredicate(), st.getObject(), context));
			}
		}
		l.add(new ContextStatementImpl(nanopubAssertionSetUri, HAS_MEMBER, nanopubCollAssertionUri, prov));
		l.add(new ContextStatementImpl(nanopubCollUri, HAS_MEMBER, npUri, info));
		try {
			npHandler.handleNanopub(new NanopubImpl(l, nsPrefixes, ns));
		} catch (MalformedNanopubException ex) {
			throwMalformed(ex);
		}
		// clear assertion graph:
		collAssertionStatements.clear();
	}

	private void clearAll() {
		headUri = null;
		nanopubUri = null;
		nanopubCollUri = null;
		nanopubCollAssertionUri = null;
		headComplete = false;
		graphs.clear();
		assertionSet.clear();
		statements.clear();
		collAssertionStatements.clear();
	}

	private void addAssertionMember(URI a) {
		String s = a.toString();
		if (!s.matches(".*[^a-zA-Z]a") && !s.matches(".*[^a-zA-Z]assertion")) {
			throwMalformed("Invalid assertion set member URI: " + s);
		}
		assertionSet.put(a, true);
	}

	private void throwMalformed(MalformedNanopubException ex) {
		throw new RuntimeException("wrapped MalformedNanopubException", ex);
	}

	private void throwMalformed(String message) {
		throw new RuntimeException("wrapped MalformedNanopubException", new MalformedNanopubException(message));
	}

	
	public interface NanopubHandler {

		public void handleNanopub(Nanopub np);

	}

}
