package org.nanopub.extra.index;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import org.nanopub.MalformedNanopubException;
import org.nanopub.MultiNanopubRdfHandler;
import org.nanopub.MultiNanopubRdfHandler.NanopubHandler;
import org.nanopub.Nanopub;
import org.nanopub.NanopubUtils;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.Rio;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

public class MakeIndex {

	@com.beust.jcommander.Parameter(description = "input-nanopub-files")
	private List<File> inputFiles = new ArrayList<File>();

	@com.beust.jcommander.Parameter(names = "-fs", description = "Add index nanopubs from input files " +
			"as sub-indexes (instead of elements)")
	private boolean useSubindexes = false;

	@com.beust.jcommander.Parameter(names = "-e", description = "Add given URIs as elements " +
			"(in addition to the ones from the input files)")
	private List<String> elements = new ArrayList<>();

	@com.beust.jcommander.Parameter(names = "-s", description = "Add given URIs as sub indexes " +
			"(in addition to the ones from the input files, if given)")
	private List<String> subindexes = new ArrayList<>();

	@com.beust.jcommander.Parameter(names = "-o", description = "Output file")
	private File outputFile = new File("index.trig");

	@com.beust.jcommander.Parameter(names = "-u", description = "Base URI for index nanopubs")
	private String baseUri = "http://np.inn.ac/";

	@com.beust.jcommander.Parameter(names = "-t", description = "Title of index")
	private String iTitle;

	@com.beust.jcommander.Parameter(names = "-d", description = "Description of index")
	private String iDesc;

	@com.beust.jcommander.Parameter(names = "-c", description = "Creator of index")
	private List<String> iCreators = new ArrayList<>();

	@com.beust.jcommander.Parameter(names = "-a", description = "'See also' resources")
	private List<String> seeAlso = new ArrayList<>();

	public static void main(String[] args) throws IOException {
		MakeIndex obj = new MakeIndex();
		JCommander jc = new JCommander(obj);
		try {
			jc.parse(args);
		} catch (ParameterException ex) {
			jc.usage();
			System.exit(1);
		}
		if (obj.inputFiles.isEmpty() && obj.elements.isEmpty() && obj.subindexes.isEmpty()) {
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

	private SimpleIndexCreator indexCreator;
	private OutputStreamWriter writer;
	private RDFFormat outFormat;
	private int count;

	private MakeIndex() {
	}

	private void init() throws IOException {
		count = 0;
		outFormat = Rio.getParserFormatForFileName(outputFile.getName());
		if (outputFile.getName().endsWith(".gz")) {
			writer = new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(outputFile)));
		} else {
			writer = new FileWriter(outputFile);
		}

		indexCreator = new SimpleIndexCreator() {

			@Override
			public void handleIncompleteIndex(NanopubIndex npi) {
				try {
					writer.write(NanopubUtils.writeToString(npi, outFormat) + "\n\n");
				} catch (Exception ex) {
					throw new RuntimeException(ex);
				}
			}

			@Override
			public void handleCompleteIndex(NanopubIndex npi) {
				System.out.println("Index URI: " + npi.getUri());
				try {
					writer.write(NanopubUtils.writeToString(npi, outFormat) + "\n\n");
				} catch (Exception ex) {
					throw new RuntimeException(ex);
				}
			}

		};

		indexCreator.setBaseUri(baseUri);
		if (iTitle != null) {
			indexCreator.setTitle(iTitle);
		}
		if (iDesc != null) {
			indexCreator.setDescription(iDesc);
		}
		for (String creator : iCreators) {
			indexCreator.addCreator(creator);
		}
		for (String sa : seeAlso) {
			indexCreator.addSeeAlsoUri(new URIImpl(sa));
		}
	}

	private void run() throws Exception {
		init();
		for (File f : inputFiles) {
			RDFFormat format = Rio.getParserFormatForFileName(f.getName());
			MultiNanopubRdfHandler.process(format, f, new NanopubHandler() {
				@Override
				public void handleNanopub(Nanopub np) {
					if (useSubindexes && IndexUtils.isIndex(np)) {
						try {
							indexCreator.addSubIndex(IndexUtils.castToIndex(np));
						} catch (MalformedNanopubException ex) {
							throw new RuntimeException(ex);
						}
					} else {
						indexCreator.addElement(np);
					}
					count++;
					if (count % 100 == 0) {
						System.out.print(count + " nanopubs...\r");
					}
				}
			});
		}
		for (String e : elements) {
			indexCreator.addElement(new URIImpl(e));
		}
		for (String s : subindexes) {
			indexCreator.addSubIndex(new URIImpl(s));
		}
		indexCreator.finalizeNanopub();
		writer.close();
	}

}
