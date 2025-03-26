package org.nanopub.trusty;

import com.beust.jcommander.ParameterException;
import net.trustyuri.TrustyUriException;
import net.trustyuri.TrustyUriResource;
import net.trustyuri.TrustyUriUtils;
import net.trustyuri.rdf.RdfFileContent;
import net.trustyuri.rdf.RdfUtils;
import org.eclipse.rdf4j.rio.*;
import org.nanopub.*;
import org.nanopub.MultiNanopubRdfHandler.NanopubHandler;

import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPOutputStream;

public class FixTrustyNanopub extends CliRunner {

	@com.beust.jcommander.Parameter(description = "input-nanopubs", required = true)
	private List<File> inputNanopubs = new ArrayList<File>();

	@com.beust.jcommander.Parameter(names = "-v", description = "Verbose")
	private boolean verbose = false;

	public static void main(String[] args) {
		try {
			FixTrustyNanopub obj = Run.initJc(new FixTrustyNanopub(), args);
			obj.run();
		} catch (ParameterException ex) {
			System.exit(1);
		} catch (Exception ex) {
			ex.printStackTrace();
			System.exit(1);
		}
	}

	private int count;

	private void run() throws IOException, RDFParseException, RDFHandlerException,
			MalformedNanopubException, TrustyUriException {
		for (File inputFile : inputNanopubs) {
			File outFile = new File(inputFile.getParent(), "fixed." + inputFile.getName());
			final OutputStream out;
			if (inputFile.getName().matches(".*\\.(gz|gzip)")) {
				out = new GZIPOutputStream(new FileOutputStream(outFile));
			} else {
				out = new FileOutputStream(outFile);
			}
			final RDFFormat format = new TrustyUriResource(inputFile).getFormat(RDFFormat.TRIG);
			try (out) {
				MultiNanopubRdfHandler.process(format, inputFile, new NanopubHandler() {
	
					@Override
					public void handleNanopub(Nanopub np) {
						try {
							np = writeAsFixedNanopub(np, format, out);
							count++;
							if (verbose) {
								System.out.println("Nanopub URI: " + np.getUri());
							} else {
								if (count % 100 == 0) {
									System.err.print(count + " nanopubs...\r");
								}
							}
						} catch (RDFHandlerException ex) {
							throw new RuntimeException(ex);
						} catch (TrustyUriException ex) {
							throw new RuntimeException(ex);
						}
					}
	
				});
			}
		}
	}

	public static Nanopub fix(Nanopub nanopub) throws TrustyUriException {
		Nanopub np;
		if (nanopub instanceof NanopubWithNs) {
			((NanopubWithNs) nanopub).removeUnusedPrefixes();
		}
		try {
			RdfFileContent r = new RdfFileContent(RDFFormat.TRIG);
			NanopubUtils.propagateToHandler(nanopub, r);
			NanopubRdfHandler h = new NanopubRdfHandler();
			if (!TrustyUriUtils.isPotentialTrustyUri(nanopub.getUri())) {
				throw new TrustyUriException("Not a (broken) trusty URI: " + nanopub.getUri());
			}
			String oldArtifactCode = TrustyUriUtils.getArtifactCode(nanopub.getUri().toString());
			RdfUtils.fixTrustyRdf(r, oldArtifactCode, h);
			np = h.getNanopub();
		} catch (RDFHandlerException ex) {
			throw new TrustyUriException(ex);
		} catch (MalformedNanopubException ex) {
			throw new TrustyUriException(ex);
		}
		return np;
	}

	public static void transformMultiNanopub(final RDFFormat format, File file, final OutputStream out)
			throws IOException, RDFParseException, RDFHandlerException, MalformedNanopubException {
		InputStream in = new FileInputStream(file);
		transformMultiNanopub(format, in, out);
	}

	public static void transformMultiNanopub(final RDFFormat format, InputStream in, final OutputStream out)
			throws IOException, RDFParseException, RDFHandlerException, MalformedNanopubException {
		try (in; out) {
			MultiNanopubRdfHandler.process(format, in, new NanopubHandler() {
	
				@Override
				public void handleNanopub(Nanopub np) {
					try {
						writeAsFixedNanopub(np, format, out);
					} catch (RDFHandlerException ex) {
						throw new RuntimeException(ex);
					} catch (TrustyUriException ex) {
						throw new RuntimeException(ex);
					}
				}
	
			});
		}
	}

	public static Nanopub writeAsFixedNanopub(Nanopub np, RDFFormat format, OutputStream out)
			throws RDFHandlerException, TrustyUriException {
		np = FixTrustyNanopub.fix(np);
		RDFWriter w = Rio.createWriter(format, new OutputStreamWriter(out, Charset.forName("UTF-8")));
		NanopubUtils.propagateToHandler(np, w);
		return np;
	}

}
