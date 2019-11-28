package org.nanopub.trusty;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.nanopub.MalformedNanopubException;
import org.nanopub.MultiNanopubRdfHandler;
import org.nanopub.MultiNanopubRdfHandler.NanopubHandler;
import org.nanopub.Nanopub;
import org.nanopub.NanopubImpl;
import org.nanopub.NanopubRdfHandler;
import org.nanopub.NanopubUtils;
import org.nanopub.NanopubWithNs;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

import net.trustyuri.TrustyUriException;
import net.trustyuri.TrustyUriResource;
import net.trustyuri.rdf.RdfFileContent;
import net.trustyuri.rdf.TransformRdf;

public class MakeTrustyNanopub {

	@com.beust.jcommander.Parameter(description = "input-nanopub-file", required = true)
	private String inputFileName;

	@com.beust.jcommander.Parameter(names = "-o", description = "Output file")
	private File outputFile;

	@com.beust.jcommander.Parameter(names = "-r", description = "Resolve cross-nanopub references")
	private boolean resolveCrossRefs = false;

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
		final Map<Resource,IRI> tempRefMap;
		if (resolveCrossRefs) {
			tempRefMap = new HashMap<>();
		} else {
			tempRefMap = null;
		}
		File inputFile = new File(inputFileName);
		if (outputFile == null) {
			outputFile = new File(inputFile.getParent(), "trusty." + inputFile.getName());
		}
		final OutputStream out;
		if (outputFile.getName().matches(".*\\.(gz|gzip)")) {
			out = new GZIPOutputStream(new FileOutputStream(outputFile));
		} else {
			out = new FileOutputStream(outputFile);
		}
		final RDFFormat inFormat = new TrustyUriResource(inputFile).getFormat(RDFFormat.TRIG);
		final RDFFormat outFormat = new TrustyUriResource(outputFile).getFormat(RDFFormat.TRIG);
		MultiNanopubRdfHandler.process(inFormat, inputFile, new NanopubHandler() {

			@Override
			public void handleNanopub(Nanopub np) {
				try {
					np = writeAsTrustyNanopub(np, outFormat, out, tempRefMap);
					if (verbose) {
						System.out.println("Nanopub URI: " + np.getUri());
					}
				} catch (RDFHandlerException | TrustyUriException ex) {
					throw new RuntimeException(ex);
				}
			}

		});
		out.close();
	}

	public static Nanopub transform(Nanopub nanopub) throws TrustyUriException {
		return transform(nanopub, null);
	}

	public static Nanopub transform(Nanopub nanopub, Map<Resource,IRI> tempRefMap) throws TrustyUriException {
		Nanopub np;
		try {
			RdfFileContent r = new RdfFileContent(RDFFormat.TRIG);
			String npUri;
			Map<Resource,IRI> tempUriReplacerMap = null;
			if (TempUriReplacer.hasTempUri(nanopub)) {
				npUri = TempUriReplacer.normUri;
				tempUriReplacerMap = new HashMap<>();
				NanopubUtils.propagateToHandler(nanopub, new TempUriReplacer(nanopub, r, tempUriReplacerMap));
			} else {
				npUri = nanopub.getUri().toString();
				NanopubUtils.propagateToHandler(nanopub, r);
			}
			if (tempRefMap != null) {
				mergeTransformMaps(tempRefMap, tempUriReplacerMap);
				RdfFileContent r2 = new RdfFileContent(RDFFormat.TRIG);
				r.propagate(new CrossRefResolver(tempRefMap, r2));
				r = r2;
			}
			NanopubRdfHandler h = new NanopubRdfHandler();
			Map<Resource,IRI> transformMap = TransformRdf.transformAndGetMap(r, h, npUri);
			np = h.getNanopub();
			mergeTransformMaps(tempRefMap, transformMap);
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
		transformMultiNanopub(format, file, out, false);
	}

	public static void transformMultiNanopub(final RDFFormat format, File file, final OutputStream out, boolean resolveCrossRefs)
			throws IOException, RDFParseException, RDFHandlerException, MalformedNanopubException {
		InputStream in = new FileInputStream(file);
		transformMultiNanopub(format, in, out, resolveCrossRefs);
	}

	public static void transformMultiNanopub(final RDFFormat format, InputStream in, final OutputStream out)
			throws IOException, RDFParseException, RDFHandlerException, MalformedNanopubException {
		transformMultiNanopub(format, in, out, false);
	}

	public static void transformMultiNanopub(final RDFFormat format, InputStream in, final OutputStream out, boolean resolveCrossRefs)
			throws IOException, RDFParseException, RDFHandlerException, MalformedNanopubException {
		final Map<Resource,IRI> tempRefMap;
		if (resolveCrossRefs) {
			tempRefMap = new HashMap<>();
		} else {
			tempRefMap = null;
		}
		MultiNanopubRdfHandler.process(format, in, new NanopubHandler() {

			@Override
			public void handleNanopub(Nanopub np) {
				try {
					// TODO temporary URI ref resolution not yet supported here
					writeAsTrustyNanopub(np, format, out, tempRefMap);
				} catch (RDFHandlerException ex) {
					throw new RuntimeException(ex);
				} catch (TrustyUriException ex) {
					throw new RuntimeException(ex);
				}
			}

		});
		out.close();
	}

	public static Nanopub writeAsTrustyNanopub(Nanopub np, RDFFormat format, OutputStream out, Map<Resource,IRI> tempRefMap)
			throws RDFHandlerException, TrustyUriException {
		np = MakeTrustyNanopub.transform(np, tempRefMap);
		RDFWriter w = Rio.createWriter(format, new OutputStreamWriter(out, Charset.forName("UTF-8")));
		NanopubUtils.propagateToHandler(np, w);
		return np;
	}

	static void mergeTransformMaps(Map<Resource,IRI> mainMap, Map<Resource,IRI> mapToMerge) {
		if (mainMap == null || mapToMerge == null) return;
		for (Resource r : new HashSet<>(mainMap.keySet())) {
			IRI v = mainMap.get(r);
			if (mapToMerge.containsKey(v)) {
				mainMap.put(r, mapToMerge.get(v));
				mapToMerge.remove(v);
			}
		}
		for (Resource r : mapToMerge.keySet()) {
			mainMap.put(r, mapToMerge.get(r));
		}
	}

}
