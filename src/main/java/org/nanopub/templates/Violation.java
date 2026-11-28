package org.nanopub.templates;

import org.eclipse.rdf4j.model.Value;

import java.io.Serializable;

/**
 * Something that is wrong with a value in a position of a template statement.
 *
 * @param templateValue the value of the template in that position: a placeholder, or a fixed
 *                      IRI or literal; null if the template defines none
 * @param value         the value that was checked; null where a raw value was checked that
 *                      cannot be an RDF value for the position at all
 * @param kind          what is wrong
 * @param blocking      true if the value cannot stand in this position at all, false if it
 *                      can but breaks a constraint the template puts on it
 * @param message       a description of the problem for a person
 */
public record Violation(Value templateValue, Value value, Kind kind, boolean blocking, String message) implements Serializable {

    /**
     * The kinds of problems a value can have.
     */
    public enum Kind {

        /**
         * The template defines no value for this position.
         */
        NO_VALUE_DEFINED,

        /**
         * An IRI where a literal is needed, or the other way round.
         */
        WRONG_KIND,

        /**
         * The value differs from the fixed value the template has in this position.
         */
        DIFFERENT_VALUE,

        /**
         * The placeholder is bound to another value already.
         */
        ALREADY_BOUND,

        /**
         * The IRI is not well-formed.
         */
        MALFORMED_IRI,

        /**
         * The IRI has a scheme that nanopublications do not allow in this position.
         */
        SCHEME_NOT_ALLOWED,

        /**
         * The value does not match the regular expression of the placeholder.
         */
        REGEX,

        /**
         * The IRI is minted within the nanopublication, but the placeholder takes only IRIs
         * of things that exist outside of it.
         */
        NOT_EXTERNAL,

        /**
         * The IRI is not a trusty URI.
         */
        NOT_TRUSTY,

        /**
         * The literal lacks a language tag, or has one the placeholder does not take.
         */
        LANGUAGE_TAG,

        /**
         * The literal has another datatype than the placeholder declares.
         */
        DATATYPE,

        /**
         * The lexical form of the literal is not valid for its datatype.
         */
        ILL_TYPED,

        /**
         * The literal is not a point in time at one of the precisions the date placeholder
         * accepts.
         */
        DATE_PRECISION,

        /**
         * The value is not among those a restricted choice allows.
         */
        NOT_A_POSSIBLE_VALUE,

        /**
         * The IRI is not a creator of the nanopublication.
         */
        NOT_A_CREATOR,

        /**
         * The literal is the SPARQL code of a query and does not parse.
         */
        INVALID_SPARQL,

        /**
         * The IRI is minted under a name the nanopublication uses for one of its own parts.
         */
        RESERVED_NAME,

        /**
         * The text given for an IRI is neither an IRI in one of the allowed schemes nor a name
         * that can follow a prefix, as it has a colon or a hash in it.
         */
        NOT_AN_IRI_OR_NAME,

        /**
         * The prefix of the placeholder depends on the space or resource the nanopublication is
         * made for, which is not known.
         */
        UNRESOLVED_PREFIX,

        /**
         * The text given for a literal is not one as {@link LiteralSerialization} writes it.
         */
        MALFORMED_LITERAL

    }

}
