package org.nanopub.vocabulary;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;

public class NTEMPLATE {

    public static final String NAMESPACE = "https://w3id.org/np/o/ntemplate/";

    public static final String PREFIX = "ntemplate";

    public static final Namespace NS = VocabUtils.createNamespace(PREFIX, NAMESPACE);

    /**
     * Represents the class for assertion templates.
     */
    public static final IRI ASSERTION_TEMPLATE = VocabUtils.createIRI(NAMESPACE, "AssertionTemplate");

    /**
     * Represents the class for provenance templates.
     */
    public static final IRI PROVENANCE_TEMPLATE = VocabUtils.createIRI(NAMESPACE, "ProvenanceTemplate");

    /**
     * Represents the class for publication information templates.
     */
    public static final IRI PUBINFO_TEMPLATE = VocabUtils.createIRI(NAMESPACE, "PubinfoTemplate");

    /**
     * Represents the class for unlisted templates.
     */
    public static final IRI UNLISTED_TEMPLATE = VocabUtils.createIRI(NAMESPACE, "UnlistedTemplate");

    /**
     * Predicate indicating a statement in the template.
     */
    public static final IRI HAS_STATEMENT = VocabUtils.createIRI(NAMESPACE, "hasStatement");

    /**
     * Represents the class for local resources.
     */
    public static final IRI LOCAL_RESOURCE = VocabUtils.createIRI(NAMESPACE, "LocalResource");

    /**
     * Represents the class for introduced resources.
     */
    public static final IRI INTRODUCED_RESOURCE = VocabUtils.createIRI(NAMESPACE, "IntroducedResource");

    /**
     * Represents the class for embedded resources.
     */
    public static final IRI EMBEDDED_RESOURCE = VocabUtils.createIRI(NAMESPACE, "EmbeddedResource");

    /**
     * Represents the class for value placeholders.
     */
    public static final IRI VALUE_PLACEHOLDER = VocabUtils.createIRI(NAMESPACE, "ValuePlaceholder");

    /**
     * Represents the class for URI placeholders.
     */
    public static final IRI URI_PLACEHOLDER = VocabUtils.createIRI(NAMESPACE, "UriPlaceholder");

    /**
     * Represents the class for auto-escaped URI placeholders.
     */
    public static final IRI AUTO_ESCAPE_URI_PLACEHOLDER = VocabUtils.createIRI(NAMESPACE, "AutoEscapeUriPlaceholder");

    /**
     * Represents the class for external URI placeholders.
     */
    public static final IRI EXTERNAL_URI_PLACEHOLDER = VocabUtils.createIRI(NAMESPACE, "ExternalUriPlaceholder");

    /**
     * Represents the class for trusty URI placeholders.
     */
    public static final IRI TRUSTY_URI_PLACEHOLDER = VocabUtils.createIRI(NAMESPACE, "TrustyUriPlaceholder");

    /**
     * Represents the class for literal placeholders.
     */
    public static final IRI LITERAL_PLACEHOLDER = VocabUtils.createIRI(NAMESPACE, "LiteralPlaceholder");

    /**
     * Represents the class for long literal placeholders.
     */
    public static final IRI LONG_LITERAL_PLACEHOLDER = VocabUtils.createIRI(NAMESPACE, "LongLiteralPlaceholder");

    /**
     * Represents the class for restricted choice placeholders.
     */
    public static final IRI RESTRICTED_CHOICE_PLACEHOLDER = VocabUtils.createIRI(NAMESPACE, "RestrictedChoicePlaceholder");

    /**
     * Represents the class for guided choice placeholders.
     */
    public static final IRI GUIDED_CHOICE_PLACEHOLDER = VocabUtils.createIRI(NAMESPACE, "GuidedChoicePlaceholder");

    /**
     * Represents the class for agent placeholders.
     */
    public static final IRI AGENT_PLACEHOLDER = VocabUtils.createIRI(NAMESPACE, "AgentPlaceholder");

    /**
     * Represents the placeholder for the creator.
     */
    public static final IRI CREATOR_PLACEHOLDER = VocabUtils.createIRI(NAMESPACE, "CREATOR");

    /**
     * Represents the placeholder for assertions.
     */
    public static final IRI ASSERTION_PLACEHOLDER = VocabUtils.createIRI(NAMESPACE, "ASSERTION");

    /**
     * Represents the placeholder for nanopublications.
     */
    public static final IRI NANOPUB_PLACEHOLDER = VocabUtils.createIRI(NAMESPACE, "NANOPUB");

    /**
     * Predicate indicating creation from a template.
     */
    public static final IRI WAS_CREATED_FROM_TEMPLATE = VocabUtils.createIRI(NAMESPACE, "wasCreatedFromTemplate");

