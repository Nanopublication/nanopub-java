package org.nanopub.trusty;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.trustyuri.TrustyUriUtils;
import net.trustyuri.rdf.RdfHasher;
import net.trustyuri.rdf.RdfPreprocessor;

import org.nanopub.MalformedNanopubException;
import org.nanopub.MultiNanopubRdfHandler;
import org.nanopub.MultiNanopubRdfHandler.NanopubHandler;
import org.nanopub.Nanopub;
import org.nanopub.NanopubUtils;
import org.openrdf.OpenRDFException;
import org.openrdf.model.Statement;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

public class CheckTrustyNanopub {

	@com.beust.jcommander.Parameter(description = "input-nanopub-files", required = true)
	private List<File> inputFiles = new ArrayList<File>();

	@com.beust.jcommander.Parameter(names = "-v", description = "verbose")
	private boolean verbose = false;

	public static void main(String[] args) {
		CheckTrustyNanopub obj = new CheckTrustyNanopub();
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

	private List<Nanopub> nanopubs = new ArrayList<Nanopub>();
	private int valid, invalid, error;

	private void run() throws IOException {
		valid = 0;
		invalid = 0;
		error = 0;
		for (File f : inputFiles) {
			nanopubs.clear();
			try {
				System.out.println("Reading file: " + f);
				MultiNanopubRdfHandler.process(f, new NanopubHandler() {
					@Override
					public void handleNanopub(Nanopub np) {
						nanopubs.add(np);
					}
				});
				for (Nanopub np : nanopubs) {
					if (isValid(np)) {
						if (verbose) {
							System.out.println("Valid trusty nanopub: " + np.getUri());
						}
						valid++;
					} else {
						System.out.println("NOT A VALID TRUSTY NANOPUB: " + np.getUri());
						invalid++;
					}
				}
				if (nanopubs.isEmpty()) {
					System.out.println("NO NANOPUB FOUND: " + f);
					error++;
				}
			} catch (OpenRDFException ex) {
				System.out.println("INVALID RDF: " + f);
				ex.printStackTrace(System.err);
				error++;
			} catch (MalformedNanopubException ex) {
				System.out.println("INVALID NANOPUB: " + f);
				ex.printStackTrace(System.err);
				error++;
			}
		}
		System.out.println("Summary: " + valid + " valid | " + invalid + " invalid | " + error + " errors");
	}
	
	public static boolean isValid(Nanopub nanopub) {
		String artifactCode = TrustyUriUtils.getArtifactCode(nanopub.getUri().toString());
		if (artifactCode == null) return false;
		List<Statement> statements = NanopubUtils.getStatements(nanopub);
		statements = RdfPreprocessor.run(statements, artifactCode);
		String ac = RdfHasher.makeArtifactCode(statements);
		return ac.equals(artifactCode);
	}

}
