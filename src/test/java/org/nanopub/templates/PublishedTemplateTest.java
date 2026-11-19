package org.nanopub.templates;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.PROV;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.junit.jupiter.api.Test;
import org.nanopub.Nanopub;
import org.nanopub.NanopubCreator;
import org.nanopub.NanopubImpl;
import org.nanopub.testsuite.NanopubTestSuite;
import org.nanopub.vocabulary.KPXL;
import org.nanopub.vocabulary.NTEMPLATE;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link Template} on published template nanopublications, and for the retrieval of
 * the nanopublications a template takes possible values from.
 */
class PublishedTemplateTest {

    private static final ValueFactory vf = SimpleValueFactory.getInstance();

    // "Declare citations with CiTO #2": legacy shape, with a restricted choice whose values
    // come from another nanopublication.
    private static final String CITO_TEMPLATE = "RAiTfCYCViuGzC_g3oe1xqtAwv4_I0SeOVwG9wIELTrD4";
    private static final String CITO_VALUES = "RAZt5kzfoJg2m4dMRdMm2SP6JeUDD_GMzSq9xyRPMgP5k";
    // "Defining an assertion template (new version)": embedded identity, space-governed,
    // with grouped statements and choices listed in the template.
    private static final String META_TEMPLATE = "RAgeON_vGgJCt5E4DPMi0eOPaGwNv9tbfFTRZtBwdQL0w";

    private static final String NP_PREFIX = "https://w3id.org/np/";

    private static Nanopub loadResource(String artifactCode) throws Exception {
        return new NanopubImpl(new File(PublishedTemplateTest.class.getResource("/templates/" + artifactCode + ".trig").toURI()));
    }

    private static Nanopub loadFromTestSuite(String artifactCode) throws Exception {
        return new NanopubImpl(NanopubTestSuite.getLatest().getByArtifactCode(artifactCode).getFirst().toFile());
    }

    /**
     * A resolver over nanopublications held in memory, which counts how often each one is
     * asked for and can be told to fail.
     */
    private static class TestResolver implements NanopubResolver {

        final Map<String, Nanopub> nanopubs = new HashMap<>();
        final List<String> requests = new ArrayList<>();
        boolean failing = false;

        TestResolver(Nanopub... nps) {
            for (Nanopub np : nps) nanopubs.put(np.getUri().stringValue(), np);
        }

        @Override
        public Nanopub getNanopub(String uriOrArtifactCode) {
            requests.add(uriOrArtifactCode);
            if (failing) throw new IllegalStateException("no connection");
            return nanopubs.get(uriOrArtifactCode);
        }

    }

    private static IRI citoIri(String localName) {
        return vf.createIRI(NP_PREFIX + CITO_TEMPLATE + "/" + localName);
    }

    private static IRI metaIri(String localName) {
        return vf.createIRI(NP_PREFIX + META_TEMPLATE + "/" + localName);
    }

    @Test
    void assertionTemplateFromTestSuite() throws Exception {
        String uri = NP_PREFIX + "RAEzjboN_ipuwsFnvhqAl9Fy7fZl34cGZ1QhJ-7cbbGGM";
        Template t = new Template(loadFromTestSuite("RAEzjboN_ipuwsFnvhqAl9Fy7fZl34cGZ1QhJ-7cbbGGM"), NanopubResolver.NONE);
        IRI st1 = vf.createIRI(uri + "/st1");
        IRI st2 = vf.createIRI(uri + "/st2");
        IRI paper = vf.createIRI(uri + "/paper");
        IRI comment = vf.createIRI(uri + "/comment");

        assertEquals(uri, t.getId());
        assertEquals("Announcing a paper that I have read", t.getLabel());
        assertFalse(t.isProvenanceTemplate());
        assertEquals(List.of(st1, st2), t.getStatementIris());
        assertEquals(NTEMPLATE.CREATOR_PLACEHOLDER, t.getSubject(st1));
        assertEquals(paper, t.getObject(st1));
        assertFalse(t.isOptionalStatement(st1));
        assertTrue(t.isOptionalStatement(st2));
        assertEquals(RDFS.COMMENT, t.getPredicate(st2));
        assertTrue(t.isUriPlaceholder(paper));
        assertEquals("https://doi.org/", t.getPrefix(paper));
        assertEquals("the paper with DOI", t.getPrefixLabel(paper));
        assertEquals("10.(\\d)+/(\\S)+", t.getRegex(paper));
        assertTrue(t.isLiteralPlaceholder(comment));
        assertEquals("comment text", t.getLabel(comment));
        assertEquals("has my comment", t.getLabel(RDFS.COMMENT));
        assertTrue(t.isRequiredField("paper"));
        assertFalse(t.isRequiredField("comment"));
        assertEquals(List.of(), t.getStatementErrors());
    }

