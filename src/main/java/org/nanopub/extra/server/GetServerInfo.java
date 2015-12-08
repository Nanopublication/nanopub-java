package org.nanopub.extra.server;

import java.io.IOException;
import java.util.List;

import org.nanopub.NanopubImpl;
import org.nanopub.extra.server.ServerInfo.ServerInfoException;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

public class GetServerInfo {

	@com.beust.jcommander.Parameter(description = "server-urls", required = true)
	private List<String> serverUrls;

	public static void main(String[] args) {
		NanopubImpl.ensureLoaded();
		GetServerInfo obj = new GetServerInfo();
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

	private void run() throws ServerInfoException, IOException {
		for (String url : serverUrls) {
			ServerInfo si = ServerInfo.load(url);
			System.out.println("Server URL:          " + si.getPublicUrl());
			System.out.println("Protocol version:    " + si.getProtocolVersion());
			System.out.println("Description:         " + si.getDescription());
			String ad = si.getAdmin();
			System.out.println("Admin:               " + (ad == null || ad.isEmpty() ? "(unknown)" : ad));
			System.out.println("Journal ID:          " + si.getJournalId());
			System.out.println("Page size:           " + si.getPageSize());
			System.out.println("Post peers:          " + (si.isPostPeersEnabled() ? "enabled" : "disabled"));
			System.out.println("Post nanopubs:       " + (si.isPostNanopubsEnabled() ? "enabled" : "disabled"));
			System.out.println("Nanopub count:       " + (si.getNextNanopubNo()-1));
			System.out.println("Max nanopubs:        " + (si.getMaxNanopubs() == null ? "unrestricted" : si.getMaxNanopubs()));
			System.out.println("Max triples/nanopub: " + (si.getMaxNanopubTriples() == null ? "unrestricted" : si.getMaxNanopubTriples()));
			System.out.println("Max bytes/nanopub:   " + (si.getMaxNanopubBytes() == null ? "unrestricted" : si.getMaxNanopubBytes()));
			System.out.println("URI pattern:         " + (si.getUriPattern() == null ? "(everything)" : si.getUriPattern()));
			System.out.println("Hash pattern:        " + (si.getHashPattern() == null ? "(everything)" : si.getHashPattern()));
			System.out.println();
		}
	}

}
