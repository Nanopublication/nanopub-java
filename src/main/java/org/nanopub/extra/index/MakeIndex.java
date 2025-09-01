package org.nanopub.extra.index;

import com.beust.jcommander.ParameterException;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.nanopub.*;
import org.nanopub.trusty.TempUriReplacer;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPOutputStream;

/**
 * Command-line tool to create an index nanopublication from a set of input nanopublications.
 */
public class MakeIndex extends CliRunner {

    @com.beust.jcommander.Parameter(description = "input-nanopub-files")
    private List<File> inputFiles = new ArrayList<>();

    @com.beust.jcommander.Parameter(names = "-fs", description = "Add index nanopubs from input files " +
                                                                 "as sub-indexes (instead of elements); has no effect if input file is plain-text list of URIs")
    private boolean useSubindexes = false;

    @com.beust.jcommander.Parameter(names = "-e", description = "Add given URIs as elements " +
                                                                "(in addition to the ones from the input files)")
    private List<String> elements = new ArrayList<>();

    @com.beust.jcommander.Parameter(names = "-s", description = "Add given URIs as sub-indexes " +
                                                                "(in addition to the ones from the input files, if given)")
    private List<String> subindexes = new ArrayList<>();

    @com.beust.jcommander.Parameter(names = "-x", description = "Set given URI as superseded index")
    private String supersededIndex;

    @com.beust.jcommander.Parameter(names = "-o", description = "Output file")
    private File outputFile = new File("index.trig");

    @com.beust.jcommander.Parameter(names = "-u", description = "Base URI for index nanopubs")
    private String baseUri = TempUriReplacer.tempUri + "index/";

    @com.beust.jcommander.Parameter(names = "-t", description = "Title of index")
    private String iTitle;

    @com.beust.jcommander.Parameter(names = "-d", description = "Description of index")
    private String iDesc;

    @com.beust.jcommander.Parameter(names = "-c", description = "Creator of index")
    private List<String> iCreators = new ArrayList<>();

    @com.beust.jcommander.Parameter(names = "-l", description = "License URI")
    private String licenseUri;

    @com.beust.jcommander.Parameter(names = "-a", description = "'See also' resources")
    private List<String> seeAlso = new ArrayList<>();

    @com.beust.jcommander.Parameter(names = "-p", description = "Make plain (non-trusty) index nanopublications")
    private boolean plainNanopub;

//	@com.beust.jcommander.Parameter(names = "--sig", description = "Path and file name of key files")
//	private boolean useSignature;
//
//	@com.beust.jcommander.Parameter(names = "--sig-key-file", description = "Path and file name of key files")
//	private String keyFilename;
//
//	@com.beust.jcommander.Parameter(names = "--sig-algorithm", description = "Signature algorithm: either RSA or DSA")
//	private SignatureAlgorithm algorithm;

    /**
     * Main method to run the MakeIndex tool.
     *
     * @param args command-line arguments
     * @throws java.io.IOException if an I/O error occurs
     */
    public static void main(String[] args) throws IOException {
        try {
            MakeIndex obj = CliRunner.initJc(new MakeIndex(), args);
            if (obj.inputFiles.isEmpty() && obj.elements.isEmpty() && obj.subindexes.isEmpty() && obj.supersededIndex == null) {
                obj.getJc().usage();
            }
            obj.run();
        } catch (ParameterException ex) {
            System.exit(1);
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }

    private SimpleIndexCreator indexCreator;
    private OutputStreamWriter writer;
    private RDFFormat outFormat;
    private int count;
//	private KeyPair key;

    private void init() throws IOException {
        count = 0;
        outFormat = Rio.getParserFormatForFileName(outputFile.getName()).orElse(RDFFormat.TRIG);
        if (outputFile.getName().endsWith(".gz")) {
            writer = new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(outputFile)), StandardCharsets.UTF_8);
        } else {
            writer = new OutputStreamWriter(new FileOutputStream(outputFile), StandardCharsets.UTF_8);
        }

        indexCreator = new SimpleIndexCreator(!plainNanopub) {

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
        if (licenseUri != null) {
            indexCreator.setLicense(SimpleValueFactory.getInstance().createIRI(licenseUri));
        }
        for (String sa : seeAlso) {
            indexCreator.addSeeAlsoUri(SimpleValueFactory.getInstance().createIRI(sa));
        }
    }

    private void run() throws Exception {
        init();

//		if (useSignature) {
//			if (algorithm == null) {
//				if (keyFilename == null) {
//					keyFilename = "~/.nanopub/id_rsa";
//					algorithm = SignatureAlgorithm.RSA;
//				} else if (keyFilename.endsWith("_rsa")) {
//					algorithm = SignatureAlgorithm.RSA;
//				} else if (keyFilename.endsWith("_dsa")) {
//					algorithm = SignatureAlgorithm.DSA;
//				} else {
//					// Assuming RSA if not other information is available
//					algorithm = SignatureAlgorithm.RSA;
//				}
//			} else if (keyFilename == null) {
//				keyFilename = "~/.nanopub/id_" + algorithm.name().toLowerCase();
//			}
//			key = SignNanopub.loadKey(keyFilename, algorithm);
//		}

        try {
            for (File f : inputFiles) {
                if (f.getName().endsWith(".txt")) {
                    try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                        String line;
                        while ((line = br.readLine()) != null) {
                            line = line.trim();
                            if (line.isEmpty()) continue;
                            // To allow for other content in the file, ignore everything after the first blank space:
                            if (line.contains(" ")) line = line.substring(0, line.indexOf(" "));
                            indexCreator.addElement(SimpleValueFactory.getInstance().createIRI(line));
                        }
                    }
                } else {
                    RDFFormat format = Rio.getParserFormatForFileName(f.getName()).orElse(RDFFormat.TRIG);
                    MultiNanopubRdfHandler.process(format, f, np -> {
                        if (useSubindexes && IndexUtils.isIndex(np)) {
                            try {
                                indexCreator.addSubIndex(IndexUtils.castToIndex(np));
                            } catch (MalformedNanopubException | NanopubAlreadyFinalizedException ex) {
                                throw new RuntimeException(ex);
                            }
                        } else {
                            indexCreator.addElement(np);
                        }
                        count++;
                        if (count % 100 == 0) {
                            System.err.print(count + " nanopubs...\r");
                        }
                    });
                }
            }
            for (String e : elements) {
                indexCreator.addElement(SimpleValueFactory.getInstance().createIRI(e));
            }
            for (String s : subindexes) {
                indexCreator.addSubIndex(SimpleValueFactory.getInstance().createIRI(s));
            }
            if (supersededIndex != null) {
                indexCreator.setSupersededIndex(SimpleValueFactory.getInstance().createIRI(supersededIndex));
            }
            indexCreator.finalizeNanopub();
        } finally {
            writer.close();
        }
    }

}
