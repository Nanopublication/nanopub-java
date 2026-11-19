package org.nanopub.templates;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;

import java.io.Serializable;

/**
 * One finding of a {@link TemplateComplianceChecker}: a way in which a nanopublication
 * differs from what its templates describe.
 *
 * @param severity  whether the nanopublication fails to comply because of this
 * @param graph     the graph of the nanopublication the finding is about
 * @param kind      what was found
 * @param message   a description of the finding for a person
 * @param template  the template concerned, as the nanopublication refers to it, or null
 * @param statement the statement of the template concerned, or null
 * @param triple    the triple of the nanopublication concerned, or null
 */
public record ComplianceIssue(Severity severity, Graph graph, Kind kind, String message, IRI template, IRI statement,
                              Statement triple) implements Serializable {

    /**
     * How much a finding counts.
     */
    public enum Severity {

        /**
         * The nanopublication does not comply with its templates.
         */
        ERROR,

        /**
         * Worth looking at, but the nanopublication may still comply.
         */
        WARNING

    }

    /**
     * The graphs of a nanopublication that templates describe.
     */
    public enum Graph {
        ASSERTION, PROVENANCE, PUBINFO
    }

    /**
     * The kinds of findings.
     */
    public enum Kind {

        /**
         * The nanopublication names no template for the graph, so the graph is not checked.
         */
        NO_TEMPLATE,

        /**
         * The nanopublication names more than one template where only one is used.
         */
        SEVERAL_TEMPLATES,

        /**
         * A template the nanopublication names cannot be retrieved or cannot be read.
         */
        TEMPLATE_UNAVAILABLE,

        /**
         * A template the nanopublication names is for another graph.
         */
        WRONG_TEMPLATE_KIND,

        /**
         * The template itself has a statement that cannot give valid triples.
         */
        TEMPLATE_ERROR,

        /**
         * A statement of the template that is not optional has no triples.
         */
        MISSING_STATEMENT,

        /**
         * A triple of the nanopublication belongs to no statement of its templates.
         */
        UNMATCHED_TRIPLE,

        /**
         * A value breaks a constraint the template puts on its placeholder.
         */
        VIOLATION,

        /**
         * A value is an IRI under a name the nanopublication keeps for one of its own parts,
         * such as {@code assertion}, so it is that part rather than a resource of its own.
         */
        RESERVED_NAME,

        /**
         * The nanopublication lacks a type the assertion template sets.
         */
        MISSING_TYPE,

        /**
         * The nanopublication names no version of a publication info template that the
         * assertion template requires.
         */
        MISSING_PUBINFO_ELEMENT,

        /**
         * A resource the template marks as introduced is not declared as such in the
         * publication info.
         */
        UNDECLARED_INTRODUCED_RESOURCE,

        /**
         * A resource the template marks as embedded is not declared as such in the
         * publication info.
         */
        UNDECLARED_EMBEDDED_RESOURCE,

        /**
         * The publication info lacks the direction of a role predicate that the template
         * pins, or the type that goes with it.
         */
        MISSING_ROLE_PIN

    }

    /**
     * @return true if the nanopublication fails to comply because of this finding
     */
    public boolean isError() {
        return severity == Severity.ERROR;
    }

}
