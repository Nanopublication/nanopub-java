package org.nanopub.trusty;

import com.beust.jcommander.ParameterException;
import net.trustyuri.TrustyUriException;
import net.trustyuri.TrustyUriResource;
import net.trustyuri.TrustyUriUtils;
import net.trustyuri.rdf.RdfFileContent;
import net.trustyuri.rdf.TransformRdf;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.rio.*;
import org.nanopub.*;
import org.nanopub.MultiNanopubRdfHandler.NanopubHandler;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;
import java.util.zip.GZIPOutputStream;

public class MakeTrustyNanopub extends CliRunner {

	@com.beust.jcommander.Parameter(description = "input-nanopub-files", required = true)
	private List<File> inputNanopubsFiles = new ArrayList<File>();

	@com.beust.jcommander.Parameter(names = "-o", description = "Output file")
	private File singleOutputFile;

	@com.beust.jcommander.Parameter(names = "-r", description = "Resolve cross-nanopub references")
	private boolean resolveCrossRefs = false;

	@com.beust.jcommander.Parameter(names = "-R", description = "Resolve cross-nanopub references based on prefixes")
	private boolean resolveCrossRefsPrefixBased = false;

	@com.beust.jcommander.Parameter(names = "-v", description = "Verbose")
	private boolean verbose = false;

	public static void main(String[] args) {
		try {
			MakeTrustyNanopub obj = Run.initJc(new MakeTrustyNanopub(), args);
			obj.run();
		} catch (ParameterException ex) {
			System.exit(1);
		} catch (Exception ex) {
			ex.printStackTrace();
			System.exit(1);
		}
	}

	private void run() throws IOException, RDFParseException, RDFHandlerException,
			MalformedNanopubException, TrustyUriException {
		final Map<Resource,IRI> tempRefMap;
		final Map<String,String> tempPrefixMap;
		if (resolveCrossRefsPrefixBased) {
			tempPrefixMap = new HashMap<>();
			tempRefMap = new HashMap<>();
		} else if (resolveCrossRefs) {
			tempPrefixMap = null;
			tempRefMap = new HashMap<>();
		} else {
			tempPrefixMap = null;
			tempRefMap = null;
		}
		final OutputStream singleOut;
		if (singleOutputFile != null) {
			if (singleOutputFile.getName().matches(".*\\.(gz|gzip)")) {
				singleOut = new GZIPOutputStream(new FileOutputStream(singleOutputFile));
			} else {
				singleOut = new FileOutputStream(singleOutputFile);
			}
		} else {
			singleOut = null;
		}
		for (File inputFile : inputNanopubsFiles) {
			File outputFile;
			final OutputStream out;
			if (singleOutputFile == null) {
				outputFile = new File(inputFile.getParent(), "trusty." + inputFile.getName());
				if (inputFile.getName().matches(".*\\.(gz|gzip)")) {
					out = new GZIPOutputStream(new FileOutputStream(outputFile));
				} else {
					out = new FileOutputStream(outputFile);
				}
			} else {
				outputFile = singleOutputFile;
				out = singleOut;
			}
			final RDFFormat inFormat = new TrustyUriResource(inputFile).getFormat(RDFFormat.TRIG);
			final RDFFormat outFormat = new TrustyUriResource(outputFile).getFormat(RDFFormat.TRIG);
			MultiNanopubRdfHandler.process(inFormat, inputFile, new NanopubHandler() {
	
				@Override
				public void handleNanopub(Nanopub np) {
					try {
						np = writeAsTrustyNanopub(np, outFormat, out, tempRefMap, tempPrefixMap);
						if (verbose) {
							System.out.println("Nanopub URI: " + np.getUri());
						}
					} catch (RDFHandlerException | TrustyUriException ex) {
						throw new RuntimeException(ex);
					}
				}
	
			});
			if (singleOutputFile == null) {
				out.close();
			}
		}
		if (singleOutputFile != null) {
			singleOut.close();
		}
	}

	public static Nanopub transform(Nanopub nanopub) throws TrustyUriException {
		return transform(nanopub, null, null);
	}

	public static Nanopub transform(Nanopub nanopub, Map<Resource,IRI> tempRefMap, Map<String,String> tempPrefixMap) throws TrustyUriException {
		String u = nanopub.getUri().stringValue();
		if (!nanopub.getHeadUri().stringValue().startsWith(u) ||
				!nanopub.getAssertionUri().stringValue().startsWith(u) ||
				!nanopub.getProvenanceUri().stringValue().startsWith(u) ||
				!nanopub.getPubinfoUri().stringValue().startsWith(u)) {
			throw new TrustyUriException("Graph URIs need have the nanopub URI as prefix: " + u + "...");
		}
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
			if (tempRefMap != null || tempPrefixMap != null) {
				if (tempRefMap == null) {
					tempRefMap = new HashMap<>();
				}
				mergeTransformMaps(tempRefMap, tempUriReplacerMap);
				RdfFileContent r2 = new RdfFileContent(RDFFormat.TRIG);
				r.propagate(new CrossRefResolver(tempRefMap, tempPrefixMap, r2));
				r = r2;
			}
			NanopubRdfHandler h = new NanopubRdfHandler();
			Map<Resource,IRI> transformMap = TransformRdf.transformAndGetMap(r, h, npUri);
			np = h.getNanopub();
			mergeTransformMaps(tempRefMap, transformMap);
			mergePrefixTransformMaps(tempPrefixMap, transformMap);
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
		try (in; out) {
			MultiNanopubRdfHandler.process(format, in, new NanopubHandler() {
	
				@Override
				public void handleNanopub(Nanopub np) {
					try {
						// TODO temporary URI ref resolution not yet supported here
						// TODO prefix-based cross-ref resolution also not yet supported
						writeAsTrustyNanopub(np, format, out, tempRefMap, null);
					} catch (RDFHandlerException ex) {
						throw new RuntimeException(ex);
					} catch (TrustyUriException ex) {
						throw new RuntimeException(ex);
					}
				}
	
			});
		}
	}

	public static Nanopub writeAsTrustyNanopub(Nanopub np, RDFFormat format, OutputStream out, Map<Resource,IRI> tempRefMap, Map<String,String> tempPrefixMap)
			throws RDFHandlerException, TrustyUriException {
		np = MakeTrustyNanopub.transform(np, tempRefMap, tempPrefixMap);
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

	static void mergePrefixTransformMaps(Map<String,String> mainPrefixMap, Map<Resource,IRI> mapToMerge) {
		if (mainPrefixMap == null || mapToMerge == null) return;
		for (Resource r : mapToMerge.keySet()) {
			if (r instanceof IRI && TrustyUriUtils.isPotentialTrustyUri(mapToMerge.get(r).stringValue())) {
				mainPrefixMap.put(r.stringValue(), mapToMerge.get(r).stringValue());
			}
		}
	}

}
