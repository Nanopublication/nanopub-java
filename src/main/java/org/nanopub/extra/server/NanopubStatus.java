package org.nanopub.extra.server;

import com.beust.jcommander.ParameterException;
import net.trustyuri.ArtifactCode;
import net.trustyuri.TrustyUriUtils;
import net.trustyuri.rdf.RdfModule;
import org.eclipse.rdf4j.common.exception.RDF4JException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.nanopub.CliRunner;
import org.nanopub.MalformedNanopubException;
import org.nanopub.Nanopub;
import org.nanopub.extra.index.IndexUtils;
import org.nanopub.extra.index.NanopubIndex;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

/**
 * Command line tool to check the status of a nanopub (or index nanopub) on all nanopub servers.
 */
public class NanopubStatus extends CliRunner {

    @com.beust.jcommander.Parameter(description = "nanopub-uri-or-artifact-code", required = true)
    private List<String> nanopubIds;

    @com.beust.jcommander.Parameter(names = "-v", description = "Verbose")
    private boolean verbose = false;

    @com.beust.jcommander.Parameter(names = "-r", description = "Recursive (check entire content of index)")
    private boolean recursive = false;

    @com.beust.jcommander.Parameter(names = "-a", description = "Check all servers (do not stop after the first successful one)")
    private boolean checkAllServers = false;

    /**
     * Main method to run the NanopubStatus command line tool.
     * Exits with status 1 if the nanopub, or in recursive mode any nanopub of
     * the index, is not found on any registry.
     *
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        try {
            NanopubStatus obj = CliRunner.initJc(new NanopubStatus(), args);
            obj.run();
            int exitStatus = obj.exitStatus();
            if (exitStatus != 0) {
                System.exit(exitStatus);
            }
        } catch (ParameterException ex) {
            System.exit(1);
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }

    private static ArtifactCode getArtifactCode(String uriOrArtifactCode) {
        return extractArtifactCode(uriOrArtifactCode);
    }

    static ArtifactCode extractArtifactCode(String uriOrArtifactCode) {
        if (uriOrArtifactCode.indexOf(":") > 0) {
            IRI uri = SimpleValueFactory.getInstance().createIRI(uriOrArtifactCode);
            if (!TrustyUriUtils.isPotentialTrustyUri(uri)) {
                throw new IllegalArgumentException("Not a well-formed trusty URI");
            }
            return ArtifactCode.of(TrustyUriUtils.getArtifactCode(uri.toString()));
        } else {
            if (!TrustyUriUtils.isPotentialArtifactCode(uriOrArtifactCode)) {
                throw new IllegalArgumentException("Not a well-formed artifact code");
            }
            return ArtifactCode.of(uriOrArtifactCode);
        }
    }

    private int contentNpCount, indexNpCount;
    private int notFoundCount;
    private int minCount = -1;

    /**
     * Default constructor for NanopubStatus.
     * Initializes the command line parameters.
     */
    public NanopubStatus() {
    }

    /**
     * Checks the status of the given nanopub and, in recursive mode, of the
     * entire content of the index.
     *
     * @throws IOException         if an I/O error occurs
     * @throws RDFHandlerException if an error occurs while handling RDF data
     */
    void run() throws IOException, RDFHandlerException {
        checkNanopub(nanopubIds.getFirst(), recursive);
        if (recursive) {
            System.out.print(indexNpCount + " index nanopub" + (indexNpCount != 1 ? "s" : "") + "; ");
            System.out.println(contentNpCount + " content nanopub" + (contentNpCount != 1 ? "s" : ""));
            if (notFoundCount > 0) {
                System.out.println(notFoundCount + " nanopub" + (notFoundCount != 1 ? "s" : "") + " not found on any nanopub server");
            }
            if (checkAllServers) {
                System.out.println("Each found on at least " + minCount + " nanopub server" + (minCount != 1 ? "s" : "") + ".");
            }
        }
    }

    /**
     * Returns the process exit status for the last run: 0 if every checked
     * nanopub was found on at least one registry, 1 otherwise.
     *
     * @return 0 if all checked nanopubs were found, 1 otherwise
     */
    int exitStatus() {
        return notFoundCount == 0 ? 0 : 1;
    }

    private void checkNanopub(String nanopubId, boolean checkIndexContent) {
        ArtifactCode ac = getArtifactCode(nanopubId);
        if (!ac.getModule().getModuleId().equals(RdfModule.MODULE_ID)) {
            System.err.println("ERROR. Not a trusty URI of type " + RdfModule.MODULE_ID + ": " + nanopubId);
            System.exit(1);
        }
        int count = 0;
        ServerIterator serverIterator = new ServerIterator();
        Nanopub nanopub = null;
        while (serverIterator.hasNext()) {
            RegistryInfo registryInfo = serverIterator.next();
            try {
                Nanopub np = GetNanopub.get(ac.toString(), registryInfo);
                if (np != null) {
                    if (checkIndexContent && !IndexUtils.isIndex(np)) {
                        System.err.println("ERROR. Not an index: " + nanopubId);
                        System.exit(1);
                    }
                    if (nanopub == null) {
                        nanopub = np;
                    }
                    if (!recursive || verbose) {
                        System.out.println("URL: " + registryInfo.getCollectionUrl() + ac);
                    }
                    if (checkAllServers) {
                        count++;
                    } else {
                        break;
                    }
                }
            } catch (FileNotFoundException ex) {
                if (verbose && !recursive) {
                    System.out.println("NOT FOUND ON: " + registryInfo);
                }
            } catch (IOException ex) {
                if (verbose && !recursive) {
                    System.out.println("CONNECTION ERROR: " + registryInfo);
                }
            } catch (RDF4JException | MalformedNanopubException ex) {
                if (verbose && !recursive) {
                    System.out.println("VALIDATION ERROR: " + registryInfo);
                }
            }
        }
        if (checkAllServers) {
            String text = "Found on " + count + " nanopub server" + (count != 1 ? "s" : "");
            if (!recursive) {
                System.out.println(text + ".");
            } else if (verbose) {
                System.out.println(text + ": " + ac);
            }
            if (minCount < 0 || minCount > count) {
                minCount = count;
            }
        }
        if (nanopub != null) {
            if (checkIndexContent) {
                indexNpCount++;
                NanopubIndex npi = null;
                try {
                    npi = IndexUtils.castToIndex(nanopub);
                } catch (MalformedNanopubException ex) {
                    ex.printStackTrace();
                    System.exit(1);
                }
                for (IRI elementUri : npi.getElements()) {
                    checkNanopub(elementUri.toString(), false);
                }
                for (IRI subIndexUri : npi.getSubIndexes()) {
                    checkNanopub(subIndexUri.toString(), true);
                }
                if (npi.getAppendedIndex() != null) {
                    checkNanopub(npi.getAppendedIndex().toString(), true);
                }
            } else {
                contentNpCount++;
            }
        } else {
            System.err.println("NOT FOUND: " + nanopubId);
            notFoundCount++;
        }
        if ((indexNpCount + contentNpCount) % 100 == 0) {
            System.err.print((indexNpCount + contentNpCount) + " nanopubs...\r");
        }
    }

}
