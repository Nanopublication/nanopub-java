package org.nanopub.op;

import com.beust.jcommander.ParameterException;
import net.trustyuri.TrustyUriException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.rio.*;
import org.nanopub.*;
import org.nanopub.MultiNanopubRdfHandler.NanopubHandler;
import org.nanopub.op.fingerprint.FingerprintHandler;

import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPOutputStream;

public class Decontextualize extends CliRunner {

	public static final IRI graphPlaceholer = SimpleValueFactory.getInstance().createIRI("http://purl.org/nanopub/placeholders/graph");

	@com.beust.jcommander.Parameter(description = "input-nanopubs", required = true)
	private List<File> inputNanopubs = new ArrayList<File>();

	@com.beust.jcommander.Parameter(names = "-o", description = "Output file")
	private File outputFile;

	@com.beust.jcommander.Parameter(names = "--in-format", description = "Format of the input nanopubs: trig, nq, trix, trig.gz, ...")
	private String inFormat;

	public static void main(String[] args) {
		try {
			Decontextualize obj = CliRunner.initJc(new Decontextualize(), args);
			obj.run();
		} catch (ParameterException ex) {
			System.exit(1);
		} catch (Exception ex) {
			ex.printStackTrace();
			System.exit(1);
		}
	}

	private RDFFormat rdfInFormat;
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
			if (outputFile != null) {
				if (outputFile.getName().endsWith(".gz")) {
					outputStream = new GZIPOutputStream(new FileOutputStream(outputFile));
				} else {
					outputStream = new FileOutputStream(outputFile);
				}
			}

			writer = Rio.createWriter(RDFFormat.NQUADS, new OutputStreamWriter(outputStream, Charset.forName("UTF-8")));
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
		for (Statement st : getNormalizedStatements(np)) {
			writer.handleStatement(st);
		}
	}

	private List<Statement> getNormalizedStatements(Nanopub np) {
		List<Statement> statements = NanopubUtils.getStatements(np);
		List<Statement> n = new ArrayList<>();
		for (Statement st : statements) {
			boolean isInHead = st.getContext().equals(np.getHeadUri());
			if (isInHead) continue;
			boolean isInProvenance = st.getContext().equals(np.getProvenanceUri());
			boolean isInPubinfo = st.getContext().equals(np.getPubinfoUri());
			IRI toBeReplacedUri = null;
			IRI replacementUri = null;
			if (isInProvenance) {
				toBeReplacedUri = np.getAssertionUri();
				replacementUri = FingerprintHandler.assertionUriPlaceholder;
			} else if (isInPubinfo) {
				toBeReplacedUri = np.getUri();
				replacementUri = FingerprintHandler.nanopubUriPlaceholder;
			}
			Resource subj = st.getSubject();
			IRI pred = st.getPredicate();
			Value obj = st.getObject();
			n.add(SimpleValueFactory.getInstance().createStatement(
					(Resource) transform(subj, toBeReplacedUri, replacementUri),
					(IRI) transform(pred, toBeReplacedUri, replacementUri),
					transform(obj, toBeReplacedUri, replacementUri),
					graphPlaceholer));
		}
		return n;
	}

	private Value transform(Value v, IRI toBeReplacedUri, IRI replacementUri) {
		if (toBeReplacedUri == null) return v;
		if (v.equals(toBeReplacedUri)) {
			return replacementUri;
		}
		return v;
	}

}
