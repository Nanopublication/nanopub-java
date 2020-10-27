package org.nanopub;

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

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.AbstractRDFHandler;

/**
 * Handles files or streams with a sequence of nanopubs.
 *
 * @author Tobias Kuhn
 */
public class MultiNanopubRdfHandler extends AbstractRDFHandler {

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
		RDFFormat format = Rio.getParserFormatForFileName(file.getName()).orElse(RDFFormat.TRIG);
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

	private Map<IRI,Boolean> graphs = new HashMap<>();
	private Map<IRI,Map<IRI,Boolean>> members = new HashMap<>();
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
		if (!graphs.containsKey(st.getContext())) {
			if (graphs.size() == 4) {
				finishAndReset();
				handleStatement(st);
				return;
			}
			graphs.put((IRI) st.getContext(), true);
		}
		addNamespaces();
		statements.add(st);
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
			if (ex.getMessage().equals("No content received for nanopub")) { // TODO: Improve this check!
				// ignore (a stream of zero nanopubs is also a valid nanopub stream)
			} else {
				throwMalformed(ex);
			}
		}
		clearAll();
	}

	private void clearAll() {
		graphs.clear();
		members.clear();
		statements.clear();
	}

	private void throwMalformed(MalformedNanopubException ex) {
		throw new RuntimeException("wrapped MalformedNanopubException", ex);
	}

	
	public interface NanopubHandler {

		public void handleNanopub(Nanopub np);

	}

}
