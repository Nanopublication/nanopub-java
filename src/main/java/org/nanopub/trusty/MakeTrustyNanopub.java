package org.nanopub.trusty;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import net.trustyuri.TrustyUriException;
import net.trustyuri.TrustyUriResource;
import net.trustyuri.rdf.RdfFileContent;
import net.trustyuri.rdf.TransformRdf;

import org.nanopub.MalformedNanopubException;
import org.nanopub.MultiNanopubRdfHandler;
import org.nanopub.MultiNanopubRdfHandler.NanopubHandler;
import org.nanopub.Nanopub;
import org.nanopub.NanopubImpl;
import org.nanopub.NanopubRdfHandler;
import org.nanopub.NanopubUtils;
import org.nanopub.NanopubWithNs;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.RDFWriter;
import org.openrdf.rio.Rio;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

public class MakeTrustyNanopub {

	@com.beust.jcommander.Parameter(description = "input-nanopubs", required = true)
	private List<File> inputNanopubs = new ArrayList<File>();

	@com.beust.jcommander.Parameter(names = "-r", description = "Resolve temporary references (http://purl.org/nanopub/temp/...)")
	private boolean resolveTempRefs = false;

	@com.beust.jcommander.Parameter(names = "-v", description = "Verbose")
	private boolean verbose = false;

	public static void main(String[] args) {
		NanopubImpl.ensureLoaded();
		MakeTrustyNanopub obj = new MakeTrustyNanopub();
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

	private void run() throws IOException, RDFParseException, RDFHandlerException,
			MalformedNanopubException, TrustyUriException {
		final Map<String,String> tempRefMap;
		if (resolveTempRefs) {
			tempRefMap = new HashMap<>();
		} else {
			tempRefMap = null;
		}
		for (File inputFile : inputNanopubs) {
			File outFile = new File(inputFile.getParent(), "trusty." + inputFile.getName());
			final OutputStream out;
			if (inputFile.getName().matches(".*\\.(gz|gzip)")) {
				out = new GZIPOutputStream(new FileOutputStream(outFile));
			} else {
				out = new FileOutputStream(outFile);
			}
			final RDFFormat format = new TrustyUriResource(inputFile).getFormat(RDFFormat.TRIG);
			MultiNanopubRdfHandler.process(format, inputFile, new NanopubHandler() {

				@Override
				public void handleNanopub(Nanopub np) {
					try {
						np = writeAsTrustyNanopub(np, format, out, tempRefMap);
						if (verbose) {
							System.out.println("Nanopub URI: " + np.getUri());
						}
					} catch (RDFHandlerException ex) {
						throw new RuntimeException(ex);
					} catch (TrustyUriException ex) {
						throw new RuntimeException(ex);
					}
				}

			});
			out.close();
		}
	}

	public static Nanopub transform(Nanopub nanopub) throws TrustyUriException {
		return transform(nanopub, null);
	}

	public static Nanopub transform(Nanopub nanopub, Map<String,String> tempRefMap) throws TrustyUriException {
		Nanopub np;
		try {
			RdfFileContent r = new RdfFileContent(RDFFormat.TRIG);
			String npUri;
			if (TempUriReplacer.hasTempUri(nanopub)) {
				npUri = TempUriReplacer.normUri;
				NanopubUtils.propagateToHandler(nanopub, new TempUriReplacer(nanopub, r));
			} else {
				npUri = nanopub.getUri().toString();
				NanopubUtils.propagateToHandler(nanopub, r);
			}
			if (tempRefMap != null) {
				RdfFileContent r2 = new RdfFileContent(RDFFormat.TRIG);
				r.propagate(new TempRefReplacer(tempRefMap, r2));
				r = r2;
			}
			NanopubRdfHandler h = new NanopubRdfHandler();
			TransformRdf.transform(r, h, npUri);
			np = h.getNanopub();
			if (TempUriReplacer.hasTempUri(nanopub) && tempRefMap != null) {
				String key = nanopub.getUri().stringValue();
				if (tempRefMap.containsKey(key)) {
					throw new RuntimeException("Temp URI found twice.");
				}
				tempRefMap.put(key, np.getUri().stringValue());
			}
		} catch (RDFHandlerException ex) {
			throw new TrustyUriException(ex);
		} catch (MalformedNanopubException ex) {
			throw new TrustyUriException(ex);
		}
		if (np instanceof NanopubWithNs) {
			((NanopubWithNs) np).removeUnusedPrefixes();
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
		MultiNanopubRdfHandler.process(format, in, new NanopubHandler() {

			@Override
			public void handleNanopub(Nanopub np) {
				try {
					// TODO temporary URI ref resolution not yet supported here
					writeAsTrustyNanopub(np, format, out, null);
				} catch (RDFHandlerException ex) {
					throw new RuntimeException(ex);
				} catch (TrustyUriException ex) {
					throw new RuntimeException(ex);
				}
			}

		});
		out.close();
	}

	public static Nanopub writeAsTrustyNanopub(Nanopub np, RDFFormat format, OutputStream out, Map<String,String> tempRefMap)
			throws RDFHandlerException, TrustyUriException {
		np = MakeTrustyNanopub.transform(np, tempRefMap);
		RDFWriter w = Rio.createWriter(format, new OutputStreamWriter(out, Charset.forName("UTF-8")));
		NanopubUtils.propagateToHandler(np, w);
		return np;
	}

}