    @Test
    void provenanceTemplateFromTestSuite() throws Exception {
        String uri = NP_PREFIX + "RANZtIDyP4k0Bh2gupF2KWEvGMIjGzp_16HZVgi9jWx-U";
        Template t = new Template(loadFromTestSuite("RANZtIDyP4k0Bh2gupF2KWEvGMIjGzp_16HZVgi9jWx-U"), NanopubResolver.NONE);
        IRI st1 = vf.createIRI(uri + "/st1");

        assertTrue(t.isProvenanceTemplate());
        assertFalse(t.isPubinfoTemplate());
        assertEquals("Attributed to somebody else", t.getLabel());
        assertEquals(List.of(st1), t.getStatementIris());
        assertEquals(NTEMPLATE.ASSERTION_PLACEHOLDER, t.getSubject(st1));
        assertEquals(PROV.WAS_ATTRIBUTED_TO, t.getPredicate(st1));
        assertEquals("https://orcid.org/", t.getPrefix(vf.createIRI(uri + "/somebodyElse")));
    }

    @Test
    void embeddedIdentityTemplateWithGroups() throws Exception {
        Template t = new Template(loadResource(META_TEMPLATE), NanopubResolver.NONE);

        assertEquals(metaIri("templateVersion").stringValue(), t.getId());
        assertTrue(t.hasId(NP_PREFIX + META_TEMPLATE));
        assertEquals(metaIri("template"), t.getTemplateKindIri());
        assertEquals(vf.createIRI("https://w3id.org/spaces/knowledgepixels/nanodash"), t.getGoverningSpace());
        assertEquals("Defining an assertion template (new version)", t.getLabel());
        assertEquals("Templates", t.getTag());
        assertEquals("${tlabel}", t.getNanopubLabelPattern());
        assertTrue(Template.hasFullTemplateDefinition(t.getNanopub()));
        assertEquals(List.of(), t.getStatementErrors());

        // The members of a group are also linked from the template node, but listed only
        // with their group:
        IRI group = metaIri("stA021");
        List<IRI> members = List.of(metaIri("stA021a"), metaIri("stA021b"));
        assertTrue(t.isGroupedStatement(group));
        assertTrue(t.isOptionalStatement(group));
        assertEquals(members, t.getStatementIris(group));
        assertTrue(t.getStatementIris().contains(group));
        assertFalse(t.getStatementIris().contains(members.get(0)));
        assertEquals(DCTERMS.IS_VERSION_OF, t.getPredicate(members.get(0)));
        assertEquals(KPXL.GOVERNED_BY, t.getPredicate(members.get(1)));
        assertEquals(metaIri("newtemplate"), t.getSubject(members.get(0)));

        IRI unlisted = null;
        for (IRI st : t.getStatementIris()) {
            if (t.getObject(st) instanceof IRI obj && t.getPossibleValues(obj).contains(NTEMPLATE.UNLISTED_TEMPLATE)) unlisted = obj;
        }
        assertNotNull(unlisted);
        assertTrue(t.isRestrictedChoicePlaceholder(unlisted));
        assertEquals(List.<Value>of(NTEMPLATE.UNLISTED_TEMPLATE), t.getPossibleValues(unlisted));
        assertEquals("Agent placeholder", t.getLabel(NTEMPLATE.AGENT_PLACEHOLDER));
    }

    @Test
    void readingATemplateRetrievesNothing() throws Exception {
        TestResolver resolver = new TestResolver(loadResource(CITO_VALUES));
        Template t = new Template(loadResource(CITO_TEMPLATE), resolver);

        assertEquals("Declare citations with CiTO #2", t.getLabel());
        assertEquals(2, t.getStatementIris().size());
        assertTrue(t.isRestrictedChoicePlaceholder(citoIri("cites")));
        assertEquals("select the citation type", t.getLabel(citoIri("cites")));
        assertTrue(t.isRepeatableStatement(citoIri("st02")));
        assertEquals(List.of(), t.getStatementErrors());
        assertEquals(List.of(), resolver.requests);
    }

