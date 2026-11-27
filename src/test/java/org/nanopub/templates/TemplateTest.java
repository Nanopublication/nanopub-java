package org.nanopub.templates;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.junit.jupiter.api.Test;
import org.nanopub.Nanopub;
import org.nanopub.NanopubCreator;
import org.nanopub.NanopubUtils;
import org.nanopub.vocabulary.KPXL;
import org.nanopub.vocabulary.KPXL_GRLC;
import org.nanopub.vocabulary.NTEMPLATE;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for what a {@link Template} tells about its placeholders, statements and constraints,
 * on templates built in memory.
 */
class TemplateTest {

    private static final ValueFactory vf = SimpleValueFactory.getInstance();

    private static final String NP_URI = TemplateBuilder.NP_URI;
    private static final IRI SUBJECT = TemplateBuilder.SUBJECT;
    private static final IRI PREDICATE = TemplateBuilder.PREDICATE;

    private static IRI local(String name) {
        return TemplateBuilder.local(name);
    }

    @Test
    void templateLevelProperties() throws Exception {
        TemplateBuilder b = new TemplateBuilder();
        IRI provenance = vf.createIRI("https://w3id.org/np/RA7lSq6MuK_TIC6JMSHvLtee3lpLoZDOqLJCLXevnrPoU");
        IRI pubinfoElement = vf.createIRI("https://w3id.org/np/RAukAcWHRDlkqxk7H2XNSegc1WnHI569INvNr-xdptDGI");
        IRI type = vf.createIRI("http://example.com/Type");
        b.add(b.templateNode, RDFS.LABEL, "My template")
                .add(b.templateNode, DCTERMS.DESCRIPTION, "A <b>bold</b> description <script>x()</script>")
                .add(b.templateNode, NTEMPLATE.HAS_TAG, "Examples")
                .add(b.templateNode, NTEMPLATE.HAS_NANOPUB_LABEL_PATTERN, "Thing: ${name}")
                .add(b.templateNode, NTEMPLATE.HAS_DEFAULT_PROVENANCE, provenance)
                .add(b.templateNode, NTEMPLATE.HAS_REQUIRED_PUBINFO_ELEMENT, pubinfoElement)
                .add(b.templateNode, NTEMPLATE.HAS_TARGET_NAMESPACE, vf.createIRI("https://example.com/ns/"))
                .add(b.templateNode, NTEMPLATE.HAS_TARGET_NANOPUB_TYPE, type);
        b.placeholder("name", NTEMPLATE.LITERAL_PLACEHOLDER);
        Template t = b.build();

        assertEquals("My template", t.getLabel());
        // Descriptions are returned as published, not sanitized:
        assertEquals("A <b>bold</b> description <script>x()</script>", t.getDescription());
        assertEquals("Examples", t.getTag());
        assertEquals("Thing: ${name}", t.getNanopubLabelPattern());
        assertEquals(provenance, t.getDefaultProvenance());
        assertEquals(List.of(pubinfoElement), t.getRequiredPubInfoElements());
        assertEquals("https://example.com/ns/", t.getTargetNamespace());
        assertEquals(List.of(type), t.getTargetNanopubTypes());
        assertEquals(NP_URI, t.getId());
        assertFalse(t.isUnlisted());
        assertFalse(t.isTransient());
        assertFalse(t.isProvenanceTemplate());
        assertFalse(t.isPubinfoTemplate());
        assertThrows(UnsupportedOperationException.class, () -> t.getTargetNanopubTypes().add(type));
        assertThrows(UnsupportedOperationException.class, () -> t.getRequiredPubInfoElements().add(type));
    }

    @Test
    void templateKinds() throws Exception {
        assertTrue(new TemplateBuilder(NTEMPLATE.PROVENANCE_TEMPLATE).build().isProvenanceTemplate());
        assertFalse(new TemplateBuilder(NTEMPLATE.PROVENANCE_TEMPLATE).build().isPubinfoTemplate());
        assertTrue(new TemplateBuilder(NTEMPLATE.PUBINFO_TEMPLATE).build().isPubinfoTemplate());
        TemplateBuilder b = new TemplateBuilder(NTEMPLATE.PUBINFO_TEMPLATE);
        b.add(b.templateNode, RDF.TYPE, NTEMPLATE.UNLISTED_TEMPLATE);
        assertTrue(b.build().isUnlisted());
    }

