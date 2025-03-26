package org.nanopub.op;

import com.beust.jcommander.ParameterException;
import net.trustyuri.TrustyUriException;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.Rio;
import org.nanopub.*;
import org.nanopub.Run;
import org.nanopub.MultiNanopubRdfHandler.NanopubHandler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

public class Count extends CliRunner {

	@com.beust.jcommander.Parameter(description = "input-nanopubs")
	private List<File> inputNanopubs = new ArrayList<File>();

	@com.beust.jcommander.Parameter(names = "-r", description = "Append line to this table file")
	private File tableFile;

	@com.beust.jcommander.Parameter(names = "--in-format", description = "Format of the input nanopubs: trig, nq, trix, trig.gz, ...")
	private String inFormat;

	public static void main(String[] args) {
		try {
			Count obj = Run.initJc(new Count(), args);
			obj.run();
		} catch (ParameterException ex) {
			System.exit(1);
		} catch (Exception ex) {
			ex.printStackTrace();
			System.exit(1);
		}
	}

	public static Count getInstance(String args) throws ParameterException {
		if (args == null) {
			args = "";
		}
		Count obj = Run.initJc(new Count(), args.trim().split(" "));
		return obj;
	}

	private RDFFormat rdfInFormat;
	private int npCount, headCount, assertionCount, provCount, pubinfoCount;

	public void run() throws IOException, RDFParseException, RDFHandlerException,
			MalformedNanopubException, TrustyUriException {
		if (inputNanopubs == null || inputNanopubs.isEmpty()) {
			throw new ParameterException("No input files given");
		}
		for (File inputFile : inputNanopubs) {
			npCount = 0;
			headCount = 0;
			assertionCount = 0;
			pubinfoCount = 0;

			if (inFormat != null) {
				rdfInFormat = Rio.getParserFormatForFileName("file." + inFormat).orElse(null);
			} else {
				rdfInFormat = Rio.getParserFormatForFileName(inputFile.toString()).orElse(null);
			}

			MultiNanopubRdfHandler.process(rdfInFormat, inputFile, new NanopubHandler() {

				@Override
				public void handleNanopub(Nanopub np) {
					countTriples(np);
				}

			});
			if (tableFile == null) {
				System.out.println("Nanopublications: " + npCount);
				System.out.println("Head triples: " + headCount + " (average: " + ((((float) headCount)) / npCount) + ")");
				System.out.println("Assertion triples: " + assertionCount + " (average: " + ((((float) assertionCount)) / npCount) + ")");
				System.out.println("Provenance triples: " + provCount + " (average: " + ((((float) provCount)) / npCount) + ")");
				System.out.println("Pubinfo triples: " + pubinfoCount + " (average: " + ((((float) pubinfoCount)) / npCount) + ")");
				int t = headCount + assertionCount + provCount + pubinfoCount;
				System.out.println("Total triples: " + t + " (average: " + ((((float) t)) / npCount) + ")");
			} else {
				PrintStream st = new PrintStream(new FileOutputStream(tableFile, true));
				st.println(inputFile.getName() + "," + npCount + "," + headCount + "," + assertionCount + "," + provCount + "," + pubinfoCount);
				st.close();
			}
		}
	}

	public void countTriples(Nanopub np) {
		npCount++;
		headCount += np.getHead().size();
		assertionCount += np.getAssertion().size();
		provCount += np.getProvenance().size();
		pubinfoCount += np.getPubinfo().size();
	}

}
