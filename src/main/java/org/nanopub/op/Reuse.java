package org.nanopub.op;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.Rio;
import org.nanopub.MalformedNanopubException;
import org.nanopub.MultiNanopubRdfHandler;
import org.nanopub.MultiNanopubRdfHandler.NanopubHandler;
import org.nanopub.Nanopub;
import org.nanopub.NanopubImpl;
import org.nanopub.NanopubRdfHandler;
import org.nanopub.NanopubUtils;
import org.nanopub.trusty.FixTrustyNanopub;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

import net.trustyuri.TrustyUriException;
import net.trustyuri.TrustyUriUtils;

public class Reuse {

	@com.beust.jcommander.Parameter(description = "input-nanopubs", required = true)
	private List<File> inputNanopubs = new ArrayList<File>();

	@com.beust.jcommander.Parameter(names = "-x", description = "Nanopubs to be reused (if not given, an initial dataset is created)")
	private File reuseNanopubFile;

	@com.beust.jcommander.Parameter(names = "-n", description = "Output new nanopubs")
	private boolean outputNew = false;

	@com.beust.jcommander.Parameter(names = "-o", description = "Output file (requires option -n to be set)")
	private File outputFile;

	@com.beust.jcommander.Parameter(names = "-a", description = "Output file of all nanopublications (-x file needs to be a full nanopub file)")
	private File allOutputFile;

	@com.beust.jcommander.Parameter(names = "-c", description = "Output cache file, which can afterwards be used for argument -x or to create an index)")
	private File cacheFile;

	@com.beust.jcommander.Parameter(names = "-r", description = "Append line to this table file")
	private File tableFile;

	@com.beust.jcommander.Parameter(names = "--in-format", description = "Format of the input nanopubs: trig, nq, trix, trig.gz, ...")
	private String inFormat;

	@com.beust.jcommander.Parameter(names = "--reuse-format", description = "Format of the nanopubs to be reused: trig, nq, trix, trig.gz, ...")
	private String reuseFormat;

	@com.beust.jcommander.Parameter(names = "--out-format", description = "Format of the output nanopubs: trig, nq, trix, trig.gz, ...")
	private String outFormat;

	@com.beust.jcommander.Parameter(names = "-f", description = "Fingerprinting options")
	private String fingerprintOptions;

	@com.beust.jcommander.Parameter(names = "-s", description = "Add npx:supersedes backlinks for changed nanopublications")
	private boolean addSupersedesBacklinks = false;

	@com.beust.jcommander.Parameter(names = "-t", description = "Topic options")
	private String topicOptions;

	public static void main(String[] args) {
		NanopubImpl.ensureLoaded();
		Reuse obj = new Reuse();
		JCommander jc = new JCommander(obj);
		try {
			jc.parse(args);
		} catch (ParameterException ex) {
			jc.usage();
			System.exit(1);
		}
		obj.init();
		try {
			obj.run();
		} catch (Exception ex) {
			ex.printStackTrace();
			System.exit(1);
		}
	}

	private static final String multipleNanopubs = "MULTI";
	private static final String matchedNanopub = "MATCHED";

	private RDFFormat rdfInFormat, rdfReuseFormat, rdfOutFormat;
	private PrintStream outputStream = System.out;
	private PrintStream allOutputStream;
	private PrintStream cacheStream;
	private Map<String,String> reusableNanopubs = new HashMap<>();
	private Map<String,String> existingTopics = new HashMap<>();
	private Map<String,String> reuseNanopubMap = new HashMap<>();
	private int reusableCount, uniqueReusableCount, inputCount, reuseCount, inTopicDuplCount, outTopicDuplCount, topicMatchErrors, topicMatchCount;
	private Fingerprint fingerprint;
	private Topic topic;

	private void init() {
		try {
			fingerprint = Fingerprint.getInstance(fingerprintOptions);
		} catch (ParameterException ex) {
			System.err.println(ex);
		}
		try {
			topic = Topic.getInstance(topicOptions);
		} catch (ParameterException ex) {
			System.err.println(ex);
		}
	}