    @Test
    void labelFallsBackToArtifactCode() throws Exception {
        assertEquals("Template RAAbCdEfGh", new TemplateBuilder().build().getLabel());
    }

    @Test
    void templateWithoutStatementsHasEmptyStatementList() throws Exception {
        Template t = new TemplateBuilder().build();
        assertEquals(List.of(), t.getStatementIris());
        assertEquals(List.of(), t.getStatementErrors());
        assertFalse(t.isRequiredField("name"));
        assertFalse(t.isSparqlPlaceholder(local("name")));
        assertNull(t.getFirstOccurrence(local("name")));
    }

    @Test
    void statementsAreOrderedAndUnmodifiable() throws Exception {
        TemplateBuilder b = new TemplateBuilder();
        IRI first = b.statement(SUBJECT, PREDICATE, vf.createLiteral("a"));
        IRI second = b.statement(SUBJECT, PREDICATE, vf.createLiteral("b"));
        IRI third = b.statement(SUBJECT, PREDICATE, vf.createLiteral("c"));
        // An explicit order comes before statements without one, which follow by IRI:
        b.add(third, NTEMPLATE.STATEMENT_ORDER, vf.createLiteral("1", XSD.INTEGER));
        Template t = b.build();

        assertEquals(List.of(third, first, second), t.getStatementIris());
        assertEquals(SUBJECT, t.getSubject(first));
        assertEquals(PREDICATE, t.getPredicate(first));
        assertEquals(vf.createLiteral("a"), t.getObject(first));
        assertNull(t.getStatementIris(first));
        assertThrows(UnsupportedOperationException.class, () -> t.getStatementIris().remove(0));
    }

    @Test
    void statementFlags() throws Exception {
        TemplateBuilder b = new TemplateBuilder();
        IRI plain = b.statement(SUBJECT, PREDICATE, vf.createLiteral("a"));
        IRI flagged = b.statement(SUBJECT, PREDICATE, vf.createLiteral("b"),
                NTEMPLATE.OPTIONAL_STATEMENT, NTEMPLATE.REPEATABLE_STATEMENT, NTEMPLATE.ADVANCED_STATEMENT);
        Template t = b.build();

        assertFalse(t.isOptionalStatement(plain));
        assertFalse(t.isRepeatableStatement(plain));
        assertFalse(t.isAdvancedStatement(plain));
        assertFalse(t.isGroupedStatement(plain));
        assertTrue(t.isOptionalStatement(flagged));
        assertTrue(t.isRepeatableStatement(flagged));
        assertTrue(t.isAdvancedStatement(flagged));
    }

