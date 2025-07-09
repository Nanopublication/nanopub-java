package org.nanopub.extra.server;

import com.beust.jcommander.ParameterException;
import org.nanopub.CliRunner;
import org.nanopub.extra.server.RegistryInfo.RegistryInfoException;

import java.io.IOException;
import java.util.List;

public class GetServerInfo extends CliRunner {

	@com.beust.jcommander.Parameter(description = "server-urls", required = true)
	private List<String> serverUrls;

	public static void main(String[] args) {
		try {
			GetServerInfo obj = CliRunner.initJc(new GetServerInfo(), args);
			obj.run();
		} catch (ParameterException ex) {
			System.exit(1);
		} catch (Exception ex) {
			ex.printStackTrace();
			System.exit(1);
		}
	}

	private void run() throws RegistryInfoException, IOException {
		for (String url : serverUrls) {
			RegistryInfo si = RegistryInfo.load(url);
			System.out.println("URL:                  " + si.getUrl());
			System.out.println("setupId:              " + si.getSetupId());
			System.out.println("trustStateCounter:    " + si.getTrustStateCounter());
			System.out.println("lastTrustStateUpdate: " + si.getLastTrustStateUpdate());
			System.out.println("trustStateHash:       " + si.getTrustStateHash());
			System.out.println("status:               " + si.getStatus());
			System.out.println("coverageTypes:        " + si.getCoverageTypes());
			System.out.println("coverageAgents:       " + si.getCoverageAgents());
			System.out.println("currentSetting:       " + si.getCurrentSetting());
			System.out.println("originalSetting:      " + si.getOriginalSetting());
			System.out.println("agentCount:           " + si.getAgentCount());
			System.out.println("accountCount:         " + si.getAccountCount());
			System.out.println("nanopubCount:         " + si.getNanopubCount());
			System.out.println("loadCounter:          " + si.getLoadCounter());
			System.out.println();
		}
	}

}
