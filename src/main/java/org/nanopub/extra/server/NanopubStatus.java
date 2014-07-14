package org.nanopub.extra.server;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import net.trustyuri.TrustyUriUtils;
import net.trustyuri.rdf.RdfModule;

import org.nanopub.MalformedNanopubException;
import org.nanopub.Nanopub;
import org.openrdf.OpenRDFException;
import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.rio.RDFHandlerException;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

public class NanopubStatus {

	@com.beust.jcommander.Parameter(description = "nanopub-uri-or-artifact-code", required = true)
	private List<String> nanopubIds;

	@com.beust.jcommander.Parameter(names = "-v", description = "Verbose")
	private boolean verbose = false;

	public static void main(String[] args) {
		NanopubStatus obj = new NanopubStatus();
		JCommander jc = new JCommander(obj);
		try {
			jc.parse(args);
		} catch (ParameterException ex) {
			jc.usage();
			System.exit(1);
		}
		if (obj.nanopubIds.size() != 1) {
			System.err.println("ERROR: Exactly one main argument needed");
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

	private static String getArtifactCode(String uriOrArtifactCode) {
		if (uriOrArtifactCode.indexOf(":") > 0) {
			URI uri = new URIImpl(uriOrArtifactCode);
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

	private ServerIterator serverIterator = new ServerIterator();

	private int count;

	public NanopubStatus() {
	}

	private void run() throws IOException, RDFHandlerException {
		count = 0;
		String nanopubId = nanopubIds.get(0);
		String ac = getArtifactCode(nanopubId);
		if (!ac.startsWith(RdfModule.MODULE_ID)) {
			throw new IllegalArgumentException("Not a trusty URI of type RA");
		}
		while (serverIterator.hasNext()) {
			String serverUrl = serverIterator.next();
			try {
				Nanopub np = GetNanopub.get(ac, serverUrl);
				if (np != null) {
					System.out.println(serverUrl + ac);
					count++;
				}
			} catch (FileNotFoundException ex) {
				if (verbose) {
					System.out.println("NOT FOUND ON: " + serverUrl);
				}
			} catch (IOException ex) {
				if (verbose) {
					System.out.println("CONNECTION ERROR: " + serverUrl);
				}
			} catch (OpenRDFException ex) {
				if (verbose) {
					System.out.println("VALIDATION ERROR: " + serverUrl);
				}
			} catch (MalformedNanopubException ex) {
				if (verbose) {
					System.out.println("VALIDATION ERROR: " + serverUrl);
				}
			}
		}
		System.out.println("Found on " + count + " nanopub server" + (count!=1?"s":"") + ".");
	}

}