    /**
     * Predicate indicating creation from a provenance template.
     */
    public static final IRI WAS_CREATED_FROM_PROVENANCE_TEMPLATE = VocabUtils.createIRI(NAMESPACE, "wasCreatedFromProvenanceTemplate");

    /**
     * Predicate indicating creation from a pubinfo template.
     */
    public static final IRI WAS_CREATED_FROM_PUBINFO_TEMPLATE = VocabUtils.createIRI(NAMESPACE, "wasCreatedFromPubinfoTemplate");

    /**
     * Predicate indicating the order of statements.
     */
    public static final IRI STATEMENT_ORDER = VocabUtils.createIRI(NAMESPACE, "statementOrder");

    /**
     * Predicate indicating possible values.
     */
    public static final IRI POSSIBLE_VALUE = VocabUtils.createIRI(NAMESPACE, "possibleValue");

    /**
     * Predicate indicating the source of possible values.
     */
    public static final IRI POSSIBLE_VALUES_FROM = VocabUtils.createIRI(NAMESPACE, "possibleValuesFrom");

    /**
     * Predicate indicating possible values from an API.
     */
    public static final IRI POSSIBLE_VALUES_FROM_API = VocabUtils.createIRI(NAMESPACE, "possibleValuesFromApi");

    /**
     * Predicate indicating a datatype for a literal placeholder.
     */
    public static final IRI HAS_DATATYPE = VocabUtils.createIRI(NAMESPACE, "hasDatatype");

    /**
     * Predicate indicating the language attribute for a literal placeholder.
     */
    public static final IRI HAS_LANGUAGE_TAG = VocabUtils.createIRI(NAMESPACE, "hasLanguageTag");

    /**
     * Predicate indicating the language attribute for a literal placeholder.
     */
    public static final IRI HAS_LANGUAGE_ATTRIBUTE = VocabUtils.createIRI(NAMESPACE, "hasLanguageAttribute");

    /**
     * Predicate indicating a prefix.
     */
    public static final IRI HAS_PREFIX = VocabUtils.createIRI(NAMESPACE, "hasPrefix");

    /**
     * Predicate indicating a regular expression.
     */
    public static final IRI HAS_REGEX = VocabUtils.createIRI(NAMESPACE, "hasRegex");

    /**
     * Predicate indicating a prefix label.
     */
    public static final IRI HAS_PREFIX_LABEL = VocabUtils.createIRI(NAMESPACE, "hasPrefixLabel");

    /**
     * Represents the class for optional statements.
     */
    public static final IRI OPTIONAL_STATEMENT = VocabUtils.createIRI(NAMESPACE, "OptionalStatement");

    /**
     * Represents the class for grouped statements.
     */
    public static final IRI GROUPED_STATEMENT = VocabUtils.createIRI(NAMESPACE, "GroupedStatement");

    /**
     * Represents the class for repeatable statements.
     */
    public static final IRI REPEATABLE_STATEMENT = VocabUtils.createIRI(NAMESPACE, "RepeatableStatement");

    /**
     * Predicate indicating default provenance.
     */
    public static final IRI HAS_DEFAULT_PROVENANCE = VocabUtils.createIRI(NAMESPACE, "hasDefaultProvenance");

    /**
     * Predicate indicating required pubinfo elements.
     */
    public static final IRI HAS_REQUIRED_PUBINFO_ELEMENT = VocabUtils.createIRI(NAMESPACE, "hasRequiredPubinfoElement");

    /**
     * Predicate indicating a tag.
     */
    public static final IRI HAS_TAG = VocabUtils.createIRI(NAMESPACE, "hasTag");

    /**
     * Predicate indicating a label from an API.
     */
    public static final IRI HAS_LABEL_FROM_API = VocabUtils.createIRI(NAMESPACE, "hasLabelFromApi");

    /**
     * Predicate indicating a default value.
     */
    public static final IRI HAS_DEFAULT_VALUE = VocabUtils.createIRI(NAMESPACE, "hasDefaultValue");

    /**
     * Predicate indicating a target namespace.
     */
    public static final IRI HAS_TARGET_NAMESPACE = VocabUtils.createIRI(NAMESPACE, "hasTargetNamespace");

    /**
     * Predicate indicating a nanopublication label pattern.
     */
    public static final IRI HAS_NANOPUB_LABEL_PATTERN = VocabUtils.createIRI(NAMESPACE, "hasNanopubLabelPattern");

    /**
     * Predicate indicating a target nanopublication type.
     */
    public static final IRI HAS_TARGET_NANOPUB_TYPE = VocabUtils.createIRI(NAMESPACE, "hasTargetNanopubType");

    /**
     * Represents the placeholder for sequence elements.
     */
    public static final IRI SEQUENCE_ELEMENT_PLACEHOLDER = VocabUtils.createIRI(NAMESPACE, "SequenceElementPlaceholder");

}
