package org.nanopub.templates;

import com.beust.jcommander.ParameterException;
import org.eclipse.rdf4j.common.exception.RDF4JException;
import org.nanopub.CliRunner;
import org.nanopub.MalformedNanopubException;
import org.nanopub.MultiNanopubRdfHandler;
import org.nanopub.Nanopub;
import org.nanopub.templates.ComplianceIssue.Graph;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Command-line tool that checks nanopublications against the templates they name, with a
 * {@link TemplateComplianceChecker}.
 */
public class CheckTemplateCompliance extends CliRunner {

    @com.beust.jcommander.Parameter(description = "input-nanopubs", required = true)
    private List<String> inputNanopubs = new ArrayList<>();

    @com.beust.jcommander.Parameter(names = "-t", description = "File with a template nanopub to use instead of retrieving it; can be given several times")
    private List<String> templateFiles = new ArrayList<>();

    @com.beust.jcommander.Parameter(names = "-v", description = "Verbose: also show the templates checked against")
    private boolean verbose = false;

    /**
     * Runs the tool.
     *
     * @param args the files to check, and options
     */
    public static void main(String[] args) {
        try {
            CheckTemplateCompliance obj = CliRunner.initJc(new CheckTemplateCompliance(), args);
            int notCompliant = obj.run();
            if (notCompliant > 0) System.exit(1);
        } catch (ParameterException ex) {
            System.exit(1);
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }

    private PrintStream out = System.out;
    private NanopubResolver networkResolver = NanopubResolver.DEFAULT;
    private int count, compliantCount, warningCount;

    /**
     * Creates the tool without files to check, for {@link #main(String[])}.
     */
    public CheckTemplateCompliance() {
    }

    /**
     * Creates the tool for the given files.
     *
     * @param inputNanopubs the files to check, each with one or more nanopublications
     * @param templateFiles files with template nanopublications to use instead of retrieving
     *                      them, for templates that are not published yet
     */
    public CheckTemplateCompliance(List<String> inputNanopubs, List<String> templateFiles) {
        this.inputNanopubs = new ArrayList<>(inputNanopubs);
        this.templateFiles = new ArrayList<>(templateFiles);
    }

    /**
     * Sets where the findings are written to; standard output if not set.
     *
     * @param out the stream to write to
     */
    public void setOutput(PrintStream out) {
        this.out = out;
    }

    /**
     * Sets how templates are retrieved that are not among the given template files; from
     * the nanopublication network if not set.
     *
     * @param resolver the resolver for templates
     */
    public void setResolver(NanopubResolver resolver) {
        this.networkResolver = resolver;
    }

    /**
     * Checks the files and writes the findings.
     *
     * @return the number of nanopublications that do not comply with their templates, plus
     * the number of files that could not be read
     * @throws IOException if a file cannot be read
     */
    public int run() throws IOException {
        Map<String, Nanopub> localTemplates = new HashMap<>();
        for (String templateFile : templateFiles) {
            try {
                MultiNanopubRdfHandler.process(new File(templateFile), np -> localTemplates.put(np.getUri().stringValue(), np));
            } catch (RDF4JException | MalformedNanopubException ex) {
                throw new IOException("Cannot read template file " + templateFile + ": " + ex.getMessage(), ex);
            }
        }
        TemplateComplianceChecker checker = new TemplateComplianceChecker(id -> {
            Nanopub local = findLocal(localTemplates, id);
            return local != null ? local : networkResolver.getNanopub(id);
        });
        count = 0;
        compliantCount = 0;
        warningCount = 0;
        int unreadable = 0;
        for (String file : inputNanopubs) {
            int countBefore = count;
            try {
                MultiNanopubRdfHandler.process(new File(file), np -> check(np, checker));
                if (count == countBefore) {
                    out.println("NO NANOPUB FOUND: " + file);
                    unreadable++;
                }
            } catch (RDF4JException | MalformedNanopubException ex) {
                out.println("INVALID NANOPUB: " + file + " (" + ex.getMessage() + ")");
                unreadable++;
            }
        }
        out.println("Summary: " + count + " checked; " + compliantCount + " comply" + (warningCount > 0 ? " (" + warningCount + " with warnings)" : "")
                + "; " + (count - compliantCount) + " do not comply" + (unreadable > 0 ? "; " + unreadable + " could not be read" : ""));
        return count - compliantCount + unreadable;
    }

    private void check(Nanopub np, TemplateComplianceChecker checker) {
        count++;
        ComplianceReport report = checker.check(np);
        if (report.isCompliant()) {
            compliantCount++;
            if (!report.getWarnings().isEmpty()) warningCount++;
        }
        out.println((report.isCompliant() ? "COMPLIES: " : "DOES NOT COMPLY: ") + np.getUri());
        if (verbose) {
            for (Graph graph : Graph.values()) {
                TemplateComplianceChecker.getTemplateLinks(np, graph).forEach(link -> out.println("  " + graph.name().toLowerCase() + " template: " + link));
            }
        }
        for (ComplianceIssue issue : report.getIssues()) {
            out.println("- " + issue.severity() + ": " + issue.message());
        }
    }

    // Finds a template among the given files: by the URI of its nanopublication, by an IRI
    // within it (a template can be referred to by its node), or by its artifact code.
    private static Nanopub findLocal(Map<String, Nanopub> localTemplates, String id) {
        Nanopub exact = localTemplates.get(id);
        if (exact != null) return exact;
        String artifactCode = getArtifactCode(id);
        for (Map.Entry<String, Nanopub> e : localTemplates.entrySet()) {
            String uri = e.getKey();
            if (artifactCode != null && artifactCode.equals(getArtifactCode(uri))) return e.getValue();
            if (!id.startsWith(uri)) continue;
            if (uri.endsWith("/") || uri.endsWith("#") || "/#".indexOf(id.charAt(uri.length())) >= 0) return e.getValue();
        }
        return null;
    }

    private static final Pattern ARTIFACT_CODE = Pattern.compile("RA[A-Za-z0-9_-]{43}");

    private static String getArtifactCode(String iri) {
        Matcher m = ARTIFACT_CODE.matcher(iri);
        return m.find() ? m.group() : null;
    }

}
