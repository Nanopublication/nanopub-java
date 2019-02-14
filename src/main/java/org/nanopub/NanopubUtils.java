package org.nanopub;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandler;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.BasicParserSettings;
import org.nanopub.trusty.TrustyNanopubUtils;

/**
 * @author Tobias Kuhn
 */
public class NanopubUtils {

	private NanopubUtils() {}  // no instances allowed

	public static List<Statement> getStatements(Nanopub nanopub) {
		List<Statement> s = new ArrayList<>();
		s.addAll(getSortedList(nanopub.getHead()));
		s.addAll(getSortedList(nanopub.getAssertion()));
		s.addAll(getSortedList(nanopub.getProvenance()));
		s.addAll(getSortedList(nanopub.getPubinfo()));
		return s;
	}

	private static List<Statement> getSortedList(Set<Statement> s) {
		List<Statement> l = new ArrayList<Statement>(s);
		Collections.sort(l, new Comparator<Statement>() {

			@Override
			public int compare(Statement st1, Statement st2) {
				// TODO better sorting
				return st1.toString().compareTo(st2.toString());
			}

		});
		return l;
	}

	public static void writeToStream(Nanopub nanopub, OutputStream out, RDFFormat format)
			throws RDFHandlerException {
		writeNanopub(nanopub,format, new OutputStreamWriter(out, Charset.forName("UTF-8")));
	}

	public static String writeToString(Nanopub nanopub, RDFFormat format) throws RDFHandlerException {
		StringWriter sw = new StringWriter();
		writeNanopub(nanopub, format, sw);
		return sw.toString();
	}

	private static void writeNanopub(Nanopub nanopub, RDFFormat format, Writer writer)
			throws RDFHandlerException {
		if (format.equals(TrustyNanopubUtils.STNP_FORMAT)) {
			try {
				writer.write(TrustyNanopubUtils.getTrustyDigestString(nanopub));
				writer.flush();
				writer.close();
			} catch (IOException ex) {
				throw new RuntimeException(ex);
			}
		} else {
			RDFWriter rdfWriter = Rio.createWriter(format, writer);
			propagateToHandler(nanopub, rdfWriter);
		}
	}

	public static void propagateToHandler(Nanopub nanopub, RDFHandler handler)
			throws RDFHandlerException {
		handler.startRDF();
		String s = nanopub.getUri().toString();
		if (nanopub instanceof NanopubWithNs && !((NanopubWithNs) nanopub).getNsPrefixes().isEmpty()) {
			NanopubWithNs np = (NanopubWithNs) nanopub;
			for (String p : np.getNsPrefixes()) {
				handler.handleNamespace(p, np.getNamespace(p));
			}
		} else {
			handler.handleNamespace("this", s);
			handler.handleNamespace("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
			handler.handleNamespace("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
			handler.handleNamespace("rdfg", "http://www.w3.org/2004/03/trix/rdfg-1/");
			handler.handleNamespace("xsd", "http://www.w3.org/2001/XMLSchema#");
			handler.handleNamespace("owl", "http://www.w3.org/2002/07/owl#");
			handler.handleNamespace("dct", "http://purl.org/dc/terms/");
			handler.handleNamespace("dce", "http://purl.org/dc/elements/1.1/");
			handler.handleNamespace("pav", "http://purl.org/pav/");
			handler.handleNamespace("np", "http://www.nanopub.org/nschema#");
		}
		for (Statement st : getStatements(nanopub)) {
			handler.handleStatement(st);
		}
		handler.endRDF();
	}

	public static RDFParser getParser(RDFFormat format) {
		RDFParser p = Rio.createParser(format);
		p.getParserConfig().set(BasicParserSettings.NAMESPACES, new HashSet<Namespace>());
		return p;
	}

	public static Set<String> getUsedPrefixes(NanopubWithNs np) {
		Set<String> usedPrefixes = new HashSet<String>();
		CustomTrigWriter writer = new CustomTrigWriter(usedPrefixes);
		try {
			NanopubUtils.propagateToHandler(np, writer);
		} catch (RDFHandlerException ex) {
			ex.printStackTrace();
			return usedPrefixes;
		}
		return usedPrefixes;
	}

}
