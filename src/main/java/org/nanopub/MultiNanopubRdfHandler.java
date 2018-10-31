package org.nanopub;

import static org.nanopub.Nanopub.HAS_ASSERTION_URI;
import static org.nanopub.Nanopub.HAS_PROVENANCE_URI;
import static org.nanopub.Nanopub.HAS_PUBINFO_URI;
import static org.nanopub.Nanopub.NANOPUB_TYPE_URI;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import org.openrdf.model.Statement;
import org.openrdf.model.URI;
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
			p.parse(new InputStreamReader(in, Charset.forName("UTF-8")), "");
		} catch (RuntimeException ex) {
			if ("wrapped MalformedNanopubException".equals(ex.getMessage()) &&
					ex.getCause() instanceof MalformedNanopubException) {
				throw (MalformedNanopubException) ex.getCause();
			} else {
				throw ex;
			}
		} finally {
			in.close();
		}
	}

	private NanopubHandler npHandler;

	private URI headUri = null;
	private URI nanopubUri = null;
	private boolean headComplete = false;
	private Map<URI,Boolean> graphs = new HashMap<>();
	private Map<URI,Map<URI,Boolean>> members = new HashMap<>();
	private Set<Statement> statements = new LinkedHashSet<>();
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
				if (headUri == null) {
					throwMalformed("Triple without context found: " +
							st.getSubject() + " " + st.getPredicate() + " " + st.getObject());
				}
				graphs.put(headUri, true);
			}
			if (headUri.equals(st.getContext())) {
				URI p = st.getPredicate();
				if (p.equals(RDF.TYPE) && st.getObject().equals(NANOPUB_TYPE_URI)) {
					nanopubUri = (URI) st.getSubject();
				}
				if (p.equals(HAS_ASSERTION_URI) || p.equals(HAS_PROVENANCE_URI) || p.equals(HAS_PUBINFO_URI)) {
					graphs.put((URI) st.getObject(), true);
				}
			} else {
				if (nanopubUri == null) {
					throwMalformed("No nanopub (collection) URI found");
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
			npHandler.handleNanopub(new NanopubImpl(statements, nsPrefixes, ns));
		} catch (MalformedNanopubException ex) {
			throwMalformed(ex);
		}
		clearAll();
	}

	private void clearAll() {
		headUri = null;
		nanopubUri = null;
		headComplete = false;
		graphs.clear();
		members.clear();
		statements.clear();
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