    @Test
    void placeholderTypes() throws Exception {
        TemplateBuilder b = new TemplateBuilder();
        IRI uri = b.placeholder("uri", NTEMPLATE.URI_PLACEHOLDER);
        IRI external = b.placeholder("external", NTEMPLATE.EXTERNAL_URI_PLACEHOLDER);
        IRI trusty = b.placeholder("trusty", NTEMPLATE.TRUSTY_URI_PLACEHOLDER);
        IRI autoEscape = b.placeholder("autoEscape", NTEMPLATE.AUTO_ESCAPE_URI_PLACEHOLDER);
        IRI restricted = b.placeholder("restricted", NTEMPLATE.RESTRICTED_CHOICE_PLACEHOLDER);
        IRI guided = b.placeholder("guided", NTEMPLATE.GUIDED_CHOICE_PLACEHOLDER);
        IRI agent = b.placeholder("agent", NTEMPLATE.AGENT_PLACEHOLDER, NTEMPLATE.EXTERNAL_URI_PLACEHOLDER);
        IRI literal = b.placeholder("literal", NTEMPLATE.LITERAL_PLACEHOLDER);
        IRI longLiteral = b.placeholder("longLiteral", NTEMPLATE.LONG_LITERAL_PLACEHOLDER);
        IRI value = b.placeholder("value", NTEMPLATE.VALUE_PLACEHOLDER);
        IRI sequence = b.placeholder("sequence", NTEMPLATE.SEQUENCE_ELEMENT_PLACEHOLDER);
        IRI root = b.placeholder("root", NTEMPLATE.ROOT_NANOPUB_PLACEHOLDER);
        IRI newUri = b.placeholder("newUri", NTEMPLATE.URI_PLACEHOLDER, NTEMPLATE.NEW_URI_PLACEHOLDER);
        Template t = b.build();

        for (IRI p : List.of(uri, external, trusty, autoEscape, restricted, guided, agent, literal, longLiteral, value, sequence, root, newUri)) {
            assertTrue(t.isPlaceholder(p), p.stringValue());
        }
        assertFalse(t.isPlaceholder(SUBJECT));
        assertFalse(t.isPlaceholder(null));

        for (IRI p : List.of(uri, external, trusty, autoEscape, restricted, guided, agent, newUri)) {
            assertTrue(t.isUriPlaceholder(p), p.stringValue());
            assertFalse(t.isLiteralPlaceholder(p), p.stringValue());
        }
        for (IRI p : List.of(literal, longLiteral)) {
            assertTrue(t.isLiteralPlaceholder(p), p.stringValue());
            assertFalse(t.isUriPlaceholder(p), p.stringValue());
        }
        assertFalse(t.isUriPlaceholder(value));
        assertFalse(t.isLiteralPlaceholder(value));

        assertTrue(t.isValuePlaceholder(value));
        assertTrue(t.isExternalUriPlaceholder(external));
        assertTrue(t.isExternalUriPlaceholder(trusty));
        assertTrue(t.isExternalUriPlaceholder(agent));
        assertFalse(t.isExternalUriPlaceholder(uri));
        assertTrue(t.isTrustyUriPlaceholder(trusty));
        assertFalse(t.isTrustyUriPlaceholder(external));
        assertTrue(t.isAutoEscapePlaceholder(autoEscape));
        assertTrue(t.isRestrictedChoicePlaceholder(restricted));
        assertTrue(t.isGuidedChoicePlaceholder(guided));
        assertTrue(t.isAgentPlaceholder(agent));
        assertTrue(t.isLongLiteralPlaceholder(longLiteral));
        assertFalse(t.isLongLiteralPlaceholder(literal));
        assertTrue(t.isSequenceElementPlaceholder(sequence));
        assertTrue(t.isRootNanopubPlaceholder(root));
        assertTrue(t.isNewUriPlaceholder(newUri));
        assertFalse(t.isNewUriPlaceholder(uri));
    }

    @Test
    void placeholderConstraints() throws Exception {
        TemplateBuilder b = new TemplateBuilder();
        IRI orcid = b.placeholder("orcid", NTEMPLATE.URI_PLACEHOLDER);
        b.add(orcid, RDFS.LABEL, "ORCID identifier")
                .add(orcid, NTEMPLATE.HAS_PREFIX, "https://orcid.org/")
                .add(orcid, NTEMPLATE.HAS_PREFIX_LABEL, "the person with ORCID")
                .add(orcid, NTEMPLATE.HAS_REGEX, "[0-9]{4}-[0-9]{4}-[0-9]{4}-[0-9]{3}[0-9X]");
        IRI count = b.placeholder("count", NTEMPLATE.LITERAL_PLACEHOLDER);
        b.add(count, NTEMPLATE.HAS_DATATYPE, XSD.INTEGER);
        IRI title = b.placeholder("title", NTEMPLATE.LITERAL_PLACEHOLDER);
        b.add(title, NTEMPLATE.HAS_LANGUAGE_TAG, "EN-us");
        IRI topic = b.placeholder("topic", NTEMPLATE.GUIDED_CHOICE_PLACEHOLDER);
        b.add(topic, NTEMPLATE.POSSIBLE_VALUES_FROM_API, "https://example.com/api?search=");
        Template t = b.build();

        assertEquals("ORCID identifier", t.getLabel(orcid));
        assertEquals("https://orcid.org/", t.getPrefix(orcid));
        assertEquals("the person with ORCID", t.getPrefixLabel(orcid));
        assertEquals("[0-9]{4}-[0-9]{4}-[0-9]{4}-[0-9]{3}[0-9X]", t.getRegex(orcid));
        assertNull(t.getDatatype(orcid));
        assertEquals(XSD.INTEGER, t.getDatatype(count));
        assertEquals("en-US", t.getLanguageTag(title));
        assertNull(t.getLabel(count));
        assertNull(t.getRegex(count));
        assertEquals(List.of("https://example.com/api?search="), t.getPossibleValuesApis(topic));
        assertEquals(List.of(), t.getPossibleValuesApis(orcid));
        // The values a template lists for a guided choice come from the template alone:
        assertEquals(List.of(), t.getPossibleValues(topic));
    }

