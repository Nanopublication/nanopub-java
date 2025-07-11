package org.nanopub;

import com.beust.jcommander.ParameterException;
import net.trustyuri.TrustyUriUtils;
import org.eclipse.rdf4j.common.exception.RDF4JException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sparql.SPARQLRepository;
import org.nanopub.MultiNanopubRdfHandler.NanopubHandler;
import org.nanopub.extra.security.LegacySignatureUtils;
import org.nanopub.extra.security.MalformedCryptoElementException;
import org.nanopub.extra.security.NanopubSignatureElement;
import org.nanopub.extra.security.SignatureUtils;
import org.nanopub.trusty.TrustyNanopubUtils;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;

/**
 * Command line tool to check nanopubs for validity and trustiness.
 */
public class CheckNanopub extends CliRunner {

    @com.beust.jcommander.Parameter(description = "input-nanopubs", required = true)
    private List<String> inputNanopubs = new ArrayList<>();

    @com.beust.jcommander.Parameter(names = "-v", description = "Verbose")
    private boolean verbose = false;

    @com.beust.jcommander.Parameter(names = "-s", description = "Load nanopubs from given SPARQL endpoint")
    private String sparqlEndpointUrl;

    /**
     * Main method to run the CheckNanopub tool from the command line.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        try {
            CheckNanopub obj = CliRunner.initJc(new CheckNanopub(), args);
            obj.setLogPrintStream(System.out);
            Report report = obj.check();
            System.out.println("Summary: " + report.getSummary());
        } catch (ParameterException ex) {
            System.exit(1);
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }

    private Report report;
    private int count;
    private SPARQLRepository sparqlRepo;
    private PrintStream logOut;

    /**
     * Default constructor for CheckNanopub.
     * Initializes the CliRunner with the command line parameters.
     */
    public CheckNanopub() {
    }

    /**
     * This constructor does not initialize the CliRunner as usual. It's for testing purposes.
     *
     * @param inputNanopubFiles a list of nanopub files to check
     */
    public CheckNanopub(List<String> inputNanopubFiles) {
        this.inputNanopubs = inputNanopubFiles;
    }

    /**
     * This constructor does not initialize the CliRunner as usual. It's for testing purposes.
     *
     * @param sparqlEndpointUrl the SPARQL endpoint URL to use for checking nanopubs
     * @param inputNanopubIds   a list of nanopub IDs to check
     */
    public CheckNanopub(String sparqlEndpointUrl, List<String> inputNanopubIds) {
        super();
        this.inputNanopubs = inputNanopubIds;
        this.sparqlEndpointUrl = sparqlEndpointUrl;
    }

