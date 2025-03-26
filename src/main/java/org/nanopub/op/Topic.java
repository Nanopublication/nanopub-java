package org.nanopub.op;

import com.beust.jcommander.ParameterException;
import net.trustyuri.TrustyUriException;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.Rio;
import org.nanopub.CliRunner;
import org.nanopub.MalformedNanopubException;
import org.nanopub.MultiNanopubRdfHandler;
import org.nanopub.MultiNanopubRdfHandler.NanopubHandler;
import org.nanopub.Nanopub;
import org.nanopub.op.topic.DefaultTopics;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPOutputStream;

public class Topic extends CliRunner {

	@com.beust.jcommander.Parameter(description = "input-nanopubs")
	private List<File> inputNanopubs = new ArrayList<File>();

	@com.beust.jcommander.Parameter(names = "-o", description = "Output file")
	private File outputFile;

	@com.beust.jcommander.Parameter(names = "--in-format", description = "Format of the input nanopubs: trig, nq, trix, trig.gz, ...")
	private String inFormat;

	@com.beust.jcommander.Parameter(names = "-i", description = "Property URIs to ignore, separated by '|' (has no effect if -d is set)")
	private String ignoreProperties;

	@com.beust.jcommander.Parameter(names = "-h", description = "Topic handler class")
	private String handlerClass;

	public static void main(String[] args) {
		try {
			Topic obj = CliRunner.initJc(new Topic(), args);
			obj.run();
		} catch (ParameterException ex) {
			System.exit(1);
		} catch (Exception ex) {
			ex.printStackTrace();
			System.exit(1);
		}
	}

	public static Topic getInstance(String args) throws ParameterException {
		if (args == null) {
			args = "";
		}
		Topic obj = CliRunner.initJc(new Topic(), args.trim().split(" "));
		return obj;
	}

	private RDFFormat rdfInFormat;
	private OutputStream outputStream = System.out;
	private BufferedWriter writer;
	private TopicHandler topicHandler;

	private void init() {
		if (handlerClass != null && !handlerClass.isEmpty()) {
			String detectorClassName = handlerClass;
			if (!handlerClass.contains(".")) {
				detectorClassName = "org.nanopub.op.topic." + handlerClass;
			}
			try {
				topicHandler = (TopicHandler) Class.forName(detectorClassName).getConstructor().newInstance();
			} catch (ReflectiveOperationException ex) {
				throw new RuntimeException(ex);
			}
		} else {
			topicHandler = new DefaultTopics(ignoreProperties);
		}
	}

	public void run() throws IOException, RDFParseException, RDFHandlerException,
			MalformedNanopubException, TrustyUriException {
		if (inputNanopubs == null || inputNanopubs.isEmpty()) {
			throw new ParameterException("No input files given");
		}
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

			MultiNanopubRdfHandler.process(rdfInFormat, inputFile, new NanopubHandler() {

				@Override
				public void handleNanopub(Nanopub np) {
					try {
						writer.write(np.getUri() + " " + getTopic(np) + "\n");
					} catch (IOException ex) {
						throw new RuntimeException(ex);
					}
				}

			});

			writer.flush();
			if (outputStream != System.out) {
				writer.close();
			}
		}
	}

	public String getTopic(Nanopub np) {
		return topicHandler.getTopic(np);
	}


	public interface TopicHandler {

		public String getTopic(Nanopub np);

	}

}
