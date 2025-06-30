package org.nanopub.op;

import com.beust.jcommander.ParameterException;
import net.trustyuri.TrustyUriException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
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
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPOutputStream;

public class ExportJson extends CliRunner {

	@com.beust.jcommander.Parameter(description = "input-nanopubs", required = true)
	private List<File> inputNanopubs = new ArrayList<>();

	@com.beust.jcommander.Parameter(names = "-o", description = "Output file")
	private File outputFile;

	@com.beust.jcommander.Parameter(names = "--in-format", description = "Format of the input nanopubs: trig, nq, trix, trig.gz, ...")
	private String inFormat;

	public static void main(String[] args) {
		try {
			ExportJson obj = CliRunner.initJc(new ExportJson(), args);
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
	private BufferedWriter writer;
	private boolean isFirstNp;
	private boolean isFirstSt;

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

			writer = new BufferedWriter(new OutputStreamWriter(outputStream));
			writer.write("[\n");
			isFirstNp = true;

			MultiNanopubRdfHandler.process(rdfInFormat, inputFile, new NanopubHandler() {

				@Override
				public void handleNanopub(Nanopub np) {
					try {
						process(np);
					} catch (RDFHandlerException | IOException ex) {
						throw new RuntimeException(ex);
					}
                }

			});

			writer.write("\n]\n");

			writer.flush();
			if (outputStream != System.out) {
				writer.close();
			}
		}
	}

	private void process(Nanopub np) throws RDFHandlerException, IOException {
		IRI npUri = np.getUri();
		if (!isFirstNp) {
			writer.write(",\n");
		}
		isFirstNp = false;
		writer.write(" [\n");
		isFirstSt = true;
		for (Statement st : np.getAssertion()) {
			writeStatement(st, "npa", np.getAssertionUri(), npUri);
		}
		for (Statement st : np.getProvenance()) {
			writeStatement(st, "npp", np.getProvenanceUri(), npUri);
		}
		for (Statement st : np.getPubinfo()) {
			writeStatement(st, "npi", np.getPubinfoUri(), npUri);
		}
		writer.write("\n ]");
	}

	private void writeStatement(Statement st, String type, IRI graphUri, IRI npUri) throws RDFHandlerException, IOException {
		if (!isFirstSt) {
			writer.write(",\n");
		}
		isFirstSt = false;
		writer.write("  {\n");
		writer.write("  \"type\": \"" + type + "\",\n");
		writer.write("  \"s\": \"" + escape(st.getSubject()) + "\",\n");
		writer.write("  \"p\": \"" + escape(st.getPredicate()) + "\",\n");
		writer.write("  \"o\": \"" + escape(st.getObject()) + "\",\n");
		if (st.getObject() instanceof Literal l) {
            if (l.getLanguage() != null) {
				writer.write("  \"dt\": \"" + escape(l.getDatatype()) + "\",\n");
			} else {
				writer.write("  \"l\": \"" + l.getLanguage() + "\",\n");
			}
		}
		writer.write("  \"g\": \"" + graphUri + "\",\n");
		writer.write("  \"np\": \"" + npUri + "\"\n");
		writer.write("  }");
	}

	private String escape(Value v) {
		return v.stringValue().replace("\\","\\\\").replace("\"", "\\\"");
	}

}
