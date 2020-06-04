package org.nanopub.op;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandler;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.nanopub.MalformedNanopubException;
import org.nanopub.Nanopub;
import org.nanopub.NanopubCreator;
import org.nanopub.NanopubImpl;
import org.nanopub.NanopubUtils;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

import net.trustyuri.TrustyUriException;

public class Build {

	@com.beust.jcommander.Parameter(description = "input-rdf-files", required = true)
	private List<File> inputRdfdFiles = new ArrayList<File>();

	@com.beust.jcommander.Parameter(names = "-o", description = "Output file")
	private File outputFile;

	@com.beust.jcommander.Parameter(names = "--in-format", description = "Format of the input RDF files: ttl, jsonld, nt, ttl.gz, ...")
	private String inFormat;

	@com.beust.jcommander.Parameter(names = "--out-format", description = "Format of the output nanopubs: trig, nq, trix, trig.gz, ...")
	private String outFormat;

	public static void main(String[] args) {
		NanopubImpl.ensureLoaded();
		Build obj = new Build();
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
	private Random random = new Random();
	private NanopubCreator npCreator;
	private IRI nanopubIri, assertionIri, creatorIri;
	private Resource previousSubj;

	private void run() throws IOException, RDFParseException, RDFHandlerException,
			MalformedNanopubException, TrustyUriException {

		for (File inputFile : inputRdfdFiles) {
			String dummyFileName;
			if (inFormat != null) {
				dummyFileName = "file." + inFormat;
			} else {
				dummyFileName = inputFile.toString();
			}
			rdfInFormat = Rio.getParserFormatForFileName(dummyFileName).orElse(null);
			InputStream inputStream;
			if (dummyFileName.matches(".*\\.(gz|gzip)")) {
				inputStream = new GZIPInputStream(new BufferedInputStream(new FileInputStream(inputFile)));
			} else {
				inputStream = new BufferedInputStream(new FileInputStream(inputFile));
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

			RDFParser parser = Rio.createParser(rdfInFormat);
			parser.setRDFHandler(new RDFHandler() {
				
				@Override
				public void startRDF() throws RDFHandlerException {}
				
				@Override
				public void handleStatement(Statement st) throws RDFHandlerException {
					processStatement(st);
				}
				
				@Override
				public void handleNamespace(String prefix, String uri) throws RDFHandlerException {
					processNamespace(prefix, uri);
				}
				
				@Override
				public void handleComment(String comment) throws RDFHandlerException {}
				
				@Override
				public void endRDF() throws RDFHandlerException {}

			});
			parser.parse(inputStream, null);
	
			finalizeNanopub();

			inputStream.close();
			outputStream.flush();
			if (outputStream != System.out) {
				outputStream.close();
			}
		}
	}

	private void processStatement(Statement st) {
		prepareCreator(st.getSubject());
		npCreator.addAssertionStatements(st);
	}

	private void processNamespace(String prefix, String uri) {
		prepareCreator(null);
		npCreator.addNamespace(prefix, uri);
	}

	private void prepareCreator(Resource subj) {
		if (previousSubj != null && subj != null && !subj.equals(previousSubj)) {
			finalizeNanopub();
		}
		if (npCreator == null) {
			// Init nanopub:
			String npUriString = "http://purl.org/nanopub/temp/" + Math.abs(random.nextInt()) + "/";
			nanopubIri = vf.createIRI(npUriString);
			assertionIri = vf.createIRI(npUriString + "assertion");
			creatorIri = vf.createIRI(npUriString + "creator");
			npCreator = new NanopubCreator(nanopubIri);
			npCreator.setAssertionUri(assertionIri);
			npCreator.addProvenanceStatement(vf.createIRI("http://www.w3.org/ns/prov#hadPrimarySource"), creatorIri);
			npCreator.addCreator(creatorIri);
		}
		previousSubj = subj;
	}

	private void finalizeNanopub() {
		npCreator.addTimestampNow();
		try {
			Nanopub np = npCreator.finalizeNanopub();
			NanopubUtils.writeToStream(np, outputStream, rdfOutFormat);
		} catch (MalformedNanopubException ex) {
			throw new RuntimeException(ex);
		}
		npCreator = null;
	}

	private static SimpleValueFactory vf = SimpleValueFactory.getInstance();

}
