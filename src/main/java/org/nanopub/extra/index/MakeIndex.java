package org.nanopub.extra.index;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.nanopub.MultiNanopubRdfHandler;
import org.nanopub.MultiNanopubRdfHandler.NanopubHandler;
import org.nanopub.Nanopub;
import org.nanopub.NanopubUtils;
import org.openrdf.rio.RDFFormat;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

public class MakeIndex {

	@com.beust.jcommander.Parameter(description = "input-nanopub-files", required = true)
	private List<File> inputFiles = new ArrayList<File>();

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

	public static void main(String[] args) throws IOException {
		MakeIndex obj = new MakeIndex();
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

	private SimpleIndexCreator indexCreator;
	private FileWriter writer;
	private RDFFormat outFormat;

	private MakeIndex() {
	}

	private void init() throws IOException {
		outFormat = RDFFormat.forFileName(outputFile.getName());
		writer = new FileWriter(outputFile);

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
	}

	private void run() throws Exception {
		init();
		for (File f : inputFiles) {
			RDFFormat format = RDFFormat.forFileName(f.getName());
			MultiNanopubRdfHandler.process(format, f, new NanopubHandler() {
				@Override
				public void handleNanopub(Nanopub np) {
					indexCreator.addElement(np);
				}
			});
		}
		indexCreator.finalizeNanopub();
		writer.close();
	}

}
