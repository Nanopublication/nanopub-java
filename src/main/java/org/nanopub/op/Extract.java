package org.nanopub.op;

import com.beust.jcommander.ParameterException;
import net.trustyuri.TrustyUriException;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.rio.*;
import org.nanopub.CliRunner;
import org.nanopub.MalformedNanopubException;
import org.nanopub.MultiNanopubRdfHandler;
import org.nanopub.MultiNanopubRdfHandler.NanopubHandler;
import org.nanopub.Nanopub;

import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPOutputStream;

public class Extract extends CliRunner {

	@com.beust.jcommander.Parameter(description = "input-nanopubs", required = true)
	private List<File> inputNanopubs = new ArrayList<>();

	@com.beust.jcommander.Parameter(names = "-a", description = "Extract assertion triples")
	private boolean extractAssertion = false;

	@com.beust.jcommander.Parameter(names = "-p", description = "Extract provenance triples")
	private boolean extractProvenance = false;

	@com.beust.jcommander.Parameter(names = "-i", description = "Extract publication info triples")
	private boolean extractPubinfo = false;

	@com.beust.jcommander.Parameter(names = "-h", description = "Extract head triples")
	private boolean extractHead = false;

	@com.beust.jcommander.Parameter(names = "-d", description = "Drop graph URIs")
	private boolean dropGraphs = false;

	@com.beust.jcommander.Parameter(names = "-o", description = "Output file")
	private File outputFile;

	@com.beust.jcommander.Parameter(names = "--in-format", description = "Format of the input nanopubs: trig, nq, trix, trig.gz, ...")
	private String inFormat;

	@com.beust.jcommander.Parameter(names = "--out-format", description = "Format of the output nanopubs: trig, nq, trix, trig.gz, ...")
	private String outFormat;

	public static void main(String[] args) {
		try {
			Extract obj = CliRunner.initJc(new Extract(), args);
			obj.run();
		} catch (ParameterException ex) {
			System.exit(1);
		} catch (Exception ex) {
			ex.printStackTrace();
			System.exit(1);
		}
	}

	private RDFFormat rdfInFormat, rdfOutFormat;
	private OutputStream outputStream = System.out;
	private RDFWriter writer;

	private void run() throws IOException, RDFParseException, RDFHandlerException,
			MalformedNanopubException, TrustyUriException {
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

			writer = Rio.createWriter(rdfOutFormat, new OutputStreamWriter(outputStream, Charset.forName("UTF-8")));
			writer.startRDF();

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

			writer.endRDF();

			outputStream.flush();
			if (outputStream != System.out) {
				outputStream.close();
			}
		}
	}

	private void process(Nanopub np) throws RDFHandlerException {
		if (extractAssertion) {
			for (Statement st : np.getAssertion()) {
				outputStatement(st);
			}
		}
		if (extractProvenance) {
			for (Statement st : np.getProvenance()) {
				outputStatement(st);
			}
		}
		if (extractPubinfo) {
			for (Statement st : np.getPubinfo()) {
				outputStatement(st);
			}
		}
		if (extractHead) {
			for (Statement st : np.getHead()) {
				outputStatement(st);
			}
		}
	}

	private void outputStatement(Statement st) throws RDFHandlerException {
		if (dropGraphs) {
			st = SimpleValueFactory.getInstance().createStatement(st.getSubject(), st.getPredicate(), st.getObject());
		}
		writer.handleStatement(st);
	}

}
