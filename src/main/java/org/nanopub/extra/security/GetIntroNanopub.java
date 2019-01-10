package org.nanopub.extra.security;

import java.io.IOException;
import java.util.List;

import org.nanopub.NanopubImpl;
import org.nanopub.NanopubUtils;
import org.openrdf.OpenRDFException;
import org.openrdf.rio.RDFFormat;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

public class GetIntroNanopub {

	@com.beust.jcommander.Parameter(description = "user-id", required = true)
	private List<String> userIds;

	public static void main(String[] args) {
		NanopubImpl.ensureLoaded();
		GetIntroNanopub obj = new GetIntroNanopub();
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

	private void run() throws IOException, OpenRDFException {
		for (String userId : userIds) {
			NanopubUtils.writeToStream(IntroNanopub.get(userId).getNanopub(), System.out, RDFFormat.TRIG);
		}
	}

}
