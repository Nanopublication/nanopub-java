package org.nanopub.vocabulary;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;

public class NTEMPLATE {

    public static final String NAMESPACE = "https://w3id.org/np/o/ntemplate/";

    public static final String PREFIX = "ntemplate";

    public static final Namespace NS = Utils.createNamespace(PREFIX, NAMESPACE);

    /**
     * Represents the class for assertion templates.
     */
    public static final IRI ASSERTION_TEMPLATE = Utils.createIRI(NAMESPACE, "AssertionTemplate");

    /**
     * Represents the class for provenance templates.
     */
    public static final IRI PROVENANCE_TEMPLATE = Utils.createIRI(NAMESPACE, "ProvenanceTemplate");

    /**
     * Represents the class for publication information templates.
     */
    public static final IRI PUBINFO_TEMPLATE = Utils.createIRI(NAMESPACE, "PubinfoTemplate");

    /**
     * Represents the class for unlisted templates.
     */
    public static final IRI UNLISTED_TEMPLATE = Utils.createIRI(NAMESPACE, "UnlistedTemplate");

    /**
     * Predicate indicating a statement in the template.
     */
    public static final IRI HAS_STATEMENT = Utils.createIRI(NAMESPACE, "hasStatement");

    /**
     * Represents the class for local resources.
     */
    public static final IRI LOCAL_RESOURCE = Utils.createIRI(NAMESPACE, "LocalResource");

    /**
     * Represents the class for introduced resources.
     */
    public static final IRI INTRODUCED_RESOURCE = Utils.createIRI(NAMESPACE, "IntroducedResource");

    /**
     * Represents the class for embedded resources.
     */
    public static final IRI EMBEDDED_RESOURCE = Utils.createIRI(NAMESPACE, "EmbeddedResource");

    /**
     * Represents the class for value placeholders.
     */
    public static final IRI VALUE_PLACEHOLDER = Utils.createIRI(NAMESPACE, "ValuePlaceholder");

    /**
     * Represents the class for URI placeholders.
     */
    public static final IRI URI_PLACEHOLDER = Utils.createIRI(NAMESPACE, "UriPlaceholder");

    /**
     * Represents the class for auto-escaped URI placeholders.
     */
    public static final IRI AUTO_ESCAPE_URI_PLACEHOLDER = Utils.createIRI(NAMESPACE, "AutoEscapeUriPlaceholder");

    /**
     * Represents the class for external URI placeholders.
     */
    public static final IRI EXTERNAL_URI_PLACEHOLDER = Utils.createIRI(NAMESPACE, "ExternalUriPlaceholder");

    /**
     * Represents the class for trusty URI placeholders.
     */
    public static final IRI TRUSTY_URI_PLACEHOLDER = Utils.createIRI(NAMESPACE, "TrustyUriPlaceholder");

    /**
     * Represents the class for literal placeholders.
     */
    public static final IRI LITERAL_PLACEHOLDER = Utils.createIRI(NAMESPACE, "LiteralPlaceholder");

    /**
     * Represents the class for long literal placeholders.
     */
    public static final IRI LONG_LITERAL_PLACEHOLDER = Utils.createIRI(NAMESPACE, "LongLiteralPlaceholder");

    /**
     * Represents the class for restricted choice placeholders.
     */
    public static final IRI RESTRICTED_CHOICE_PLACEHOLDER = Utils.createIRI(NAMESPACE, "RestrictedChoicePlaceholder");

    /**
     * Represents the class for guided choice placeholders.
     */
    public static final IRI GUIDED_CHOICE_PLACEHOLDER = Utils.createIRI(NAMESPACE, "GuidedChoicePlaceholder");

    /**
     * Represents the class for agent placeholders.
     */
    public static final IRI AGENT_PLACEHOLDER = Utils.createIRI(NAMESPACE, "AgentPlaceholder");

    /**
     * Represents the placeholder for the creator.
     */
    public static final IRI CREATOR_PLACEHOLDER = Utils.createIRI(NAMESPACE, "CREATOR");

    /**
     * Represents the placeholder for assertions.
     */
    public static final IRI ASSERTION_PLACEHOLDER = Utils.createIRI(NAMESPACE, "ASSERTION");

    /**
     * Represents the placeholder for nanopublications.
     */
    public static final IRI NANOPUB_PLACEHOLDER = Utils.createIRI(NAMESPACE, "NANOPUB");

    /**
     * Predicate indicating creation from a template.
     */
    public static final IRI WAS_CREATED_FROM_TEMPLATE = Utils.createIRI(NAMESPACE, "wasCreatedFromTemplate");

