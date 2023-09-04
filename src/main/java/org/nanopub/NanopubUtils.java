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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandler;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.BasicParserSettings;
import org.nanopub.extra.security.KeyDeclaration;
import org.nanopub.trusty.TrustyNanopubUtils;

/**
 * @author Tobias Kuhn
 */
public class NanopubUtils {

	private NanopubUtils() {}  // no instances allowed

	private static final List<Pair<String,String>> defaultNamespaces = new ArrayList<>();

	static {
		defaultNamespaces.add(Pair.of("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#"));
		defaultNamespaces.add(Pair.of("rdfs", "http://www.w3.org/2000/01/rdf-schema#"));
		defaultNamespaces.add(Pair.of("rdfg", "http://www.w3.org/2004/03/trix/rdfg-1/"));
		defaultNamespaces.add(Pair.of("xsd", "http://www.w3.org/2001/XMLSchema#"));
		defaultNamespaces.add(Pair.of("owl", "http://www.w3.org/2002/07/owl#"));
		defaultNamespaces.add(Pair.of("dct", "http://purl.org/dc/terms/"));
		defaultNamespaces.add(Pair.of("dce", "http://purl.org/dc/elements/1.1/"));
		defaultNamespaces.add(Pair.of("pav", "http://purl.org/pav/"));
		defaultNamespaces.add(Pair.of("prov", "http://www.w3.org/ns/prov#"));
		defaultNamespaces.add(Pair.of("np", "http://www.nanopub.org/nschema#"));
	}

