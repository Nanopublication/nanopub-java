package org.nanopub;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.openrdf.OpenRDFException;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

/**
 * A class to be run from the command line to validate nanopub files.
 *
 * @author Tobias Kuhn
 */
public class ValidateNanopub {

	// TODO support files with multiple nanopubs

	@com.beust.jcommander.Parameter(description = "input-nanopub-files", required = true)
	private List<File> inputFiles = new ArrayList<File>();

	public static void main(String[] args) {
		ValidateNanopub obj = new ValidateNanopub();
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
		for (File f : inputFiles) {
			try {
				new NanopubImpl(f);
				System.out.println("Valid nanopub: " + f);
			} catch (OpenRDFException ex) {
				System.out.println("INVALID RDF: " + f);
				ex.printStackTrace(System.err);
			} catch (MalformedNanopubException ex) {
				System.out.println("INVALID NANOPUB: " + f);
				ex.printStackTrace(System.err);
			}
		}
	}

}
