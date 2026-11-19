package org.nanopub.templates;

import org.nanopub.Nanopub;

import java.util.List;

/**
 * The outcome of checking a nanopublication against the templates it names.
 */
public class ComplianceReport {

    private final Nanopub nanopub;
    private final List<ComplianceIssue> issues;
    private final TemplateMatch assertionMatch;
    private final TemplateMatch provenanceMatch;
    private final List<TemplateMatch> pubinfoMatches;

    ComplianceReport(Nanopub nanopub, List<ComplianceIssue> issues, TemplateMatch assertionMatch, TemplateMatch provenanceMatch,
                     List<TemplateMatch> pubinfoMatches) {
        this.nanopub = nanopub;
        this.issues = List.copyOf(issues);
        this.assertionMatch = assertionMatch;
        this.provenanceMatch = provenanceMatch;
        this.pubinfoMatches = List.copyOf(pubinfoMatches);
    }

    /**
     * @return the nanopublication that was checked
     */
    public Nanopub getNanopub() {
        return nanopub;
    }

    /**
     * Whether the nanopublication complies with its templates. A graph that no template is
     * named for is not checked and does not count against it.
     *
     * @return true if there is no error among the findings
     */
    public boolean isCompliant() {
        return issues.stream().noneMatch(ComplianceIssue::isError);
    }

    /**
     * @return all findings, errors and warnings
     */
    public List<ComplianceIssue> getIssues() {
        return issues;
    }

    /**
     * @return the findings because of which the nanopublication does not comply
     */
    public List<ComplianceIssue> getErrors() {
        return issues.stream().filter(ComplianceIssue::isError).toList();
    }

    /**
     * @return the findings that do not keep the nanopublication from complying
     */
    public List<ComplianceIssue> getWarnings() {
        return issues.stream().filter(issue -> !issue.isError()).toList();
    }

    /**
     * @return how the assertion was matched against its template; null if it was not
     */
    public TemplateMatch getAssertionMatch() {
        return assertionMatch;
    }

    /**
     * @return how the provenance was matched against its template; null if it was not
     */
    public TemplateMatch getProvenanceMatch() {
        return provenanceMatch;
    }

    /**
     * @return how the publication info was matched against each of its templates, each
     * against all of its triples
     */
    public List<TemplateMatch> getPubinfoMatches() {
        return pubinfoMatches;
    }

}