    /**
     * Predicate indicating creation from a provenance template.
     */
    public static final IRI WAS_CREATED_FROM_PROVENANCE_TEMPLATE = Utils.createIRI(NAMESPACE, "wasCreatedFromProvenanceTemplate");

    /**
     * Predicate indicating creation from a pubinfo template.
     */
    public static final IRI WAS_CREATED_FROM_PUBINFO_TEMPLATE = Utils.createIRI(NAMESPACE, "wasCreatedFromPubinfoTemplate");

    /**
     * Predicate indicating the order of statements.
     */
    public static final IRI STATEMENT_ORDER = Utils.createIRI(NAMESPACE, "statementOrder");

    /**
     * Predicate indicating possible values.
     */
    public static final IRI POSSIBLE_VALUE = Utils.createIRI(NAMESPACE, "possibleValue");

    /**
     * Predicate indicating the source of possible values.
     */
    public static final IRI POSSIBLE_VALUES_FROM = Utils.createIRI(NAMESPACE, "possibleValuesFrom");

    /**
     * Predicate indicating possible values from an API.
     */
    public static final IRI POSSIBLE_VALUES_FROM_API = Utils.createIRI(NAMESPACE, "possibleValuesFromApi");

    /**
     * Predicate indicating a datatype for a literal placeholder.
     */
    public static final IRI HAS_DATATYPE = Utils.createIRI(NAMESPACE, "hasDatatype");

    /**
     * Predicate indicating the language attribute for a literal placeholder.
     */
    public static final IRI HAS_LANGUATE_ATTRIBUTE = Utils.createIRI(NAMESPACE, "hasLanguageAttribute");

    /**
     * Predicate indicating a prefix.
     */
    public static final IRI HAS_PREFIX = Utils.createIRI(NAMESPACE, "hasPrefix");

    /**
     * Predicate indicating a regular expression.
     */
    public static final IRI HAS_REGEX = Utils.createIRI(NAMESPACE, "hasRegex");

    /**
     * Predicate indicating a prefix label.
     */
    public static final IRI HAS_PREFIX_LABEL = Utils.createIRI(NAMESPACE, "hasPrefixLabel");

    /**
     * Represents the class for optional statements.
     */
    public static final IRI OPTIONAL_STATEMENT = Utils.createIRI(NAMESPACE, "OptionalStatement");

    /**
     * Represents the class for grouped statements.
     */
    public static final IRI GROUPED_STATEMENT = Utils.createIRI(NAMESPACE, "GroupedStatement");

    /**
     * Represents the class for repeatable statements.
     */
    public static final IRI REPEATABLE_STATEMENT = Utils.createIRI(NAMESPACE, "RepeatableStatement");

    /**
     * Predicate indicating default provenance.
     */
    public static final IRI HAS_DEFAULT_PROVENANCE = Utils.createIRI(NAMESPACE, "hasDefaultProvenance");

    /**
     * Predicate indicating required pubinfo elements.
     */
    public static final IRI HAS_REQUIRED_PUBINFO_ELEMENT = Utils.createIRI(NAMESPACE, "hasRequiredPubinfoElement");

    /**
     * Predicate indicating a tag.
     */
    public static final IRI HAS_TAG = Utils.createIRI(NAMESPACE, "hasTag");

    /**
     * Predicate indicating a label from an API.
     */
    public static final IRI HAS_LABEL_FROM_API = Utils.createIRI(NAMESPACE, "hasLabelFromApi");

    /**
     * Predicate indicating a default value.
     */
    public static final IRI HAS_DEFAULT_VALUE = Utils.createIRI(NAMESPACE, "hasDefaultValue");

    /**
     * Predicate indicating a target namespace.
     */
    public static final IRI HAS_TARGET_NAMESPACE = Utils.createIRI(NAMESPACE, "hasTargetNamespace");

    /**
     * Predicate indicating a nanopublication label pattern.
     */
    public static final IRI HAS_NANOPUB_LABEL_PATTERN = Utils.createIRI(NAMESPACE, "hasNanopubLabelPattern");

    /**
     * Predicate indicating a target nanopublication type.
     */
    public static final IRI HAS_TARGET_NANOPUB_TYPE = Utils.createIRI(NAMESPACE, "hasTargetNanopubType");

    /**
     * Represents the placeholder for sequence elements.
     */
    public static final IRI SEQUENCE_ELEMENT_PLACEHOLDER = Utils.createIRI(NAMESPACE, "SequenceElementPlaceholder");

}