	public static List<Pair<String,String>> getDefaultNamespaces() {
		return defaultNamespaces;
	}

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
		writeNanopub(nanopub, format, new OutputStreamWriter(out, Charset.forName("UTF-8")));
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
		if (nanopub instanceof NanopubWithNs && !((NanopubWithNs) nanopub).getNsPrefixes().isEmpty()) {
			NanopubWithNs np = (NanopubWithNs) nanopub;
			for (String p : np.getNsPrefixes()) {
				handler.handleNamespace(p, np.getNamespace(p));
			}
		} else {
			handler.handleNamespace("this", nanopub.getUri().toString());
			for (Pair<String,String> p : defaultNamespaces) {
				handler.handleNamespace(p.getLeft(), p.getRight());
			}
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

	public static String getLabel(Nanopub np) {
		String npLabel = "", npTitle = "", aLabel = "", aTitle = "", introLabel = "";
		final IRI npId = np.getUri();
		final IRI aId = np.getAssertionUri();
		final Map<IRI,Boolean> introMap = new HashMap<>();
		for (Statement st : np.getPubinfo()) {
			final Resource subj = st.getSubject();
			final IRI pred = st.getPredicate();
			final Value obj = st.getObject();
			if (subj.equals(npId) && pred.equals(RDFS.LABEL) && obj instanceof Literal) {
				npLabel += " " + obj.stringValue();
			}
			if (subj.equals(npId) && (pred.equals(DCTERMS.TITLE) || pred.equals(DCE_TITLE)) && obj instanceof Literal) {
				npTitle += " " + obj.stringValue();
			}
			if (subj.equals(npId) && (pred.equals(INTRODUCES) || pred.equals(DESCRIBES)) && obj instanceof IRI) {
				introMap.put((IRI) obj, true);
			}
		}
		for (Statement st : np.getProvenance()) {
			final Resource subj = st.getSubject();
			final IRI pred = st.getPredicate();
			final Value obj = st.getObject();
			if (subj.equals(aId) && pred.equals(RDFS.LABEL) && obj instanceof Literal) {
				aLabel += " " + obj.stringValue();
			}
			if (subj.equals(aId) && pred.equals(DCTERMS.TITLE) && obj instanceof Literal) {
				aTitle += " " + obj.stringValue();
			}
		}
		for (Statement st : np.getAssertion()) {
			final Resource subj = st.getSubject();
			final IRI pred = st.getPredicate();
			final Value obj = st.getObject();
			if (subj.equals(aId) && pred.equals(RDFS.LABEL) && obj instanceof Literal) {
				aLabel += " " + obj.stringValue();
			}
			if (subj.equals(aId) && (pred.equals(DCTERMS.TITLE) || pred.equals(DCE_TITLE)) && obj instanceof Literal) {
				aTitle += " " + obj.stringValue();
			}
			if (introMap.containsKey(subj) && pred.equals(RDFS.LABEL) && obj instanceof Literal) {
				introLabel += " " + obj.stringValue();
			}
		}
		if (!npLabel.isEmpty()) return npLabel.substring(1);
		if (!npTitle.isEmpty()) return npTitle.substring(1);
		if (!aLabel.isEmpty()) return aLabel.substring(1);
		if (!aTitle.isEmpty()) return aTitle.substring(1);
		if (!introLabel.isEmpty()) return introLabel.substring(1);
		return null;
	}

	public static Set<IRI> getTypes(Nanopub np) {
		final Set<IRI> types = new HashSet<>();
		final IRI npId = np.getUri();
		final IRI aId = np.getAssertionUri();
		final Map<IRI,Boolean> introMap = new HashMap<>();
		for (Statement st : np.getPubinfo()) {
			final Resource subj = st.getSubject();
			final IRI pred = st.getPredicate();
			final Value obj = st.getObject();
			if (subj.equals(npId) && pred.equals(RDF.TYPE) && obj instanceof IRI) {
				types.add((IRI) obj);
			}
			if (subj.equals(npId) && pred.equals(HAS_NANOPUB_TYPE) && obj instanceof IRI) {
				types.add((IRI) obj);
			}
			if (subj.equals(npId) && (pred.equals(INTRODUCES) || pred.equals(DESCRIBES)) && obj instanceof IRI) {
				introMap.put((IRI) obj, true);
			}
		}
		for (Statement st : np.getProvenance()) {
			final Resource subj = st.getSubject();
			final IRI pred = st.getPredicate();
			final Value obj = st.getObject();
			if (subj.equals(aId) && pred.equals(RDF.TYPE) && obj instanceof IRI) {
				types.add((IRI) obj);
			}
		}
		IRI onlySubjectInAssertion = null;
		List<IRI> allTypes = new ArrayList<>();
		boolean hasOnlySubjectInAssertion = true;
		IRI onlyPredicateInAssertion = null;
		boolean hasOnlyPredicateInAssertion = true;
		for (Statement st : np.getAssertion()) {
			final IRI subj = (IRI) st.getSubject();
			final IRI pred = st.getPredicate();
			final Value obj = st.getObject();
			if (pred.equals(RDF.TYPE) && obj instanceof IRI) {
				allTypes.add((IRI) obj);
				if (subj.equals(aId)) types.add((IRI) obj);
				if (introMap.containsKey(subj)) types.add((IRI) obj);
			}
			if (pred.equals(KeyDeclaration.DECLARED_BY)) {
				// This predicate is used in introduction nanopubs for users. To simplify backwards compatibility,
				// this predicate is treated as a special case that triggers a type assignment.
				types.add(pred);
			}
			if (onlySubjectInAssertion == null) {
				onlySubjectInAssertion = subj;
			} else if (!onlySubjectInAssertion.equals(subj)) {
				hasOnlySubjectInAssertion = false;
			}
			if (onlyPredicateInAssertion == null) {
				onlyPredicateInAssertion = pred;
			} else if (!onlyPredicateInAssertion.equals(pred)) {
				hasOnlyPredicateInAssertion = false;
			}
		}
		if (hasOnlySubjectInAssertion) types.addAll(allTypes);
		if (hasOnlyPredicateInAssertion) types.add(onlyPredicateInAssertion);
		return types;
	}

	private static final ValueFactory vf = SimpleValueFactory.getInstance();
	private static final IRI INTRODUCES = vf.createIRI("http://purl.org/nanopub/x/introduces");
	private static final IRI DESCRIBES = vf.createIRI("http://purl.org/nanopub/x/describes");
	private static final IRI HAS_NANOPUB_TYPE = vf.createIRI("http://purl.org/nanopub/x/hasNanopubType");
	private static final IRI DCE_TITLE = vf.createIRI("http://purl.org/dc/elements/1.1/title");

}
