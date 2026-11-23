package org.nanopub.extra.checks;

import java.io.Serializable;

/**
 * One finding of a {@link NetworkChecker}.
 *
 * @param severity whether something is wrong, or only worth knowing
 * @param kind     what was found
 * @param message  a description of the finding for a person
 */
public record NetworkFinding(Severity severity, Kind kind, String message) implements Serializable {

    /**
     * How much a finding counts.
     */
    public enum Severity {

        /**
         * The nanopublication will not have the effect it is meant to have.
         */
        ERROR,

        /**
         * Worth knowing, and possibly intended.
         */
        NOTE

    }

    /**
     * The kinds of findings.
     */
    public enum Kind {

        /**
         * A new version no longer introduces a resource that the version before it
         * introduces, and introduces one under its own URI instead: the resource has got a
         * new IRI with the new version.
         */
        REMINTED_RESOURCE,

        /**
         * As {@link #REMINTED_RESOURCE}, for a query. A query is specific to its version, so
         * that it gets a new IRI is right, but it should then be embedded, not introduced.
         */
        REMINTED_QUERY,

        /**
         * As {@link #REMINTED_RESOURCE}, for the assertion of the nanopublication itself,
         * which a template in the old shape introduces. It cannot keep its IRI.
         */
        REMINTED_ASSERTION,

        /**
         * A new version no longer introduces a resource that the version before it
         * introduces, and introduces nothing of its own in its place.
         */
        DROPPED_RESOURCE,

        /**
         * A template link names a template of another kind than the link is for, or
         * something that is no template.
         */
        WRONG_TEMPLATE_KIND,

        /**
         * The nanopublication names an assertion template, but none for its provenance or
         * its publication info.
         */
        MISSING_TEMPLATE_LINK,

        /**
         * A definition names a space that governs it, but that has no effect.
         */
        GOVERNANCE_WITHOUT_EFFECT,

        /**
         * A new version is signed with another key than the version before it, and is not
         * governed by a space, so that it does not take the place of that version.
         */
        OTHER_KEY,

        /**
         * Something could not be checked, as a nanopublication could not be retrieved or a
         * service did not answer.
         */
        UNCHECKED

    }

    /**
     * @return true if this finding is an error
     */
    public boolean isError() {
        return severity == Severity.ERROR;
    }

}