    @Test
    void placeholderInRepetitionIsLookedUpByItsTemplateIri() throws Exception {
        TemplateBuilder b = new TemplateBuilder();
        IRI name = b.placeholder("name", NTEMPLATE.LITERAL_PLACEHOLDER);
        b.add(name, RDFS.LABEL, "the name").add(name, NTEMPLATE.HAS_REGEX, "[a-z]+");
        IRI marked = vf.createIRI("https://example.com/r/~~ARTIFACTCODE~~/thing");
        b.add(marked, RDFS.LABEL, "the thing");
        b.statement(marked, PREDICATE, name);
        Template t = b.build();

        IRI repeated = local("name__3");
        assertTrue(t.isLiteralPlaceholder(repeated));
        assertEquals("the name", t.getLabel(repeated));
        assertEquals("[a-z]+", t.getRegex(repeated));
        assertEquals(name, t.getTemplateIri(repeated));
        assertEquals(name, t.getTemplateIri(name));
        // The marker as it is expanded for the nanopublication being filled in:
        IRI expanded = vf.createIRI("https://example.com/r/~~~ARTIFACTCODE~~~/thing__1");
        assertEquals(marked, t.getTemplateIri(expanded));
        assertEquals("the thing", t.getLabel(expanded));
        assertNull(t.getTemplateIri(null));
    }

    @Test
    void defaultValues() throws Exception {
        TemplateBuilder b = new TemplateBuilder();
        IRI fixed = b.placeholder("fixed", NTEMPLATE.URI_PLACEHOLDER);
        IRI fixedDefault = vf.createIRI("http://example.com/default");
        b.add(fixed, NTEMPLATE.HAS_DEFAULT_VALUE, fixedDefault);
        IRI minted = b.placeholder("minted", NTEMPLATE.URI_PLACEHOLDER);
        b.add(minted, NTEMPLATE.HAS_DEFAULT_VALUE, local("item"));
        IRI text = b.placeholder("text", NTEMPLATE.LITERAL_PLACEHOLDER);
        b.add(text, NTEMPLATE.HAS_DEFAULT_VALUE, "some text");
        IRI none = b.placeholder("none", NTEMPLATE.LITERAL_PLACEHOLDER);
        Template t = b.build();

        assertEquals(fixedDefault, t.getDefault(fixed));
        assertEquals(local("item"), t.getDefault(minted));
        assertEquals(vf.createLiteral("some text"), t.getDefault(text));
        assertNull(t.getDefault(none));
        // In a repetition, a default in the template's own namespace gets a name of its own,
        // while a default from elsewhere and a literal stay as they are:
        assertEquals(fixedDefault, t.getDefault(local("fixed__2")));
        assertEquals(local("item3"), t.getDefault(local("minted__2")));
        assertEquals(vf.createLiteral("some text"), t.getDefault(local("text__2")));
    }

    @Test
    void possibleValuesListedInTheTemplate() throws Exception {
        TemplateBuilder b = new TemplateBuilder();
        IRI choice = b.placeholder("choice", NTEMPLATE.RESTRICTED_CHOICE_PLACEHOLDER);
        IRI a = vf.createIRI("http://example.com/a");
        IRI c = vf.createIRI("http://example.com/c");
        b.add(choice, NTEMPLATE.POSSIBLE_VALUE, a).add(choice, NTEMPLATE.POSSIBLE_VALUE, c);
        b.add(a, RDFS.LABEL, "option A");
        Template t = b.build();

        assertEquals(2, t.getPossibleValues(choice).size());
        assertTrue(t.getPossibleValues(choice).containsAll(List.of(a, c)));
        assertEquals(2, t.getPossibleValues(local("choice__1")).size());
        assertEquals("option A", t.getLabel(a));
        assertNull(t.getLabel(c));
        assertThrows(UnsupportedOperationException.class, () -> t.getPossibleValues(choice).add(a));
    }