    @Test
    void possibleValuesAreRetrievedOnce() throws Exception {
        TestResolver resolver = new TestResolver(loadResource(CITO_VALUES));
        Template t = new Template(loadResource(CITO_TEMPLATE), resolver);
        IRI agreesWith = vf.createIRI("http://purl.org/spar/cito/agreesWith");

        List<Value> values = t.getPossibleValues(citoIri("cites"));
        assertEquals(43, values.size());
        assertTrue(values.contains(agreesWith));
        assertEquals(values, t.getPossibleValues(citoIri("cites")));
        assertEquals(values, t.getPossibleValues(citoIri("cites__2")));
        assertEquals(List.of(NP_PREFIX + CITO_VALUES), resolver.requests);

        assertTrue(t.getLabel(agreesWith).startsWith("agrees with - "));
        assertEquals(1, resolver.requests.size());
        // Other placeholders have no possible values:
        assertEquals(List.of(), t.getPossibleValues(citoIri("work")));
    }

    @Test
    void labelOfAPossibleValueTriggersRetrieval() throws Exception {
        TestResolver resolver = new TestResolver(loadResource(CITO_VALUES));
        Template t = new Template(loadResource(CITO_TEMPLATE), resolver);

        // A label the template gives itself needs no retrieval:
        assertEquals("is a", t.getLabel(vf.createIRI("http://www.w3.org/1999/02/22-rdf-syntax-ns#type")));
        assertEquals(List.of(), resolver.requests);

        assertTrue(t.getLabel(vf.createIRI("http://purl.org/spar/cito/cites")).startsWith("cites - "));
        assertEquals(1, resolver.requests.size());
        assertNull(t.getLabel(vf.createIRI("http://example.com/unknown")));
        assertEquals(1, resolver.requests.size());
        assertEquals(43, t.getPossibleValues(citoIri("cites")).size());
        assertEquals(1, resolver.requests.size());
    }

    @Test
    void failedRetrievalIsRetriedForValuesButNotForLabels() throws Exception {
        TestResolver resolver = new TestResolver(loadResource(CITO_VALUES));
        resolver.failing = true;
        Template t = new Template(loadResource(CITO_TEMPLATE), resolver);
        IRI cites = vf.createIRI("http://purl.org/spar/cito/cites");

        assertEquals(List.of(), t.getPossibleValues(citoIri("cites")));
        assertEquals(1, resolver.requests.size());
        // Asking for labels does not try again...
        assertNull(t.getLabel(cites));
        assertNull(t.getLabel(cites));
        assertEquals(1, resolver.requests.size());
        // ... but asking for the values does:
        assertEquals(List.of(), t.getPossibleValues(citoIri("cites")));
        assertEquals(2, resolver.requests.size());

        resolver.failing = false;
        assertEquals(43, t.getPossibleValues(citoIri("cites")).size());
        assertEquals(3, resolver.requests.size());
        assertTrue(t.getLabel(cites).startsWith("cites - "));
        assertEquals(3, resolver.requests.size());
    }

    @Test
    void unretrievableValuesNanopubLeavesTheListEmpty() throws Exception {
        Template t = new Template(loadResource(CITO_TEMPLATE), NanopubResolver.NONE);
        assertEquals(List.of(), t.getPossibleValues(citoIri("cites")));
        assertNull(t.getLabel(vf.createIRI("http://purl.org/spar/cito/cites")));
        assertEquals("select the citation type", t.getLabel(citoIri("cites")));
    }

