package org.nanopub.extra.server;

import com.beust.jcommander.ParameterException;
import net.trustyuri.TrustyUriUtils;
import net.trustyuri.rdf.RdfModule;
import org.eclipse.rdf4j.common.exception.RDF4JException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.nanopub.CliRunner;
import org.nanopub.MalformedNanopubException;
import org.nanopub.Nanopub;
import org.nanopub.extra.index.IndexUtils;
import org.nanopub.extra.index.NanopubIndex;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

public class NanopubStatus extends CliRunner {

	@com.beust.jcommander.Parameter(description = "nanopub-uri-or-artifact-code", required = true)
	private List<String> nanopubIds;

	@com.beust.jcommander.Parameter(names = "-v", description = "Verbose")
	private boolean verbose = false;

	@com.beust.jcommander.Parameter(names = "-r", description = "Recursive (check entire content of index)")
	private boolean recursive = false;

	@com.beust.jcommander.Parameter(names = "-a", description = "Check all servers (do not stop after the first successful one)")
	private boolean checkAllServers = false;

	public static void main(String[] args) {
		try {
			NanopubStatus obj = CliRunner.initJc(new NanopubStatus(), args);
			obj.run();
		} catch (ParameterException ex) {
			System.exit(1);
		} catch (Exception ex) {
			ex.printStackTrace();
			System.exit(1);
		}
	}

	private static String getArtifactCode(String uriOrArtifactCode) {
		return extractArtifactCode(uriOrArtifactCode);
	}

	static String extractArtifactCode(String uriOrArtifactCode) {
		if (uriOrArtifactCode.indexOf(":") > 0) {
			IRI uri = SimpleValueFactory.getInstance().createIRI(uriOrArtifactCode);
			if (!TrustyUriUtils.isPotentialTrustyUri(uri)) {
				throw new IllegalArgumentException("Not a well-formed trusty URI");
			}
			return TrustyUriUtils.getArtifactCode(uri.toString());
		} else {
			if (!TrustyUriUtils.isPotentialArtifactCode(uriOrArtifactCode)) {
				throw new IllegalArgumentException("Not a well-formed artifact code");
			}
			return uriOrArtifactCode;
		}
	}

	private int contentNpCount, indexNpCount;
	private int minCount = -1;

	public NanopubStatus() {
	}

	private void run() throws IOException, RDFHandlerException {
		checkNanopub(nanopubIds.get(0), recursive);
		if (recursive) {
			System.out.print(indexNpCount + " index nanopub" + (indexNpCount!=1?"s":"") + "; ");
			System.out.println(contentNpCount + " content nanopub" + (contentNpCount!=1?"s":""));
			if (checkAllServers) {
				System.out.println("Each found on at least " + minCount + " nanopub server" + (minCount!=1?"s":"") + ".");
			}
		}
	}

	private void checkNanopub(String nanopubId, boolean checkIndexContent) {
		String ac = getArtifactCode(nanopubId);
		if (!ac.startsWith(RdfModule.MODULE_ID)) {
			System.err.println("ERROR. Not a trusty URI of type RA: " + nanopubId);
			System.exit(1);
		}
		int count = 0;
		ServerIterator serverIterator = new ServerIterator();
		Nanopub nanopub = null;
		while (serverIterator.hasNext()) {
			ServerInfo serverInfo = serverIterator.next();
			String serverUrl = serverInfo.getPublicUrl();
			try {
				Nanopub np = GetNanopub.get(ac, serverUrl);
				if (np != null) {
					if (checkIndexContent && !IndexUtils.isIndex(np)) {
						System.err.println("ERROR. Not an index: " + nanopubId);
						System.exit(1);
					}
					if (nanopub == null) nanopub = np;
					if (!recursive || verbose) {
						System.out.println("URL: " + serverUrl + ac);
					}
					if (checkAllServers) {
						count++;
					} else {
						break;
					}
				}
			} catch (FileNotFoundException ex) {
				if (verbose && !recursive) {
					System.out.println("NOT FOUND ON: " + serverUrl);
				}
			} catch (IOException ex) {
				if (verbose && !recursive) {
					System.out.println("CONNECTION ERROR: " + serverUrl);
				}
			} catch (RDF4JException ex) {
				if (verbose && !recursive) {
					System.out.println("VALIDATION ERROR: " + serverUrl);
				}
			} catch (MalformedNanopubException ex) {
				if (verbose && !recursive) {
					System.out.println("VALIDATION ERROR: " + serverUrl);
				}
			}
		}
		if (checkAllServers) {
			String text = "Found on " + count + " nanopub server" + (count!=1?"s":"");
			if (!recursive) {
				System.out.println(text + ".");
			} else if (verbose) {
				System.out.println(text + ": " + ac);
			}
			if (minCount < 0 || minCount > count) {
				minCount = count;
			}
		}
		if (nanopub != null) {
			if (checkIndexContent) {
				indexNpCount++;
				NanopubIndex npi = null;
				try {
					npi = IndexUtils.castToIndex(nanopub);
				} catch (MalformedNanopubException ex) {
					ex.printStackTrace();
					System.exit(1);
				}
				for (IRI elementUri : npi.getElements()) {
					checkNanopub(elementUri.toString(), false);
				}
				for (IRI subIndexUri : npi.getSubIndexes()) {
					checkNanopub(subIndexUri.toString(), true);
				}
				if (npi.getAppendedIndex() != null) {
					checkNanopub(npi.getAppendedIndex().toString(), true);
				}
			} else {
				contentNpCount++;
			}
		}
		if ((indexNpCount+contentNpCount) % 100 == 0) {
			System.err.print((indexNpCount+contentNpCount) + " nanopubs...\r");
		}
	}

}