    @Test
    void datePlaceholderAcceptsAllPrecisionsByDefault() throws Exception {
        TemplateBuilder b = new TemplateBuilder();
        IRI date = b.placeholder("date", NTEMPLATE.DATE_PLACEHOLDER);
        IRI other = b.placeholder("other", NTEMPLATE.LITERAL_PLACEHOLDER);
        Template t = b.build();

        assertTrue(t.isDatePlaceholder(date));
        assertTrue(t.isPlaceholder(date));
        assertTrue(t.isLiteralPlaceholder(date));
        assertFalse(t.isUriPlaceholder(date));
        assertEquals(DatePrecision.ALL, t.getDatePrecisions(date));
        assertNull(t.getDatatype(date));
        assertFalse(t.isDatePlaceholder(other));
        assertEquals(List.of(), t.getDatePrecisions(other));
    }

    @Test
    void datePlaceholderListsItsPrecisionsCoarsestFirst() throws Exception {
        TemplateBuilder b = new TemplateBuilder();
        IRI date = b.placeholder("date", NTEMPLATE.DATE_PLACEHOLDER);
        b.add(date, NTEMPLATE.HAS_DATATYPE, XSD.DATETIME)
                .add(date, NTEMPLATE.HAS_DATATYPE, XSD.GYEAR)
                // Not a precision a date placeholder can take, so it is ignored:
                .add(date, NTEMPLATE.HAS_DATATYPE, XSD.STRING);
        Template t = b.build();

        assertEquals(List.of(DatePrecision.YEAR, DatePrecision.DATE_TIME), t.getDatePrecisions(date));
        // A date placeholder has no single datatype:
        assertNull(t.getDatatype(date));
        assertEquals(List.of(DatePrecision.YEAR, DatePrecision.DATE_TIME), t.getDatePrecisions(local("date__1")));
    }

    @Test
    void datePrecisionOfDatatype() {
        assertEquals(DatePrecision.YEAR, DatePrecision.of(XSD.GYEAR));
        assertEquals(DatePrecision.YEAR_MONTH, DatePrecision.of(XSD.GYEARMONTH));
        assertEquals(DatePrecision.DATE, DatePrecision.of(XSD.DATE));
        assertEquals(DatePrecision.DATE_TIME, DatePrecision.of(XSD.DATETIME));
        assertNull(DatePrecision.of(XSD.STRING));
        assertNull(DatePrecision.of(null));
        assertEquals(XSD.DATE, DatePrecision.DATE.getDatatype());
        assertEquals("a year, a date or a date and time",
                DatePrecision.describe(List.of(DatePrecision.YEAR, DatePrecision.DATE, DatePrecision.DATE_TIME)));
        assertEquals("a year", DatePrecision.describe(List.of(DatePrecision.YEAR)));
    }

    @Test
    void roleDirectionPin() throws Exception {
        TemplateBuilder b = new TemplateBuilder();
        IRI inverse = vf.createIRI("http://example.com/hasOrganizer");
        IRI regular = b.placeholder("role", NTEMPLATE.URI_PLACEHOLDER);
        b.add(inverse, RDF.TYPE, KPXL.INVERSE_ROLE_PROPERTY);
        b.add(regular, RDF.TYPE, KPXL.REGULAR_ROLE_PROPERTY);
        b.statement(SUBJECT, inverse, vf.createLiteral("x"));
        Template t = b.build();

        assertEquals(KPXL.INVERSE_ROLE_PROPERTY, t.getRoleDirectionPin(inverse));
        assertEquals(KPXL.REGULAR_ROLE_PROPERTY, t.getRoleDirectionPin(regular));
        assertEquals(KPXL.REGULAR_ROLE_PROPERTY, t.getRoleDirectionPin(local("role__2")));
        assertNull(t.getRoleDirectionPin(PREDICATE));
    }

