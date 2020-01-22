package org.nanopub.op;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Random;
import java.util.zip.GZIPOutputStream;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.Rio;
import org.nanopub.MalformedNanopubException;
import org.nanopub.Nanopub;
import org.nanopub.NanopubCreator;
import org.nanopub.NanopubImpl;
import org.nanopub.NanopubUtils;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

import net.trustyuri.TrustyUriException;

public class Create {

	@com.beust.jcommander.Parameter(names = "-o", description = "Output file")
	private File outputFile;

	@com.beust.jcommander.Parameter(names = "--format", description = "Format of the output nanopubs: trig, nq, trix, trig.gz, ...")
	private String format;

	public static void main(String[] args) {
		NanopubImpl.ensureLoaded();
		Create obj = new Create();
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

	private RDFFormat rdfOutFormat;
	private OutputStream outputStream = System.out;
	private Random random = new Random();

	private void run() throws IOException, RDFParseException, RDFHandlerException,
			MalformedNanopubException, TrustyUriException {

		if (outputFile == null) {
			if (format == null) {
				format = "trig";
			}
			rdfOutFormat = Rio.getParserFormatForFileName("file." + format).orElse(null);
		} else {
			rdfOutFormat = Rio.getParserFormatForFileName(outputFile.getName()).orElse(null);
			if (outputFile.getName().endsWith(".gz")) {
				outputStream = new GZIPOutputStream(new FileOutputStream(outputFile));
			} else {
				outputStream = new FileOutputStream(outputFile);
			}
		}

		String npUri = "http://purl.org/nanopub/temp/" + Math.abs(random.nextInt()) + "/";
		IRI nanopubIri = vf.createIRI(npUri);
		IRI assertionIri = vf.createIRI(npUri + "assertion");
		IRI creatorIri = vf.createIRI(npUri + "creator");
		NanopubCreator npCreator = new NanopubCreator(nanopubIri);
		npCreator.setAssertionUri(assertionIri);
		npCreator.addAssertionStatement(assertionIri, RDFS.COMMENT, vf.createLiteral("Replace this with your assertion content."));
		npCreator.addProvenanceStatement(vf.createIRI("http://www.w3.org/ns/prov#hadPrimarySource"), creatorIri);
		npCreator.addTimestampNow();
		npCreator.addCreator(creatorIri);
		Nanopub np = npCreator.finalizeNanopub();
		NanopubUtils.writeToStream(np, outputStream, rdfOutFormat);

		outputStream.flush();
		if (outputStream != System.out) {
			outputStream.close();
		}
	}

	private static SimpleValueFactory vf = SimpleValueFactory.getInstance();

}
