package org.nanopub.op;

import com.beust.jcommander.ParameterException;
import net.trustyuri.TrustyUriException;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.Rio;
import org.nanopub.CliRunner;
import org.nanopub.MalformedNanopubException;
import org.nanopub.MultiNanopubRdfHandler;
import org.nanopub.MultiNanopubRdfHandler.NanopubHandler;
import org.nanopub.Nanopub;

import java.io.*;
import java.util.*;
import java.util.zip.GZIPOutputStream;

public class Aggregate extends CliRunner {

	@com.beust.jcommander.Parameter(description = "input-nanopubs")
	private List<File> inputNanopubs = new ArrayList<File>();

	@com.beust.jcommander.Parameter(names = "-h", description = "Output file for aggregation of head graph")
	private File headOutputFile;

	@com.beust.jcommander.Parameter(names = "-a", description = "Output file for aggregation of assertion graph")
	private File assertionOutputFile;

	@com.beust.jcommander.Parameter(names = "-p", description = "Output file for aggregation of provenance graph")
	private File provOutputFile;

	@com.beust.jcommander.Parameter(names = "-i", description = "Output file for aggregation of pubinfo graph")
	private File pubinfoOutputFile;

	@com.beust.jcommander.Parameter(names = "--in-format", description = "Format of the input nanopubs: trig, nq, trix, trig.gz, ...")
	private String inFormat;

	public static void main(String[] args) {
		try {
			Aggregate obj = CliRunner.initJc(new Aggregate(), args);
			obj.run();
		} catch (ParameterException ex) {
			System.exit(1);
		} catch (Exception ex) {
			ex.printStackTrace();
			System.exit(1);
		}
	}

	public static Aggregate getInstance(String args) throws ParameterException {
		if (args == null) {
			args = "";
		}
		Aggregate obj = CliRunner.initJc(new Aggregate(), args.trim().split(" "));
		return obj;
	}

	private RDFFormat rdfInFormat;
	private Map<Statement,Integer> headCounts, assertionCounts, provCounts, pubinfoCounts;

	public void run() throws IOException, RDFParseException, RDFHandlerException,
			MalformedNanopubException, TrustyUriException {
		if (inputNanopubs == null || inputNanopubs.isEmpty()) {
			throw new ParameterException("No input files given");
		}
		if (headOutputFile != null) headCounts = new HashMap<>();
		if (assertionOutputFile != null) assertionCounts = new HashMap<>();
		if (provOutputFile != null) provCounts = new HashMap<>();
		if (pubinfoOutputFile != null) pubinfoCounts = new HashMap<>();
		for (File inputFile : inputNanopubs) {
			if (inFormat != null) {
				rdfInFormat = Rio.getParserFormatForFileName("file." + inFormat).orElse(null);
			} else {
				rdfInFormat = Rio.getParserFormatForFileName(inputFile.toString()).orElse(null);
			}

			MultiNanopubRdfHandler.process(rdfInFormat, inputFile, new NanopubHandler() {

				@Override
				public void handleNanopub(Nanopub np) {
					try {
						process(np);
					} catch (RDFHandlerException ex) {
						throw new RuntimeException(ex);
					} catch (IOException ex) {
						throw new RuntimeException(ex);
					}
				}

			});

		}
		writeStatementCounts(headCounts, headOutputFile);
		writeStatementCounts(assertionCounts, assertionOutputFile);
		writeStatementCounts(provCounts, provOutputFile);
		writeStatementCounts(pubinfoCounts, pubinfoOutputFile);
	}

	public void process(Nanopub np) throws RDFHandlerException, IOException {
		aggregate(np.getHead(), np, headCounts);
		aggregate(np.getAssertion(), np, assertionCounts);
		aggregate(np.getProvenance(), np, provCounts);
		aggregate(np.getPubinfo(), np, pubinfoCounts);
	}

	private void aggregate(Set<Statement> statements, Nanopub np, Map<Statement,Integer> statementCounts) throws IOException {
		if (statementCounts == null) return;
		for (Statement st : statements) {
			Statement pst = preprocessStatement(st, np);
			if (!statementCounts.containsKey(pst)) {
				statementCounts.put(pst, 1);
			} else {
				statementCounts.put(pst, statementCounts.get(pst) + 1);
			}
		}
	}

	private Statement preprocessStatement(Statement st, Nanopub np) {
		Resource subject = st.getSubject();
		IRI predicate = st.getPredicate();
		Value object = st.getObject();
		subject = (Resource) preprocessValue(subject, np);
		predicate = (IRI) preprocessValue(predicate, np);
		object = preprocessValue(object, np);
		return vf.createStatement(subject, predicate, object);
	}

	private static ValueFactory vf = SimpleValueFactory.getInstance();

	private static final IRI thisNanopub = vf.createIRI("http://example.org/npop-dummy-uri/this_nanopub");
	private static final IRI thisHead = vf.createIRI("http://example.org/npop-dummy-uri/this_head");
	private static final IRI thisAssertion = vf.createIRI("http://example.org/npop-dummy-uri/this_assertion");
	private static final IRI thisProvenance = vf.createIRI("thttp://example.org/npop-dummy-uri/his_provenance");
	private static final IRI thisPubinfo = vf.createIRI("http://example.org/npop-dummy-uri/this_pubinfo");

	private Value preprocessValue(Value v, Nanopub np) {
		if (np.getUri().equals(v)) {
			return thisNanopub;
		} else if (np.getHeadUri().equals(v)) {
			return thisHead;
		} else if (np.getAssertionUri().equals(v)) {
			return thisAssertion;
		} else if (np.getProvenanceUri().equals(v)) {
			return thisProvenance;
		} else if (np.getPubinfoUri().equals(v)) {
			return thisPubinfo;
		}
		return v;
	}

	private void writeStatementCounts(final Map<Statement,Integer> statementCounts, File outputFile) throws IOException {
		if (statementCounts == null) return;
		BufferedWriter w = makeWriter(outputFile);
		List<Statement> statementList = new ArrayList<>(statementCounts.keySet());
		Collections.sort(statementList, new Comparator<Statement>() {
			@Override
			public int compare(Statement st1, Statement st2) {
				return -statementCounts.get(st1).compareTo(statementCounts.get(st2));
			}
		});
		for (Statement st : statementList) {
			w.write(statementCounts.get(st) + " " + st.toString().replaceAll("http://example.org/npop-dummy-uri/", "") + "\n");
		}
		w.flush();
		w.close();
	}

	private BufferedWriter makeWriter(File f) throws IOException {
		if (f == null) return null;
		OutputStream stream = null;
		if (f.getName().endsWith(".gz")) {
			stream = new GZIPOutputStream(new FileOutputStream(f));
		} else {
			stream = new FileOutputStream(f);
		}
		return new BufferedWriter(new OutputStreamWriter(stream));
	}

}
