package org.nanopub.extra.server;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.trustyuri.TrustyUriUtils;
import net.trustyuri.rdf.RdfModule;

import org.nanopub.MalformedNanopubException;
import org.nanopub.Nanopub;
import org.nanopub.NanopubImpl;
import org.nanopub.NanopubUtils;
import org.nanopub.trusty.TrustyNanopubUtils;
import org.openrdf.OpenRDFException;
import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

public class GetNanopub {

	@com.beust.jcommander.Parameter(description = "nanopub-uris-or-artifact-codes", required = true)
	private List<String> nanopubIds;

	@com.beust.jcommander.Parameter(names = "-f", description = "Format of the nanopub: trig, nq, trix, ...")
	private String format = "trig";

	@com.beust.jcommander.Parameter(names = "-d", description = "Save as file(s) in the given directory")
	private File outputDir;

	public static void main(String[] args) {
		GetNanopub obj = new GetNanopub();
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

	private static final List<String> serverBootstrapList = new ArrayList<>();

	static {
		// Hard-coded server instances:
		serverBootstrapList.add("http://example.org/");
		serverBootstrapList.add("http://np.inn.ac/");
		// more to come...
	}

	public static Nanopub get(String uriOrArtifactCode) {
		return new GetNanopub().getNanopub(uriOrArtifactCode);
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

	private List<String> serversToContact = new ArrayList<>();
	private List<String> serversToGetPeers = new ArrayList<>();
	private Map<String,Boolean> serversContacted = new HashMap<>();
	private Map<String,Boolean> serversPeersGot = new HashMap<>();

	public GetNanopub() {
		serversToContact.addAll(serverBootstrapList);
		serversToGetPeers.addAll(serverBootstrapList);
	}

	private void run() throws IOException, RDFHandlerException {
		for (String nanopubId : nanopubIds) {
			Nanopub np = getNanopub(nanopubId);
			if (np == null) {
				System.err.println("NOT FOUND: " + nanopubId);
			} else if (outputDir == null) {
				NanopubUtils.writeToStream(np, System.out, RDFFormat.forFileName("file." + format));
				System.out.print("\n\n");
			} else {
				OutputStream out = new FileOutputStream(new File(outputDir, getArtifactCode(nanopubId) + "." + format));
				NanopubUtils.writeToStream(np, out, RDFFormat.forFileName("file." + format));
				out.close();
			}
		}
	}

	public Nanopub getNanopub(String uriOrArtifactCode) {
		String ac = getArtifactCode(uriOrArtifactCode);
		if (!ac.startsWith(RdfModule.MODULE_ID)) {
			throw new IllegalArgumentException("Not a trusty URI of type RA");
		}
		String npsUrl;
		while ((npsUrl = getNextServerUrl()) != null) {
			try {
				URL url = new URL(npsUrl + ac);
				Nanopub nanopub = new NanopubImpl(url);
				if (TrustyNanopubUtils.isValidTrustyNanopub(nanopub)) {
					return nanopub;
				}
			} catch (IOException ex) {
				// ignore
			} catch (OpenRDFException ex) {
				// ignore
			} catch (MalformedNanopubException ex) {
				// ignore
			}
		}
		return null;
	}

	private String getNextServerUrl() {
		while (!serversToContact.isEmpty() || !serversToGetPeers.isEmpty()) {
			if (!serversToContact.isEmpty()) {
				String url = serversToContact.remove(0);
				if (serversContacted.containsKey(url)) continue;
				serversContacted.put(url, true);
				return url;
			}
			if (!serversToGetPeers.isEmpty()) {
				String url = serversToGetPeers.remove(0);
				if (serversPeersGot.containsKey(url)) continue;
				serversPeersGot.put(url, true);
				try {
					for (String peerUrl : NanopubServerUtils.loadPeerList(url)) {
						if (!serversContacted.containsKey(peerUrl)) {
							serversToContact.add(peerUrl);
						}
						if (!serversPeersGot.containsKey(peerUrl)) {
							serversToGetPeers.add(peerUrl);
						}
					}
				} catch (IOException ex) {
					// ignore
				}
			}
		}
		return null;
	}

}