    @Test
    void labelFromTheTemplateWinsOverOneFromAValuesNanopub() throws Exception {
        String valuesUri = "https://w3id.org/np/RAZyXwVuTsRqPoNmLkJiHgFeDcBa9876543210_-ZyXwV";
        IRI both = vf.createIRI("http://example.com/labelled-by-both");
        IRI valuesOnly = vf.createIRI("http://example.com/labelled-by-values-nanopub");
        NanopubCreator values = newCreator(valuesUri);
        values.addAssertionStatement(both, RDFS.LABEL, vf.createLiteral("label from the values nanopub"));
        values.addAssertionStatement(valuesOnly, RDFS.LABEL, vf.createLiteral("only labelled there"));

        String templateUri = "https://w3id.org/np/RAAbCdEfGhIjKlMnOpQrStUvWxYz0123456789-_AbCdE";
        IRI choice = vf.createIRI(templateUri + "/choice");
        IRI st = vf.createIRI(templateUri + "/st1");
        NanopubCreator template = newCreator(templateUri);
        template.addAssertionStatement(template.getAssertionUri(), RDF.TYPE, NTEMPLATE.ASSERTION_TEMPLATE);
        template.addAssertionStatement(template.getAssertionUri(), NTEMPLATE.HAS_STATEMENT, st);
        template.addAssertionStatement(st, RDF.SUBJECT, vf.createIRI("http://example.com/subject"));
        template.addAssertionStatement(st, RDF.PREDICATE, vf.createIRI("http://example.com/predicate"));
        template.addAssertionStatement(st, RDF.OBJECT, choice);
        template.addAssertionStatement(choice, RDF.TYPE, NTEMPLATE.RESTRICTED_CHOICE_PLACEHOLDER);
        template.addAssertionStatement(choice, NTEMPLATE.POSSIBLE_VALUE, both);
        template.addAssertionStatement(choice, NTEMPLATE.POSSIBLE_VALUES_FROM, vf.createIRI(valuesUri));
        template.addAssertionStatement(both, RDFS.LABEL, vf.createLiteral("label from the template"));

        Template t = new Template(template.finalizeNanopub(), new TestResolver(values.finalizeNanopub()));
        // Values the template lists itself come first, followed by those of the nanopub:
        List<Value> possibleValues = t.getPossibleValues(choice);
        assertEquals(both, possibleValues.get(0));
        assertEquals(3, possibleValues.size());
        assertTrue(possibleValues.contains(valuesOnly));
        assertEquals("label from the template", t.getLabel(both));
        assertEquals("only labelled there", t.getLabel(valuesOnly));
    }

    private static NanopubCreator newCreator(String uri) throws Exception {
        NanopubCreator creator = new NanopubCreator(uri);
        creator.addProvenanceStatement(vf.createStatement(creator.getAssertionUri(), RDFS.SEEALSO, creator.getAssertionUri()));
        creator.addPubinfoStatement(vf.createStatement(creator.getNanopubUri(), RDFS.SEEALSO, creator.getNanopubUri()));
        return creator;
    }

    @Test
    void templateIsRetrievedById() throws Exception {
        TestResolver resolver = new TestResolver(loadResource(CITO_TEMPLATE), loadResource(CITO_VALUES));
        Template t = new Template(NP_PREFIX + CITO_TEMPLATE, resolver);

        assertEquals(NP_PREFIX + CITO_TEMPLATE, t.getId());
        assertEquals(List.of(NP_PREFIX + CITO_TEMPLATE), resolver.requests);
        assertEquals(43, t.getPossibleValues(citoIri("cites")).size());
    }

    @Test
    void unretrievableTemplateIsMalformed() {
        MalformedTemplateException ex = assertThrows(MalformedTemplateException.class,
                () -> new Template(NP_PREFIX + CITO_TEMPLATE, NanopubResolver.NONE));
        assertEquals("nanopublication could not be retrieved", ex.getMessage());
    }

    @Test
    void nanopubThatIsNoTemplateIsMalformed() throws Exception {
        // The values nanopub has neither a template node nor a SHACL node shape:
        MalformedTemplateException ex = assertThrows(MalformedTemplateException.class,
                () -> new Template(loadResource(CITO_VALUES), NanopubResolver.NONE));
        assertEquals("Base node shape not found", ex.getMessage());
        assertFalse(Template.hasFullTemplateDefinition(loadResource(CITO_VALUES)));
    }

    @Test
    void templateSurvivesSerialization() throws Exception {
        Template t = new Template(loadResource(META_TEMPLATE));
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(t);
        }
        Template copy;
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            copy = (Template) in.readObject();
        }
        assertEquals(t.getId(), copy.getId());
        assertEquals(t.getStatementIris(), copy.getStatementIris());
        assertEquals(t.getStatementIris(metaIri("stA021")), copy.getStatementIris(metaIri("stA021")));
        assertEquals(t.getLabel(metaIri("tlabel")), copy.getLabel(metaIri("tlabel")));
    }

}
