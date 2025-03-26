package org.nanopub.extra.setting;

import com.beust.jcommander.ParameterException;
import org.eclipse.rdf4j.common.exception.RDF4JException;
import org.eclipse.rdf4j.model.IRI;
import org.nanopub.CliRunner;
import org.nanopub.MalformedNanopubException;
import org.nanopub.Run;

import java.io.IOException;

public class ShowSetting extends CliRunner {

	public static void main(String[] args) {
		ShowSetting obj = Run.initJc(new ShowSetting(), args);
		try {
			obj.run();
		} catch (ParameterException ex) {
			System.exit(1);
		} catch (Exception ex) {
			ex.printStackTrace();
			System.exit(1);
		}
	}

	private void run() throws RDF4JException, MalformedNanopubException, IOException {
		NanopubSetting ns =  NanopubSetting.getLocalSetting();
		System.out.println("setting nanopub:       " + ns.getNanopub().getUri());
		System.out.println("name:                  " + ns.getName());
		for (IRI i : ns.getBootstrapServices()) {
			System.out.println("bootstrap service:     " + i);
		}
		System.out.println("agent index:           " + ns.getAgentIntroCollection());
		System.out.println("service index:         " + ns.getServiceIntroCollection());
		System.out.println("trust range algorithm: " + ns.getTrustRangeAlgorithm());
		System.out.println("update strategy:       " + ns.getUpdateStrategy());
	}

}
