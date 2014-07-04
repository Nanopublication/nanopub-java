package org.nanopub.extra.server;

import java.io.IOException;
import java.util.List;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

public class GetServerInfo {

	@com.beust.jcommander.Parameter(description = "server-urls", required = true)
	private List<String> serverUrls;

	public static void main(String[] args) {
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

	private void run() throws IOException {
		for (String url : serverUrls) {
			ServerInfo si = ServerInfo.load(url);
			System.out.println("Server URL:    " + si.getPublicUrl());
			String ad = si.getAdmin();
			System.out.println("Admin:         " + (ad == null || ad.isEmpty() ? "(unknown)" : ad));
			System.out.println("Journal ID:    " + si.getJournalId());
			System.out.println("Journal size:  " + si.getNextNanopubNo());
			System.out.println("Page size:     " + si.getPageSize());
			System.out.println("Post peers:    " + (si.isPostPeersEnabled() ? "enabled" : "disabled"));
			System.out.println("Post nanopubs: " + (si.isPostNanopubsEnabled() ? "enabled" : "disabled"));
			System.out.println();
		}
	}

}
