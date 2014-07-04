package org.nanopub;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.trustyuri.TrustyUriUtils;

import org.nanopub.MultiNanopubRdfHandler.NanopubHandler;
import org.nanopub.trusty.TrustyNanopubUtils;
import org.openrdf.OpenRDFException;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sparql.SPARQLRepository;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

public class CheckNanopub {

	@com.beust.jcommander.Parameter(description = "input-nanopubs", required = true)
	private List<String> inputNanopubs = new ArrayList<String>();

	@com.beust.jcommander.Parameter(names = "-v", description = "Verbose")
	private boolean verbose = false;

	@com.beust.jcommander.Parameter(names = "-s", description = "Load nanopubs from given SPARQL endpoint")
	private String sparqlEndpointUrl;

	public static void main(String[] args) {
		CheckNanopub obj = new CheckNanopub();
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

	private int valid, notTrusty, invalid, error;
	private int count;
	private SPARQLRepository sparqlRepo;

	private void run() throws IOException {
		valid = 0;
		notTrusty = 0;
		invalid = 0;
		error = 0;
		for (String s : inputNanopubs) {
			count = 0;
			try {
				if (sparqlEndpointUrl != null) {
					if (sparqlRepo == null) {
						sparqlRepo = new SPARQLRepository(sparqlEndpointUrl);
						sparqlRepo.initialize();
					}
					Nanopub np = new NanopubImpl(sparqlRepo, new URIImpl(s));
					check(np);
				} else {
					System.out.println("Reading file: " + s);
					MultiNanopubRdfHandler.process(new File(s), new NanopubHandler() {
						@Override
						public void handleNanopub(Nanopub np) {
							count++;
							check(np);
						}
					});
					if (count == 0) {
						System.out.println("NO NANOPUB FOUND: " + s);
						error++;
					}
				}
			} catch (OpenRDFException ex) {
				System.out.println("RDF ERROR: " + s);
				ex.printStackTrace(System.err);
				error++;
			} catch (MalformedNanopubException ex) {
				System.out.println("INVALID NANOPUB: " + s);
				ex.printStackTrace(System.err);
				invalid++;
			}
		}
		System.out.println("Summary: " + valid + " valid and trusty | " + notTrusty + " valid but not trusty | " +
				invalid + " invalid | " + error + " errors");
		if (sparqlRepo != null) {
			try {
				sparqlRepo.shutDown();
			} catch (RepositoryException ex) {
				ex.printStackTrace();
			}
		}
	}

	private void check(Nanopub np) {
		if (TrustyNanopubUtils.isValidTrustyNanopub(np)) {
			if (verbose) {
				System.out.println("Nanopub is valid and trusty: " + np.getUri());
			}
			valid++;
		} else if (TrustyUriUtils.isPotentialTrustyUri(np.getUri())) {
			System.out.println("Looks like a trusty nanopub BUT VERIFICATION FAILED: " + np.getUri());
			notTrusty++;
		} else {
			if (verbose) {
				System.out.println("Nanopub is valid BUT NOT TRUSTY: " + np.getUri());
			}
			notTrusty++;
		}
	}

}