    @Test
    void requiredFields() throws Exception {
        TemplateBuilder b = new TemplateBuilder();
        IRI required = local("required");
        IRI optional = local("optional");
        IRI inOptionalGroup = local("inOptionalGroup");
        IRI inRequiredGroup = local("inRequiredGroup");
        for (IRI p : List.of(required, optional, inOptionalGroup, inRequiredGroup)) {
            b.add(p, RDF.TYPE, NTEMPLATE.LITERAL_PLACEHOLDER);
        }
        b.statement(SUBJECT, PREDICATE, required);
        b.statement(SUBJECT, PREDICATE, optional, NTEMPLATE.OPTIONAL_STATEMENT);
        addGroup(b, "optionalGroup", inOptionalGroup, NTEMPLATE.OPTIONAL_STATEMENT);
        addGroup(b, "requiredGroup", inRequiredGroup);
        Template t = b.build();

        assertTrue(t.isRequiredField("required"));
        assertTrue(t.isRequiredField("required__.1"));
        assertFalse(t.isRequiredField("optional"));
        assertFalse(t.isRequiredField("inOptionalGroup"));
        assertTrue(t.isRequiredField("inRequiredGroup"));
        assertFalse(t.isRequiredField("unknown"));
        assertFalse(t.isRequiredField(null));
    }

    // Adds a grouped statement with one member that has the given placeholder as its object.
    private static void addGroup(TemplateBuilder b, String name, IRI placeholder, IRI... groupTypes) throws Exception {
        IRI group = local(name);
        IRI member = local(name + "-member");
        b.add(b.templateNode, NTEMPLATE.HAS_STATEMENT, group);
        b.add(group, RDF.TYPE, NTEMPLATE.GROUPED_STATEMENT);
        for (IRI type : groupTypes) b.add(group, RDF.TYPE, type);
        b.add(group, NTEMPLATE.HAS_STATEMENT, member);
        b.add(member, RDF.SUBJECT, SUBJECT).add(member, RDF.PREDICATE, PREDICATE).add(member, RDF.OBJECT, placeholder);
    }

    @Test
    void groupedStatementListsItsMembers() throws Exception {
        TemplateBuilder b = new TemplateBuilder();
        IRI placeholder = local("name");
        b.add(placeholder, RDF.TYPE, NTEMPLATE.LITERAL_PLACEHOLDER);
        addGroup(b, "group", placeholder, NTEMPLATE.REPEATABLE_STATEMENT);
        // A member that is also linked from the template node is listed with its group only:
        b.add(b.templateNode, NTEMPLATE.HAS_STATEMENT, local("group-member"));
        Template t = b.build();

        assertEquals(List.of(local("group")), t.getStatementIris());
        assertEquals(List.of(local("group-member")), t.getStatementIris(local("group")));
        assertTrue(t.isGroupedStatement(local("group")));
        assertTrue(t.isRepeatableStatement(local("group")));
        assertEquals(local("group-member"), t.getFirstOccurrence(placeholder));
        assertThrows(UnsupportedOperationException.class, () -> t.getStatementIris(local("group")).clear());
    }

    @Test
    void sparqlPlaceholderIsTheObjectOfASparqlStatement() throws Exception {
        TemplateBuilder b = new TemplateBuilder();
        IRI sparql = local("sparql");
        IRI other = b.placeholder("other", NTEMPLATE.LONG_LITERAL_PLACEHOLDER);
        b.add(sparql, RDF.TYPE, NTEMPLATE.LONG_LITERAL_PLACEHOLDER);
        b.statement(SUBJECT, KPXL_GRLC.SPARQL, sparql);
        IRI grouped = local("groupedSparql");
        b.add(grouped, RDF.TYPE, NTEMPLATE.LONG_LITERAL_PLACEHOLDER);
        IRI group = local("group");
        IRI member = local("group-member");
        b.add(b.templateNode, NTEMPLATE.HAS_STATEMENT, group).add(group, RDF.TYPE, NTEMPLATE.GROUPED_STATEMENT)
                .add(group, NTEMPLATE.HAS_STATEMENT, member)
                .add(member, RDF.SUBJECT, SUBJECT).add(member, RDF.PREDICATE, KPXL_GRLC.SPARQL).add(member, RDF.OBJECT, grouped);
        Template t = b.build();

        assertTrue(t.isSparqlPlaceholder(sparql));
        assertTrue(t.isSparqlPlaceholder(local("sparql__1")));
        assertTrue(t.isSparqlPlaceholder(grouped));
        assertFalse(t.isSparqlPlaceholder(other));
    }

