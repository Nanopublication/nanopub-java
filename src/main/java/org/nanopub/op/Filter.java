package org.nanopub.op;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.Rio;
import org.nanopub.MalformedNanopubException;
import org.nanopub.MultiNanopubRdfHandler;
import org.nanopub.MultiNanopubRdfHandler.NanopubHandler;
import org.nanopub.Nanopub;
import org.nanopub.NanopubImpl;
import org.nanopub.NanopubUtils;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

import net.trustyuri.TrustyUriException;

public class Filter {

	@com.beust.jcommander.Parameter(description = "input-nanopubs", required = true)
	private List<File> inputNanopubs = new ArrayList<File>();

	@com.beust.jcommander.Parameter(names = "-f", description = "Filter by URI or literal")
	private String filter = null;

	@com.beust.jcommander.Parameter(names = "-F", description = "Filter by URI or literal read from file")
	private File filterFile = null;

	@com.beust.jcommander.Parameter(names = "--split", description = "Treat blanks in filter string as OR connectives")
	private boolean splitFilter = false;

	@com.beust.jcommander.Parameter(names = "-o", description = "Output file")
	private File outputFile;

	@com.beust.jcommander.Parameter(names = "--no-examples", description = "Do not output example nanopublications")
	private boolean noExamples = false;

	@com.beust.jcommander.Parameter(names = "--only-examples", description = "Output only example nanopublications")
	private boolean onlyExamples = false;

	@com.beust.jcommander.Parameter(names = "--in-format", description = "Format of the input nanopubs: trig, nq, trix, trig.gz, ...")
	private String inFormat;

	@com.beust.jcommander.Parameter(names = "--out-format", description = "Format of the output nanopubs: trig, nq, trix, trig.gz, ...")
	private String outFormat;

	public static void main(String[] args) {
		NanopubImpl.ensureLoaded();
		Filter obj = new Filter();
		JCommander jc = new JCommander(obj);
		try {
			jc.parse(args);
		} catch (ParameterException ex) {
			jc.usage();
			System.exit(1);
		}
		try {
			obj.run();
		} catch (Exception ex) {
			ex.printStackTrace();
			System.exit(1);
		}
	}

	private RDFFormat rdfInFormat, rdfOutFormat;
	private OutputStream outputStream = System.out;
	private Map<String,Boolean> filterComponents = new HashMap<>();

	private void run() throws IOException, RDFParseException, RDFHandlerException,
			MalformedNanopubException, TrustyUriException {
		if (splitFilter) {
			for (String s : filter.split(" ")) {
				filterComponents.put(s, true);
			}
		} else {
			filterComponents.put(filter, true);
		}
		if (filterFile != null) {
			BufferedReader br = null;
			try {
				if (filterFile.getName().endsWith(".gz")) {
					br = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(filterFile))));
				} else {
					br = new BufferedReader(new FileReader(filterFile));
				}
			    String line;
			    while ((line = br.readLine()) != null) {
			    	line = line.trim();
			    	if (line.isEmpty()) continue;
			    	filterComponents.put(line, true);
			    }
			} finally {
				if (br != null) br.close();
			}
		}
		if (filter == null && filterFile == null) {
			filterComponents = null;
		}

		for (File inputFile : inputNanopubs) {
			if (inFormat != null) {
				rdfInFormat = Rio.getParserFormatForFileName("file." + inFormat).orElse(null);
			} else {
				rdfInFormat = Rio.getParserFormatForFileName(inputFile.toString()).orElse(null);
			}
			if (outputFile == null) {
				if (outFormat == null) {
					outFormat = "trig";
				}
				rdfOutFormat = Rio.getParserFormatForFileName("file." + outFormat).orElse(null);
			} else {
				rdfOutFormat = Rio.getParserFormatForFileName(outputFile.getName()).orElse(null);
				if (outputFile.getName().endsWith(".gz")) {
					outputStream = new GZIPOutputStream(new FileOutputStream(outputFile));
				} else {
					outputStream = new FileOutputStream(outputFile);
				}
			}

			MultiNanopubRdfHandler.process(rdfInFormat, inputFile, new NanopubHandler() {

				@Override
				public void handleNanopub(Nanopub np) {
					try {
						process(np);
					} catch (RDFHandlerException ex) {
						throw new RuntimeException(ex);
					}
				}

			});

			outputStream.flush();
			if (outputStream != System.out) {
				outputStream.close();
			}
		}
	}

	private void process(Nanopub np) throws RDFHandlerException {
		if (matchesFilter(np)) {
			NanopubUtils.writeToStream(np, outputStream, rdfOutFormat);
		}
	}

	private boolean matchesFilter(Nanopub np) {
		if (noExamples && isExampleNanopub(np)) {
			return false;
		}
		if (onlyExamples && !isExampleNanopub(np)) {
			return false;
		}
		if (filterComponents == null) return true;
		for (Statement st : NanopubUtils.getStatements(np)) {
			if (filterComponents.containsKey(st.getSubject().stringValue())) {
				return true;
			}
			if (filterComponents.containsKey(st.getPredicate().stringValue())) {
				return true;
			}
			if (filterComponents.containsKey(st.getObject().stringValue())) {
				return true;
			}
			if (filterComponents.containsKey(st.getContext().stringValue())) {
				return true;
			}
		}
		return false;
	}


	public static IRI exampleNanopubType = SimpleValueFactory.getInstance().createIRI("http://purl.org/nanopub/x/ExampleNanopub");

	public static boolean isExampleNanopub(Nanopub np) {
		if (np.getPubinfo().contains(SimpleValueFactory.getInstance().createStatement(np.getUri(), RDF.TYPE, exampleNanopubType))) {
			return true;
		}
		for (Statement st : NanopubUtils.getStatements(np)) {
			if (isExampleUri(st.getSubject())) return true;
			if (isExampleUri(st.getPredicate())) return true;
			if (isExampleUri(st.getObject())) return true;
		}
		return false;
	}

	public static boolean isExampleUri(Value v) {
		if (!(v instanceof IRI)) return false;
		if (v.stringValue().startsWith("http://example.org/")) return true;
		if (v.stringValue().startsWith("https://example.org/")) return true;
		if (v.stringValue().startsWith("http://example.com/")) return true;
		if (v.stringValue().startsWith("https://example.com/")) return true;
		return false;
	}

}
