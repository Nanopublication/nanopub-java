package org.nanopub;

import java.io.OutputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.openrdf.model.Statement;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFWriter;
import org.openrdf.rio.RDFWriterRegistry;
import org.openrdf.rio.Rio;

/**
 * @author Tobias Kuhn
 */
public class NanopubUtils {

	static {
		RDFWriterRegistry.getInstance().add(new CustomTrigWriterFactory());
	}

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
		RDFWriter writer = Rio.createWriter(format, out);
		propagateToHandler(nanopub, writer);
	}

	public static String writeToString(Nanopub nanopub, RDFFormat format)
			throws RDFHandlerException {
		StringWriter sw = new StringWriter();
		RDFWriter writer = Rio.createWriter(format, sw);
		propagateToHandler(nanopub, writer);
		return sw.toString();
	}

	public static void propagateToHandler(Nanopub nanopub, RDFHandler handler)
			throws RDFHandlerException {
		handler.startRDF();
		String s = nanopub.getUri().toString();
		if (nanopub instanceof NanopubImpl && !((NanopubImpl) nanopub).getNsPrefixes().isEmpty()) {
			NanopubImpl np = (NanopubImpl) nanopub;
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
			handler.handleNamespace("dc", "http://purl.org/dc/terms/");
			handler.handleNamespace("pav", "http://purl.org/pav/");
			handler.handleNamespace("np", "http://www.nanopub.org/nschema#");
		}
		for (Statement st : getStatements(nanopub)) {
			handler.handleStatement(st);
		}
		handler.endRDF();
	}

}