    @Test
    void firstOccurrence() throws Exception {
        TemplateBuilder b = new TemplateBuilder();
        IRI name = local("name");
        b.add(name, RDF.TYPE, NTEMPLATE.LITERAL_PLACEHOLDER);
        IRI first = b.statement(SUBJECT, PREDICATE, vf.createLiteral("x"));
        IRI second = b.statement(SUBJECT, PREDICATE, name);
        b.statement(SUBJECT, RDFS.LABEL, name);
        Template t = b.build();

        assertEquals(first, t.getFirstOccurrence(SUBJECT));
        assertEquals(first, t.getFirstOccurrence(PREDICATE));
        assertEquals(second, t.getFirstOccurrence(name));
        assertEquals(second, t.getFirstOccurrence(local("name__4")));
        assertNull(t.getFirstOccurrence(vf.createIRI("http://example.com/unused")));
    }

    @Test
    void statementDescriptionsAreReturnedAsPublished() throws Exception {
        TemplateBuilder b = new TemplateBuilder();
        IRI st = b.statement(SUBJECT, PREDICATE, vf.createLiteral("x"));
        IRI plain = b.statement(SUBJECT, PREDICATE, vf.createLiteral("y"));
        b.add(st, DCTERMS.DESCRIPTION, "Use <em>this</em> <script>x()</script>");
        b.add(b.templateNode, DCTERMS.DESCRIPTION, "About the template");
        Template t = b.build();

        assertEquals("Use <em>this</em> <script>x()</script>", t.getStatementDescription(st));
        assertNull(t.getStatementDescription(plain));
        // The description of the template itself is not that of a statement:
        assertNull(t.getStatementDescription(b.templateNode));
        assertEquals("About the template", t.getDescription());
    }

    @Test
    void reservedLocalNamesAreReported() throws Exception {
        for (String reserved : NanopubUtils.RESERVED_LOCAL_NAMES) {
            TemplateBuilder b = new TemplateBuilder();
            IRI minted = local(reserved);
            b.add(minted, RDF.TYPE, NTEMPLATE.LOCAL_RESOURCE);
            b.statement(minted, PREDICATE, vf.createLiteral("x"));
            List<String> errors = b.build().getStatementErrors();
            assertEquals(1, errors.size(), reserved);
            assertTrue(errors.get(0).contains("is minted as '" + reserved + "'"), errors.get(0));
        }
    }

    @Test
    void ordinaryLocalNameIsNotReported() throws Exception {
        TemplateBuilder b = new TemplateBuilder();
        IRI minted = local("thing");
        b.add(minted, RDF.TYPE, NTEMPLATE.LOCAL_RESOURCE);
        b.statement(minted, PREDICATE, vf.createLiteral("x"));
        assertEquals(List.of(), b.build().getStatementErrors());
    }