	private void run() throws IOException, RDFParseException, RDFHandlerException,
			MalformedNanopubException, TrustyUriException {

		reusableCount = 0;
		uniqueReusableCount = 0;
		inputCount = 0;
		reuseCount = 0;
		inTopicDuplCount = 0;
		topicMatchCount = 0;
		topicMatchErrors = 0;


		if (outputFile == null) {
			if (outFormat == null) {
				outFormat = "trig";
			}
			rdfOutFormat = Rio.getParserFormatForFileName("file." + outFormat).orElse(null);
		} else {
			rdfOutFormat = Rio.getParserFormatForFileName(outputFile.getName()).orElse(null);
		}

		if (reuseNanopubFile == null) {
			// Initial dataset creation
		} else if (reuseNanopubFile.getName().endsWith(".txt") || reuseNanopubFile.getName().endsWith(".txt.gz")) {
			// Reuse nanopubs from cache file
			if (allOutputFile != null) {
				throw new RuntimeException("-x needs to specify a full nanopub file if -a is specified");
			}
			BufferedReader br = null;
			try {
				if (reuseNanopubFile.getName().endsWith(".gz")) {
					br = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(reuseNanopubFile))));
				} else {
					br = new BufferedReader(new FileReader(reuseNanopubFile));
				}
			    String line;
			    while ((line = br.readLine()) != null) {
			    	line = line.trim();
			    	if (line.isEmpty()) continue;
			    	String[] columns = line.split(" ");
			    	String uri = columns[0];
			    	String fingerprint = columns[1];
			    	reusableNanopubs.put(fingerprint, uri);
					reusableCount++;
					if (addSupersedesBacklinks) {
						String topic = columns[2];
						recordTopic(topic, uri);
					}
			    }
			} finally {
				if (br != null) br.close();
			}
		} else {
			// Reuse nanopubs from full nanopub file
			if (reuseFormat != null) {
				rdfReuseFormat = Rio.getParserFormatForFileName("file." + reuseFormat).orElse(null);
			} else {
				rdfReuseFormat = Rio.getParserFormatForFileName(reuseNanopubFile.toString()).orElse(null);
			}
			MultiNanopubRdfHandler.process(rdfReuseFormat, reuseNanopubFile, new NanopubHandler() {
	
				@Override
				public void handleNanopub(Nanopub np) {
					try {
						String fp = fingerprint.getFingerprint(np);
						System.err.println("FP: " + fp);
						String uri = np.getUri().toString();
						reusableNanopubs.put(fp, uri);
						reusableCount++;
						if (addSupersedesBacklinks) {
							recordTopic(topic.getTopic(np), uri);
						}
						if (allOutputFile != null) {
							reuseNanopubMap.put(fp, NanopubUtils.writeToString(np, rdfOutFormat));
						}
					} catch (IOException ex) {
						throw new RuntimeException(ex);
					} catch (RDFHandlerException ex) {
						throw new RuntimeException(ex);
					}
				}
	
			});
		}
		uniqueReusableCount = reusableNanopubs.size();

		// Reuse matching nanopubs:
		if (cacheFile != null) {
			if (cacheFile.getName().endsWith(".gz")) {
				cacheStream = new PrintStream(new GZIPOutputStream(new FileOutputStream(cacheFile)));
			} else {
				cacheStream = new PrintStream(new FileOutputStream(cacheFile));
			}
		}
		for (File inputFile : inputNanopubs) {
			if (inFormat != null) {
				rdfInFormat = Rio.getParserFormatForFileName("file." + inFormat).orElse(null);
			} else {
				rdfInFormat = Rio.getParserFormatForFileName(inputFile.toString()).orElse(null);
			}
			if (outputFile != null) {
				if (outputFile.getName().endsWith(".gz")) {
					outputStream = new PrintStream(new GZIPOutputStream(new FileOutputStream(outputFile)));
				} else {
					outputStream = new PrintStream(new FileOutputStream(outputFile));
				}
			}
			if (allOutputFile != null) {
				if (allOutputFile.getName().endsWith(".gz")) {
					allOutputStream = new PrintStream(new GZIPOutputStream(new FileOutputStream(allOutputFile)));
				} else {
					allOutputStream = new PrintStream(new FileOutputStream(allOutputFile));
				}
			}

			MultiNanopubRdfHandler.process(rdfInFormat, inputFile, new NanopubHandler() {

				@Override
				public void handleNanopub(Nanopub np) {
					try {
						process(np);
					} catch (Exception ex) {
						throw new RuntimeException(ex);
					}
				}

			});

			outputStream.flush();
			if (outputStream != System.out) {
				outputStream.close();
			}
			if (allOutputStream != null) {
				allOutputStream.flush();
				allOutputStream.close();
			}
			if (cacheStream != null) {
				cacheStream.flush();
				cacheStream.close();
			}

			if (tableFile != null) {
				PrintStream st = new PrintStream(new FileOutputStream(tableFile, true));
				if (addSupersedesBacklinks) {
					st.println(inputFile.getName() + "," + reusableCount + "," + inputCount + "," + reuseCount + "," + topicMatchCount + "," +
							inTopicDuplCount + "," + outTopicDuplCount + "," + topicMatchErrors);
				} else {
					st.println(inputFile.getName() + "," + reusableCount + "," + inputCount + "," + reuseCount);
				}
				st.close();
			}
			System.err.println("Older dataset count (unique): " + reusableCount + " (" + uniqueReusableCount + ")");
			System.err.println("Newer dataset count: " + inputCount);
			System.err.println("Reuse count: " + reuseCount);
			if (addSupersedesBacklinks) {
				System.err.println("Topic match count: " + topicMatchCount);
				System.err.println("Duplicate topics in older dataset: " + inTopicDuplCount);
				System.err.println("Duplicate topics in newer dataset: " + outTopicDuplCount);
				System.err.println("Total topic matching errors: " + topicMatchErrors);
			}
		}
	}

	private void recordTopic(String topic, String uri) {
		if (existingTopics.containsKey(topic)) {
			existingTopics.put(topic, multipleNanopubs);
			inTopicDuplCount++;
			topicMatchErrors++;
		} else {
			existingTopics.put(topic, uri);
		}
	}

	private void process(Nanopub np) throws IOException, RDFHandlerException, MalformedNanopubException, TrustyUriException {
		inputCount++;
		String fp = fingerprint.getFingerprint(np);
		System.err.println("FPX: " + fp);
		String t = null;
		if (addSupersedesBacklinks) {
			t = topic.getTopic(np);
		}
		String uri = np.getUri().toString();
		if (reusableNanopubs.containsKey(fp)) {
			reuseCount++;
			uri = reusableNanopubs.get(fp);
			if (addSupersedesBacklinks) {
				String et = existingTopics.get(t);
				if (et == multipleNanopubs || et == matchedNanopub) {
					topicMatchErrors++;
				}
				existingTopics.put(t, matchedNanopub);
			}
			if (allOutputStream != null) {
				allOutputStream.println(reuseNanopubMap.get(fp));
			}
		} else {
			if (addSupersedesBacklinks) {
				if (existingTopics.containsKey(t)) {
					String et = existingTopics.get(t);
					if (et == multipleNanopubs) {
						topicMatchErrors++;
					} else if (et == matchedNanopub) {
						topicMatchErrors++;
						outTopicDuplCount++;
					} else {
						topicMatchCount++;
						String oldNpUri = existingTopics.get(t);
						existingTopics.put(t, matchedNanopub);
						np = addSupersedesBacklink(np, SimpleValueFactory.getInstance().createIRI(oldNpUri));
						uri = np.getUri().toString();
					}
				}
			}
			if (outputNew) {
				NanopubUtils.writeToStream(np, outputStream, rdfOutFormat);
			}
			if (allOutputStream != null) {
				NanopubUtils.writeToStream(np, allOutputStream, rdfOutFormat);
			}
		}
		if (cacheStream != null) {
			if (addSupersedesBacklinks) {
				cacheStream.println(uri + " " + fp + " " + t);
			} else {
				cacheStream.println(uri + " " + fp);
			}
		}
	}

	private Nanopub addSupersedesBacklink(final Nanopub newNp, final IRI oldUri)
			throws RDFHandlerException, MalformedNanopubException, TrustyUriException {
		SupersedesLinkAdder linkAdder = new SupersedesLinkAdder(oldUri, newNp);
		NanopubUtils.propagateToHandler(newNp, linkAdder);
		if (TrustyUriUtils.isPotentialTrustyUri(newNp.getUri().stringValue())) {
			return FixTrustyNanopub.fix(linkAdder.getNanopub());
		} else {
			return linkAdder.getNanopub();
		}
	}

	
	private class SupersedesLinkAdder extends NanopubRdfHandler {

		private IRI oldUri;
		private Nanopub newNp;

		public SupersedesLinkAdder(IRI oldUri, Nanopub newNp) {
			this.oldUri = oldUri;
			this.newNp = newNp;
		}

		@Override
		public void handleStatement(Statement st) throws RDFHandlerException {
			if (st.getSubject().equals(newNp.getUri()) && st.getPredicate().equals(Nanopub.SUPERSEDES)) {
				// remove existing supersedes-triple
				return;
			}
			super.handleStatement(st);
		}

		@Override
		public void endRDF() throws RDFHandlerException {
			super.handleStatement(SimpleValueFactory.getInstance().createStatement(
					newNp.getUri(), Nanopub.SUPERSEDES, oldUri, newNp.getPubinfoUri()));
			super.endRDF();
		}

	}

}
