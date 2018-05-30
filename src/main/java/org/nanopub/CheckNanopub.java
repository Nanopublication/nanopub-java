package org.nanopub;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;

import org.nanopub.MultiNanopubRdfHandler.NanopubHandler;
import org.nanopub.extra.security.MalformedSignatureException;
import org.nanopub.extra.security.NanopubSignatureElement;
import org.nanopub.extra.security.SignatureUtils;
import org.nanopub.trusty.TrustyNanopubUtils;
import org.openrdf.OpenRDFException;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sparql.SPARQLRepository;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

import net.trustyuri.TrustyUriUtils;

public class CheckNanopub {

	@com.beust.jcommander.Parameter(description = "input-nanopubs", required = true)
	private List<String> inputNanopubs = new ArrayList<String>();

	@com.beust.jcommander.Parameter(names = "-v", description = "Verbose")
	private boolean verbose = false;

	@com.beust.jcommander.Parameter(names = "-s", description = "Load nanopubs from given SPARQL endpoint")
	private String sparqlEndpointUrl;

	public static void main(String[] args) {
		NanopubImpl.ensureLoaded();
		CheckNanopub obj = new CheckNanopub();
		JCommander jc = new JCommander(obj);
		try {
			jc.parse(args);
		} catch (ParameterException ex) {
			jc.usage();
			System.exit(1);
		}
		try {
			obj.setLogPrintStream(System.out);
			Report report = obj.check();
			System.out.println("Summary: " + report.getSummary());
		} catch (Exception ex) {
			ex.printStackTrace();
			System.exit(1);
		}
	}

	private Report report;
	private int count;
	private SPARQLRepository sparqlRepo;
	private PrintStream logOut;

	public CheckNanopub(List<String> inputNanopubFiles) {
		this.inputNanopubs = inputNanopubFiles;
	}

	public CheckNanopub(String...  inputNanopubFiles) {
		for (String i : inputNanopubFiles) {
			this.inputNanopubs.add(i);
		}
	}

	public CheckNanopub(String sparqlEndpointUrl, List<String> inputNanopubIds) {
		this.inputNanopubs = inputNanopubIds;
		this.sparqlEndpointUrl = sparqlEndpointUrl;
	}

	public Report check() throws IOException {
		report = new Report();
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
					if (verbose) {
						log("Reading file: " + s + "\n");
					}
					MultiNanopubRdfHandler.process(new File(s), new NanopubHandler() {
						@Override
						public void handleNanopub(Nanopub np) {
							count++;
							if (count % 100 == 0) {
								log(count + " nanopubs...\r");
							}
							check(np);
						}
					});
					if (count == 0) {
						log("NO NANOPUB FOUND: " + s + "\n");
						report.countError();
					}
				}
			} catch (OpenRDFException ex) {
				log("RDF ERROR: " + s + "\n");
				if (logOut != null) ex.printStackTrace(logOut);
				report.countError();
			} catch (MalformedNanopubException ex) {
				log("INVALID NANOPUB: " + s + "\n");
				if (logOut != null) ex.printStackTrace(logOut);
				report.countInvalid();
			}
		}
		if (sparqlRepo != null) {
			try {
				sparqlRepo.shutDown();
			} catch (RepositoryException ex) {
				ex.printStackTrace();
			}
		}
		return report;
	}

	private void check(Nanopub np) {
		if (TrustyNanopubUtils.isValidTrustyNanopub(np)) {
			NanopubSignatureElement se;
			try {
				se = SignatureUtils.getLegacySignatureElement(np);
			} catch (MalformedSignatureException ex) {
				System.out.println("SIGNATURE IS NOT WELL-FORMED (" + ex.getMessage() + "): " + np.getUri());
				report.countInvalidSignature();
				return;
			}
			if (se == null) {
				if (verbose) {
					System.out.println("Trusty: " + np.getUri());
				}
				report.countTrusty();
			} else {
				boolean valid = false;
				try {
					valid = SignatureUtils.hasValidLegacySignature(se);
				} catch (GeneralSecurityException ex) {
					System.out.println("FAILED TO CHECK SIGNATURE: " + np.getUri() + " (" + ex.getMessage() + ")");
					report.countError();
					return;
				}
				if (valid) {
					if (verbose) {
						System.out.println("Signed and trusty: " + np.getUri());
					}
					report.countSigned();
				} else {
					System.out.println("INVALID SIGNATURE: " + np.getUri());
					report.countInvalidSignature();
				}
			}
		} else if (TrustyUriUtils.isPotentialTrustyUri(np.getUri())) {
			System.out.println("Looks like a trusty nanopub BUT VERIFICATION FAILED: " + np.getUri());
			report.countNotTrusty();
		} else {
			if (verbose) {
				System.out.println("Valid (but not trusty): " + np.getUri());
			}
			report.countNotTrusty();
		}
	}

	public void setLogPrintStream(PrintStream logOut) {
		this.logOut = logOut;
	}

	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}

	private void log(String message) {
		if (logOut != null) {
			logOut.print(message);
		}
	}


	public class Report {

		private int signed, trusty, notTrusty, invalidSignature, invalid, error;

		private Report() {
		}

		private void countSigned() {
			signed++;
		}

		public int getSignedCount() {
			return signed;
		}

		private void countTrusty() {
			trusty++;
		}

		public int getTrustyCount() {
			return trusty;
		}

		private void countNotTrusty() {
			notTrusty++;
		}

		public int getNotTrustyCount() {
			return notTrusty;
		}

		private void countInvalidSignature() {
			invalidSignature++;
		}

		public int getInvalidSignatureCount() {
			return invalidSignature;
		}

		private void countInvalid() {
			invalid++;
		}

		public int getInvalidCount() {
			return invalid;
		}

		private void countError() {
			error++;
		}

		public int getErrorCount() {
			return error;
		}

		public int getAllValidCount() {
			return signed + trusty + notTrusty;
		}

		public int getAllInvalidCount() {
			return invalidSignature + invalid + error;
		}

		public boolean areAllValid() {
			return getAllInvalidCount() == 0;
		}


		public String getSummary() {
			String s = "";
			if (signed > 0) s += " " + signed + " signed and trusty;";
			if (trusty > 0) s += " " + trusty + " trusty (unsigned);";
			if (notTrusty > 0) s += " " + notTrusty + " valid (not trusty);";
			if (invalidSignature > 0) s += " " + invalidSignature + " invalid signature;";
			if (invalid > 0) s += " " + invalid + " invalid nanopubs;";
			if (error > 0) s += " " + error + " errors;";
			s = s.replaceFirst("^ ", "");
			return s;
		}
	}

}