    @Test
    void unsignedTemplateIsRead() throws Exception {
        // A template that is not published yet has a temporary URI without an artifact code:
        String tempUri = "http://purl.org/nanopub/temp/np001/";
        NanopubCreator creator = new NanopubCreator(tempUri);
        creator.addProvenanceStatement(vf.createStatement(creator.getAssertionUri(), RDFS.SEEALSO, creator.getAssertionUri()));
        creator.addPubinfoStatement(vf.createStatement(creator.getNanopubUri(), RDFS.SEEALSO, creator.getNanopubUri()));
        IRI node = vf.createIRI(tempUri + "templateVersion");
        IRI st = vf.createIRI(tempUri + "st1");
        IRI name = vf.createIRI(tempUri + "name");
        creator.addAssertionStatement(node, RDF.TYPE, NTEMPLATE.ASSERTION_TEMPLATE);
        creator.addAssertionStatement(node, NTEMPLATE.HAS_STATEMENT, st);
        creator.addAssertionStatement(st, RDF.SUBJECT, SUBJECT);
        creator.addAssertionStatement(st, RDF.PREDICATE, PREDICATE);
        creator.addAssertionStatement(st, RDF.OBJECT, name);
        creator.addAssertionStatement(name, RDF.TYPE, NTEMPLATE.LITERAL_PLACEHOLDER);
        Nanopub np = creator.finalizeNanopub();

        Template t = new Template(np, NanopubResolver.NONE);
        assertEquals(node.stringValue(), t.getId());
        assertEquals("Template " + tempUri, t.getLabel());
        assertEquals(List.of(st), t.getStatementIris());
        assertTrue(t.isLiteralPlaceholder(name));
    }

    @Test
    void valuesOfTheirOwnPerRepetition() throws Exception {
        TemplateBuilder b = new TemplateBuilder();
        // "thing" occurs in two statements and stands for one value; "keyword" and the local
        // resource "part" occur in one each; "item" is a sequence element.
        IRI thing = b.declare("thing", NTEMPLATE.URI_PLACEHOLDER);
        IRI keyword = b.declare("keyword", NTEMPLATE.LITERAL_PLACEHOLDER);
        IRI part = b.declare("part", NTEMPLATE.LOCAL_RESOURCE);
        IRI item = b.declare("item", NTEMPLATE.SEQUENCE_ELEMENT_PLACEHOLDER);
        b.statement(thing, RDFS.LABEL, vf.createLiteral("a thing"));
        b.statement(thing, DCTERMS.SUBJECT, keyword, NTEMPLATE.OPTIONAL_STATEMENT, NTEMPLATE.REPEATABLE_STATEMENT);
        b.statement(part, DCTERMS.IS_PART_OF, thing, NTEMPLATE.REPEATABLE_STATEMENT);
        b.statement(SUBJECT, item, PREDICATE, NTEMPLATE.REPEATABLE_STATEMENT);
        Template t = b.build();

        assertFalse(t.hasOwnValuePerRepetition(thing));
        assertTrue(t.hasOwnValuePerRepetition(keyword));
        assertTrue(t.hasOwnValuePerRepetition(part));
        assertTrue(t.hasOwnValuePerRepetition(item));
        assertFalse(t.hasOwnValuePerRepetition(DCTERMS.SUBJECT));
        assertFalse(t.hasOwnValuePerRepetition(vf.createLiteral("a thing")));

        assertEquals(keyword, t.getRepetitionKey(keyword, 0));
        assertEquals(vf.createIRI(keyword + "__2"), t.getRepetitionKey(keyword, 2));
        assertEquals(thing, t.getRepetitionKey(thing, 2));
        assertEquals(DCTERMS.SUBJECT, t.getRepetitionKey(DCTERMS.SUBJECT, 1));
        assertThrows(IllegalArgumentException.class, () -> t.getRepetitionKey(keyword, -1));

        // The rule is worked out again after a template is serialized, as Nanodash does with
        // the templates of a form.
        java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
        try (java.io.ObjectOutputStream out = new java.io.ObjectOutputStream(bytes)) {
            out.writeObject(t);
        }
        Template copy;
        try (java.io.ObjectInputStream in = new java.io.ObjectInputStream(new java.io.ByteArrayInputStream(bytes.toByteArray()))) {
            copy = (Template) in.readObject();
        }
        assertTrue(copy.hasOwnValuePerRepetition(keyword));
        assertFalse(copy.hasOwnValuePerRepetition(thing));
    }

    @Test
    void nullArgumentsAreRejected() throws Exception {
        Nanopub np = new TemplateBuilder().creator.finalizeNanopub();
        assertThrows(NullPointerException.class, () -> new Template((Nanopub) null));
        assertThrows(NullPointerException.class, () -> new Template(np, null));
        assertThrows(MalformedTemplateException.class, () -> new Template((String) null));
        assertThrows(NullPointerException.class, () -> new Template(NP_URI, null));
    }

}