    /**
     * Checks the nanopubs provided.
     *
     * @return a Report object containing the results of the checks
     * @throws IOException if an I/O error occurs
     */
    public Report check() throws IOException {
        report = new Report();
        for (String s : inputNanopubs) {
            count = 0;
            try {
                if (sparqlEndpointUrl != null) {
                    if (sparqlRepo == null) {
                        sparqlRepo = new SPARQLRepository(sparqlEndpointUrl);
                        sparqlRepo.init();
                    }
                    Nanopub np = new NanopubImpl(sparqlRepo, SimpleValueFactory.getInstance().createIRI(s));
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
            } catch (RDF4JException ex) {
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
            NanopubSignatureElement se = null;
            NanopubSignatureElement legacySe = null;
            try {
                se = SignatureUtils.getSignatureElement(np);
                if (se == null) legacySe = LegacySignatureUtils.getSignatureElement(np);
            } catch (MalformedCryptoElementException ex) {
                System.out.println("SIGNATURE IS NOT WELL-FORMED (" + ex.getMessage() + "): " + np.getUri());
                report.countInvalidSignature();
                return;
            }
            if (se == null && legacySe == null) {
                // no signature
                if (verbose) {
                    System.out.println("Trusty (without signature): " + np.getUri());
                }
                report.countTrusty();
            } else if (se != null) {
                // new signature
                boolean valid = false;
                try {
                    valid = SignatureUtils.hasValidSignature(se);
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
            } else {
                // legacy signature
                boolean valid = false;
                try {
                    valid = LegacySignatureUtils.hasValidSignature(legacySe);
                } catch (GeneralSecurityException ex) {
                    System.out.println("FAILED TO CHECK LEGACY SIGNATURE: " + np.getUri() + " (" + ex.getMessage() + ")");
                    report.countError();
                    return;
                }
                if (valid) {
                    if (verbose) {
                        System.out.println("Trusty with legacy signature: " + np.getUri());
                    }
                    report.countLegacySigned();
                } else {
                    System.out.println("INVALID LEGACY SIGNATURE: " + np.getUri());
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
        if (verbose) {
            System.out.println("LABEL: " + NanopubUtils.getLabel(np));
            System.out.println("TYPES:");
            for (IRI typeIri : NanopubUtils.getTypes(np)) {
                System.out.println("- " + typeIri);
            }
            System.out.println("DESCRIPTION:\n" + NanopubUtils.getDescription(np));
            System.out.println("AUTHORS:");
            for (IRI authorIri : SimpleCreatorPattern.getAuthors(np)) {
                System.out.println("- " + authorIri);
            }
            System.out.println("AUTHOR LIST:");
            for (IRI authorIri : SimpleCreatorPattern.getAuthorList(np)) {
                System.out.println("- " + authorIri);
            }
            System.out.println("CREATORS:");
            for (IRI creatorIri : SimpleCreatorPattern.getCreators(np)) {
                System.out.println("- " + creatorIri);
            }
        }
    }

    /**
     * Sets the PrintStream for logging output.
     *
     * @param logOut the PrintStream to use for logging
     */
    public void setLogPrintStream(PrintStream logOut) {
        this.logOut = logOut;
    }

    /**
     * Sets whether the tool should run in verbose mode.
     *
     * @param verbose true if verbose output is desired, false otherwise
     */
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }


    private void log(String message) {
        if (logOut != null) {
            logOut.print(message);
        }
    }

    /**
     * Report class to hold the results of the nanopub checks.
     */
    public class Report {

        private int signed, legacySigned, trusty, notTrusty, invalidSignature, invalid, error;

        private Report() {
        }

        private void countSigned() {
            signed++;
        }

        /**
         * Returns the count of nanopubs that are signed.
         *
         * @return the number of signed nanopubs
         */
        public int getSignedCount() {
            return signed;
        }

        private void countLegacySigned() {
            legacySigned++;
        }

        /**
         * Returns the count of nanopubs that are signed with a legacy signature.
         *
         * @return the number of legacy signed nanopubs
         */
        public int getLegacySignedCount() {
            return legacySigned;
        }

        private void countTrusty() {
            trusty++;
        }

        /**
         * Returns the count of nanopubs that are considered trusty.
         *
         * @return the number of trusty nanopubs
         */
        public int getTrustyCount() {
            return trusty;
        }

        private void countNotTrusty() {
            notTrusty++;
        }

        /**
         * Returns the count of nanopubs that are valid but not considered trusty.
         *
         * @return the number of valid but not trusty nanopubs
         */
        public int getNotTrustyCount() {
            return notTrusty;
        }

        private void countInvalidSignature() {
            invalidSignature++;
        }

        /**
         * Returns the count of nanopubs that have an invalid signature.
         *
         * @return the number of nanopubs with an invalid signature
         */
        public int getInvalidSignatureCount() {
            return invalidSignature;
        }

        private void countInvalid() {
            invalid++;
        }

        /**
         * Returns the count of nanopubs that are invalid (not well-formed).
         *
         * @return the number of invalid nanopubs
         */
        public int getInvalidCount() {
            return invalid;
        }

        private void countError() {
            error++;
        }

        /**
         * Returns the count of nanopubs that encountered an error during processing.
         *
         * @return the number of nanopubs with errors
         */
        public int getErrorCount() {
            return error;
        }

        /**
         * Returns the total count of all valid nanopubs (signed, legacy signed, trusty, and not trusty).
         *
         * @return the total count of valid nanopubs
         */
        public int getAllValidCount() {
            return signed + legacySigned + trusty + notTrusty;
        }

        /**
         * Returns the total count of all invalid nanopubs (invalid signature, invalid, and error).
         *
         * @return the total count of invalid nanopubs
         */
        public int getAllInvalidCount() {
            return invalidSignature + invalid + error;
        }

        /**
         * Returns the total count of all nanopubs processed (both valid and invalid).
         *
         * @return the total count of all nanopubs
         */
        public int getAllCount() {
            return getAllValidCount() + getAllInvalidCount();
        }

        /**
         * Returns the total count of all nanopubs processed (both valid and invalid).
         *
         * @return the total count of all nanopubs
         */
        public boolean areAllValid() {
            return getAllInvalidCount() == 0;
        }

        /**
         * Returns whether all nanopubs are considered trusty (including those with legacy signatures).
         *
         * @return true if all nanopubs are trusty, false otherwise
         */
        public boolean areAllTrusty() {
            return trusty + signed + legacySigned == getAllCount();
        }

        /**
         * Returns whether all nanopubs are signed (including those with legacy signatures).
         *
         * @return true if all nanopubs are signed, false otherwise
         */
        public boolean areAllSigned() {
            return signed + legacySigned == getAllCount();
        }

        /**
         * Returns a summary of the counts of nanopubs processed.
         *
         * @return a string summarizing the counts of nanopubs
         */
        public String getSummary() {
            String s = "";
            if (signed > 0) s += " " + signed + " trusty with signature;";
            if (legacySigned > 0) s += " " + legacySigned + " trusty with legacy signature;";
            if (trusty > 0) s += " " + trusty + " trusty (without signature);";
            if (notTrusty > 0) s += " " + notTrusty + " valid (not trusty);";
            if (invalidSignature > 0) s += " " + invalidSignature + " invalid signature;";
            if (invalid > 0) s += " " + invalid + " invalid nanopubs;";
            if (error > 0) s += " " + error + " errors;";
            s = s.replaceFirst("^ ", "");
            return s;
        }
    }

}
