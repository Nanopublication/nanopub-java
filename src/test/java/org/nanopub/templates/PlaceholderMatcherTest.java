package org.nanopub.templates;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.junit.jupiter.api.Test;
import org.nanopub.vocabulary.KPXL_GRLC;
import org.nanopub.vocabulary.NTEMPLATE;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.nanopub.templates.TemplateBuilder.PREDICATE;
import static org.nanopub.templates.TemplateBuilder.SUBJECT;
import static org.nanopub.templates.TemplateBuilder.local;

/**
 * Tests for {@link PlaceholderMatcher}, for each kind of position in both directions: checking
 * an RDF value, and turning a raw value into an RDF value and back.
 */
class PlaceholderMatcherTest {

    private static final ValueFactory vf = SimpleValueFactory.getInstance();

    // The nanopublication that is made from the template in these tests.
    private static final String NS = "http://purl.org/nanopub/temp/np001/";
    private static final IRI CREATOR = vf.createIRI("https://orcid.org/0000-0000-0000-0001");

    private static IRI iri(String s) {
        return vf.createIRI(s);
    }

    private static BindingContext context(Template template) {
        return BindingContext.forNewNanopub(template, NS, CREATOR);
    }

    private static List<Violation.Kind> kinds(MatchResult result) {
        return result.violations().stream().map(Violation::kind).toList();
    }

    private static void assertValid(PlaceholderMatcher matcher, Value value, BindingContext context) {
        MatchResult result = matcher.match(value, context);
        assertTrue(result.isValid(), () -> value + ": " + result.violations());
    }

    // The value cannot be bound, for the given reason.
    private static void assertBlocked(Violation.Kind kind, PlaceholderMatcher matcher, Value value, BindingContext context) {
        MatchResult result = matcher.match(value, context);
        assertFalse(result.isBindable(), () -> value + " should not be bindable");
        assertTrue(result.violations().stream().anyMatch(v -> v.kind() == kind && v.blocking()), () -> value + ": " + result.violations());
    }

    // The value can be bound, but breaks the given constraint.
    private static void assertReported(Violation.Kind kind, PlaceholderMatcher matcher, Value value, BindingContext context) {
        MatchResult result = matcher.match(value, context);
        assertTrue(result.isBindable(), () -> value + ": " + result.violations());
        assertEquals(List.of(kind), kinds(result), () -> value + ": " + result.violations());
    }

    // The raw value stands for the RDF value, and the RDF value for the raw value.
    private static void assertRoundTrip(PlaceholderMatcher matcher, String raw, Value value, BindingContext context) {
        assertEquals(value, matcher.toValue(new RawValue(raw), context));
        assertEquals(new RawValue(raw), matcher.toRaw(value, context));
    }

    // ---------- fixed values ----------

    @Test
    void fixedIriStandsForItself() throws Exception {
        TemplateBuilder b = new TemplateBuilder();
        b.statement(SUBJECT, PREDICATE, vf.createLiteral("x"));
        Template t = b.build();
        BindingContext c = context(t);
        PlaceholderMatcher m = PlaceholderMatcher.of(t, SUBJECT);

        assertFalse(m.isPlaceholder());
        assertEquals(SUBJECT, m.getKey());
        assertValid(m, SUBJECT, c);
        assertBlocked(Violation.Kind.DIFFERENT_VALUE, m, iri("http://example.com/other"), c);
        assertBlocked(Violation.Kind.WRONG_KIND, m, vf.createLiteral("http://example.com/subject"), c);
        assertEquals(SUBJECT, m.resolve(c));
        assertEquals(SUBJECT, m.toValue(new RawValue("ignored"), c));
    }

    @Test
    void nanopubAndAssertionStandForTheNanopublication() throws Exception {
        TemplateBuilder b = new TemplateBuilder(NTEMPLATE.PROVENANCE_TEMPLATE);
        b.statement(NTEMPLATE.ASSERTION_PLACEHOLDER, PREDICATE, NTEMPLATE.NANOPUB_PLACEHOLDER);
        Template t = b.build();
        BindingContext c = context(t);

        PlaceholderMatcher assertion = PlaceholderMatcher.of(t, NTEMPLATE.ASSERTION_PLACEHOLDER);
        assertEquals(iri(NS + "assertion"), assertion.resolve(c));
        assertValid(assertion, iri(NS + "assertion"), c);
        assertBlocked(Violation.Kind.DIFFERENT_VALUE, assertion, iri(NS), c);

        PlaceholderMatcher nanopub = PlaceholderMatcher.of(t, NTEMPLATE.NANOPUB_PLACEHOLDER);
        assertEquals(iri(NS), nanopub.resolve(c));
        assertValid(nanopub, iri(NS), c);
        assertBlocked(Violation.Kind.DIFFERENT_VALUE, nanopub, iri("https://w3id.org/np/RAother"), c);
    }

    @Test
    void localResourceIsMintedInTheNanopublication() throws Exception {
        TemplateBuilder b = new TemplateBuilder();
        IRI thing = local("thing");
        b.statement(thing, PREDICATE, vf.createLiteral("x"));
        Template t = b.build();
        BindingContext c = context(t);
        PlaceholderMatcher m = PlaceholderMatcher.of(t, thing);

        assertEquals(iri(NS + "thing"), m.resolve(c));
        assertValid(m, iri(NS + "thing"), c);
        // The IRI as it is in the template is not what a nanopublication made from it has:
        assertBlocked(Violation.Kind.DIFFERENT_VALUE, m, thing, c);
        assertBlocked(Violation.Kind.DIFFERENT_VALUE, m, iri(NS + "other"), c);
        // In a repetition, the local resource has a name of its own:
        PlaceholderMatcher repeated = PlaceholderMatcher.of(t, local("thing__2"));
        assertEquals(iri(NS + "thing__2"), repeated.resolve(c));
        assertValid(repeated, iri(NS + "thing__2"), c);
    }

    @Test
    void introducedLocalResourceMayComeFromAnEarlierVersion() throws Exception {
        TemplateBuilder b = new TemplateBuilder();
        IRI thing = local("thing");
        b.add(thing, RDF.TYPE, NTEMPLATE.INTRODUCED_RESOURCE).add(thing, RDF.TYPE, NTEMPLATE.LOCAL_RESOURCE);
        IRI embedded = local("part");
        b.add(embedded, RDF.TYPE, NTEMPLATE.EMBEDDED_RESOURCE).add(embedded, RDF.TYPE, NTEMPLATE.LOCAL_RESOURCE);
        b.statement(thing, PREDICATE, embedded);
        Template t = b.build();
        BindingContext c = context(t);
        IRI earlier = iri("https://w3id.org/np/RAZyXwVuTsRqPoNmLkJiHgFeDcBa9876543210_-ZyXwV/thing");

        PlaceholderMatcher introduced = PlaceholderMatcher.of(t, thing);
        assertValid(introduced, iri(NS + "thing"), c);
        assertValid(introduced, earlier, c);
        assertBlocked(Violation.Kind.DIFFERENT_VALUE, introduced, iri("https://w3id.org/np/RAZyXwVuTsRqPoNmLkJiHgFeDcBa9876543210_-ZyXwV/other"), c);
        // An embedded resource belongs to its version, so it is always minted anew:
        PlaceholderMatcher embeddedMatcher = PlaceholderMatcher.of(t, embedded);
        assertBlocked(Violation.Kind.DIFFERENT_VALUE, embeddedMatcher, iri("https://w3id.org/np/RAZyXwVuTsRqPoNmLkJiHgFeDcBa9876543210_-ZyXwV/part"), c);
    }

    @Test
    void artifactCodeMarkerStandsForAnyArtifactCode() throws Exception {
        TemplateBuilder b = new TemplateBuilder();
        IRI marked = iri("https://example.com/r/~~ARTIFACTCODE~~/thing");
        b.statement(marked, PREDICATE, vf.createLiteral("x"));
        Template t = b.build();
        BindingContext c = context(t);
        PlaceholderMatcher m = PlaceholderMatcher.of(t, marked);

        // As written into a nanopublication that is not signed yet:
        IRI unsigned = iri("https://example.com/r/~~~ARTIFACTCODE~~~/thing");
        assertEquals(unsigned, m.resolve(c));
        assertValid(m, unsigned, c);
        assertValid(m, iri("https://example.com/r/RAZyXwVuTsRqPoNmLkJiHgFeDcBa9876543210_-ZyXwV/thing"), c);
        assertBlocked(Violation.Kind.DIFFERENT_VALUE, m, iri("https://example.com/r/RAZyXw/other"), c);
        assertBlocked(Violation.Kind.DIFFERENT_VALUE, m, iri("https://example.com/r/two/parts/thing"), c);
        // The form the marker has in a repetition key is understood as well:
        assertValid(PlaceholderMatcher.of(t, unsigned), unsigned, c);
    }

    @Test
    void fixedLiteralStandsForItself() throws Exception {
        TemplateBuilder b = new TemplateBuilder();
        b.statement(SUBJECT, PREDICATE, vf.createLiteral("fixed", "en"));
        Template t = b.build();
        BindingContext c = context(t);
        PlaceholderMatcher m = PlaceholderMatcher.of(t, vf.createLiteral("fixed", "en"));

        assertNull(m.getKey());
        assertValid(m, vf.createLiteral("fixed", "en"), c);
        assertBlocked(Violation.Kind.DIFFERENT_VALUE, m, vf.createLiteral("fixed"), c);
        assertBlocked(Violation.Kind.WRONG_KIND, m, SUBJECT, c);
        assertEquals(vf.createLiteral("fixed", "en"), m.resolve(c));
    }

    @Test
    void undefinedPositionMatchesNothing() throws Exception {
        Template t = new TemplateBuilder().build();
        BindingContext c = context(t);
        PlaceholderMatcher m = PlaceholderMatcher.of(t, null);

        assertBlocked(Violation.Kind.NO_VALUE_DEFINED, m, SUBJECT, c);
        assertNull(m.resolve(c));
        assertNull(m.getKey());
    }

    // ---------- creator and sequence element ----------

    @Test
    void creatorStandsForACreatorOfTheNanopublication() throws Exception {
        TemplateBuilder b = new TemplateBuilder();
        b.statement(NTEMPLATE.CREATOR_PLACEHOLDER, PREDICATE, vf.createLiteral("x"));
        Template t = b.build();
        BindingContext c = context(t);
        PlaceholderMatcher m = PlaceholderMatcher.of(t, NTEMPLATE.CREATOR_PLACEHOLDER);

        assertTrue(m.isPlaceholder());
        assertEquals(CREATOR, m.resolve(c));
        assertEquals(CREATOR, m.toValue(new RawValue(""), c));
        assertValid(m, CREATOR, c);
        assertBlocked(Violation.Kind.NOT_A_CREATOR, m, iri("https://orcid.org/0000-0000-0000-0002"), c);
        assertBlocked(Violation.Kind.WRONG_KIND, m, vf.createLiteral("me"), c);

        // Where the creators are not known, any IRI can be the creator, but only one:
        BindingContext unknown = BindingContext.forNewNanopub(t, NS, null);
        assertNull(m.resolve(unknown));
        assertTrue(unknown.bind(NTEMPLATE.CREATOR_PLACEHOLDER, CREATOR).isValid());
        assertBlocked(Violation.Kind.ALREADY_BOUND, m, iri("https://orcid.org/0000-0000-0000-0002"), unknown);
    }

    @Test
    void sequenceElementFollowsTheRepetition() throws Exception {
        TemplateBuilder b = new TemplateBuilder();
        IRI element = local("element");
        b.add(element, RDF.TYPE, NTEMPLATE.SEQUENCE_ELEMENT_PLACEHOLDER);
        b.statement(SUBJECT, element, vf.createLiteral("x"));
        Template t = b.build();
        BindingContext c = context(t);

        PlaceholderMatcher first = PlaceholderMatcher.of(t, element);
        assertEquals(iri(RDF.NAMESPACE + "_1"), first.resolve(c));
        assertValid(first, iri(RDF.NAMESPACE + "_1"), c);
        assertBlocked(Violation.Kind.DIFFERENT_VALUE, first, iri(RDF.NAMESPACE + "_2"), c);
        assertBlocked(Violation.Kind.WRONG_KIND, first, vf.createLiteral("1"), c);

        PlaceholderMatcher third = PlaceholderMatcher.of(t, local("element__2"));
        assertEquals(iri(RDF.NAMESPACE + "_3"), third.resolve(c));
        assertValid(third, iri(RDF.NAMESPACE + "_3"), c);
    }

    // ---------- placeholders that take an IRI ----------

    @Test
    void uriPlaceholderTakesAnIriInFull() throws Exception {
        TemplateBuilder b = new TemplateBuilder();
        IRI p = b.placeholder("page", NTEMPLATE.URI_PLACEHOLDER);
        Template t = b.build();
        BindingContext c = context(t);
        PlaceholderMatcher m = PlaceholderMatcher.of(t, p);

        assertTrue(m.isPlaceholder());
        assertNull(m.resolve(c));
        assertNull(m.toValue(new RawValue(""), c));
        assertValid(m, iri("https://example.com/page"), c);
        assertRoundTrip(m, "https://example.com/page", iri("https://example.com/page"), c);
        assertValid(m, iri("did:example:123"), c);
        assertBlocked(Violation.Kind.WRONG_KIND, m, vf.createLiteral("https://example.com/page"), c);
        assertBlocked(Violation.Kind.SCHEME_NOT_ALLOWED, m, iri("urn:isbn:0451450523"), c);
        assertThrows(IllegalArgumentException.class, () -> m.toValue(new RawValue("not an iri at all"), c));
    }

    @Test
    void plainNameIsMintedInTheNanopublication() throws Exception {
        TemplateBuilder b = new TemplateBuilder();
        IRI p = b.placeholder("item", NTEMPLATE.URI_PLACEHOLDER);
        Template t = b.build();
        BindingContext c = context(t);
        PlaceholderMatcher m = PlaceholderMatcher.of(t, p);

        assertRoundTrip(m, "my-item", iri(NS + "my-item"), c);
        assertValid(m, iri(NS + "my-item"), c);
        // Minting a name the nanopublication uses for one of its own parts is reported:
        assertReported(Violation.Kind.RESERVED_NAME, m, iri(NS + "assertion"), c);
        assertReported(Violation.Kind.RESERVED_NAME, m, iri(NS + "pubinfo"), c);
    }

    @Test
    void localResourcePlaceholderMintsItsName() throws Exception {
        TemplateBuilder b = new TemplateBuilder();
        IRI p = b.placeholder("id", NTEMPLATE.URI_PLACEHOLDER, NTEMPLATE.LOCAL_RESOURCE);
        b.add(p, NTEMPLATE.HAS_REGEX, "[a-z-]+");
        Template t = b.build();
        BindingContext c = context(t);
        PlaceholderMatcher m = PlaceholderMatcher.of(t, p);

        assertRoundTrip(m, "my-thing", iri(NS + "my-thing"), c);
        assertValid(m, iri(NS + "my-thing"), c);
        // The pattern is for the name, not for the IRI it is minted into:
        assertBlocked(Violation.Kind.REGEX, m, iri(NS + "My_Thing"), c);
        // The value of an earlier version is an IRI in full, which the pattern does not fit:
        assertBlocked(Violation.Kind.REGEX, m, iri("https://w3id.org/np/RAZyXwVuTsRqPoNmLkJiHgFeDcBa9876543210_-ZyXwV/my-thing"), c);
    }

    @Test
    void prefixIsPutInFrontOfTheValue() throws Exception {
        TemplateBuilder b = new TemplateBuilder();
        IRI p = b.placeholder("paper", NTEMPLATE.URI_PLACEHOLDER);
        b.add(p, NTEMPLATE.HAS_PREFIX, "https://doi.org/").add(p, NTEMPLATE.HAS_REGEX, "10\\.[0-9]+/\\S+");
        Template t = b.build();
        BindingContext c = context(t);
        PlaceholderMatcher m = PlaceholderMatcher.of(t, p);

        IRI doi = iri("https://doi.org/10.1234/abc");
        assertValid(m, doi, c);
        assertRoundTrip(m, "10.1234/abc", doi, c);
        // The IRI in full is taken as well:
        assertEquals(doi, m.toValue(new RawValue("https://doi.org/10.1234/abc"), c));
        assertBlocked(Violation.Kind.REGEX, m, iri("https://doi.org/11.1234/abc"), c);
        // An IRI without the prefix is taken in full, so the pattern for what follows the
        // prefix does not fit it:
        assertBlocked(Violation.Kind.REGEX, m, iri("https://example.com/10.1234/abc"), c);
    }

    @Test
    void whatFollowsAPrefixHasNoColonOrHash() throws Exception {
        TemplateBuilder b = new TemplateBuilder();
        IRI p = b.placeholder("paper", NTEMPLATE.URI_PLACEHOLDER);
        b.add(p, NTEMPLATE.HAS_PREFIX, "https://doi.org/");
        Template t = b.build();
        BindingContext c = context(t);
        PlaceholderMatcher m = PlaceholderMatcher.of(t, p);

        // Such an IRI has to be given in full, and is taken that way when read:
        IRI withColon = iri("https://doi.org/10.1234/a:b");
        assertValid(m, withColon, c);
        assertRoundTrip(m, "https://doi.org/10.1234/a:b", withColon, c);
        assertThrows(IllegalArgumentException.class, () -> m.toValue(new RawValue("10.1234/a:b"), c));
        assertThrows(IllegalArgumentException.class, () -> m.toValue(new RawValue("urn:isbn:0451450523"), c));
        // Without a pattern, any other IRI is taken too:
        assertValid(m, iri("https://example.com/elsewhere"), c);
        assertRoundTrip(m, "https://example.com/elsewhere", iri("https://example.com/elsewhere"), c);
    }

    @Test
    void externalUriPlaceholderTakesNoMintedIri() throws Exception {
        TemplateBuilder b = new TemplateBuilder();
        IRI p = b.placeholder("space", NTEMPLATE.EXTERNAL_URI_PLACEHOLDER, NTEMPLATE.GUIDED_CHOICE_PLACEHOLDER);
        b.add(p, NTEMPLATE.POSSIBLE_VALUES_FROM_API, "https://example.com/api?search=");
        Template t = b.build();
        BindingContext c = context(t);
        PlaceholderMatcher m = PlaceholderMatcher.of(t, p);

        // A guided choice only suggests values, so any external IRI is taken:
        assertValid(m, iri("https://w3id.org/spaces/example"), c);
        assertBlocked(Violation.Kind.NOT_EXTERNAL, m, iri(NS + "my-space"), c);
        // A plain name still turns into a minted IRI, which the check then refuses:
        Value minted = m.toValue(new RawValue("my-space"), c);
        assertEquals(iri(NS + "my-space"), minted);
        assertFalse(m.match(minted, c).isBindable());
    }

    @Test
    void agentPlaceholderTakesAnIri() throws Exception {
        TemplateBuilder b = new TemplateBuilder();
        IRI p = b.placeholder("author", NTEMPLATE.AGENT_PLACEHOLDER, NTEMPLATE.EXTERNAL_URI_PLACEHOLDER);
        Template t = b.build();
        BindingContext c = context(t);
        PlaceholderMatcher m = PlaceholderMatcher.of(t, p);

        assertValid(m, CREATOR, c);
        assertRoundTrip(m, CREATOR.stringValue(), CREATOR, c);
        assertBlocked(Violation.Kind.WRONG_KIND, m, vf.createLiteral("Jane Doe"), c);
    }

    @Test
    void trustyUriPlaceholderTakesTrustyUris() throws Exception {
        TemplateBuilder b = new TemplateBuilder();
        IRI p = b.placeholder("np", NTEMPLATE.TRUSTY_URI_PLACEHOLDER);
        Template t = b.build();
        BindingContext c = context(t);
        PlaceholderMatcher m = PlaceholderMatcher.of(t, p);

        assertValid(m, iri("https://w3id.org/np/RAZyXwVuTsRqPoNmLkJiHgFeDcBa9876543210_-ZyXwV"), c);
        assertBlocked(Violation.Kind.NOT_TRUSTY, m, iri("https://example.com/not-trusty"), c);
        assertBlocked(Violation.Kind.NOT_EXTERNAL, m, iri(NS + "thing"), c);
    }

    @Test
    void autoEscapePlaceholderEscapesItsText() throws Exception {
        TemplateBuilder b = new TemplateBuilder();
        IRI p = b.placeholder("sentence", NTEMPLATE.AUTO_ESCAPE_URI_PLACEHOLDER);
        b.add(p, NTEMPLATE.HAS_PREFIX, "http://purl.org/aida/");
        Template t = b.build();
        BindingContext c = context(t);
        PlaceholderMatcher m = PlaceholderMatcher.of(t, p);

        IRI escaped = iri("http://purl.org/aida/Malaria+is+transmitted+by+mosquitoes.");
        assertValid(m, escaped, c);
        assertRoundTrip(m, "Malaria is transmitted by mosquitoes.", escaped, c);

        // "%20" is a space as well, but escaping writes "+". The value that is published
        // with "%20" has to come back unchanged, so its raw form is the IRI in full:
        IRI otherSpelling = iri("http://purl.org/aida/Malaria%20is%20transmitted%20by%20mosquitoes.");
        assertValid(m, otherSpelling, c);
        assertRoundTrip(m, otherSpelling.stringValue(), otherSpelling, c);

        // A percent sign that starts no escape makes the IRI malformed:
        IRI stray = iri("http://purl.org/aida/100%+sure.");
        assertBlocked(Violation.Kind.MALFORMED_IRI, m, stray, c);
        assertEquals(new RawValue(stray.stringValue()), m.toRaw(stray, c));
        // The text is escaped whatever it is, also where it looks like an IRI:
        assertRoundTrip(m, "see https://example.com/x#y", iri("http://purl.org/aida/see+https%3A%2F%2Fexample.com%2Fx%23y"), c);
    }

    @Test
    void prefixThatDependsOnTheSpaceHasToBeGiven() throws Exception {
        TemplateBuilder b = new TemplateBuilder();
        IRI p = b.placeholder("resource", NTEMPLATE.URI_PLACEHOLDER);
        b.add(p, NTEMPLATE.HAS_PREFIX, BindingContext.SPACE_TOKEN + "/r/");
        Template t = b.build();
        BindingContext c = context(t);
        PlaceholderMatcher m = PlaceholderMatcher.of(t, p);

        assertTrue(c.hasDynamicPrefix(p));
        assertTrue(c.hasUnresolvedPrefix(p));
        assertNull(c.getPrefix(p));
        // Without the space, a name cannot be turned into an IRI, but an IRI in full can:
        assertThrows(IllegalArgumentException.class, () -> m.toValue(new RawValue("thing"), c));
        IRI full = iri("https://w3id.org/spaces/example/r/thing");
        assertEquals(full, m.toValue(new RawValue(full.stringValue()), c));
        assertValid(m, full, c);
        assertEquals(new RawValue(full.stringValue()), m.toRaw(full, c));

        c.setPrefixBase(BindingContext.SPACE_TOKEN, "https://w3id.org/spaces/example");
        assertFalse(c.hasUnresolvedPrefix(p));
        assertEquals("https://w3id.org/spaces/example/r/", c.getPrefix(p));
        assertRoundTrip(m, "thing", full, c);

        c.setPrefixBase(BindingContext.SPACE_TOKEN, null);
        assertTrue(c.hasUnresolvedPrefix(p));
    }

    @Test
    void rootNanopubPlaceholderDefaultsToTheNanopublication() throws Exception {
        TemplateBuilder b = new TemplateBuilder();
        IRI p = b.placeholder("root", NTEMPLATE.ROOT_NANOPUB_PLACEHOLDER);
        Template t = b.build();
        BindingContext c = context(t);
        PlaceholderMatcher m = PlaceholderMatcher.of(t, p);

        assertEquals(iri(NS), m.resolve(c));
        assertValid(m, iri(NS), c);
        IRI earlier = iri("https://w3id.org/np/RAZyXwVuTsRqPoNmLkJiHgFeDcBa9876543210_-ZyXwV");
        assertTrue(c.bind(p, earlier).isValid());
        assertEquals(earlier, m.resolve(c));
    }

    // ---------- restricted choice ----------

    @Test
    void restrictedChoiceTakesOnlyItsOptions() throws Exception {
        TemplateBuilder b = new TemplateBuilder();
        IRI p = b.placeholder("type", NTEMPLATE.RESTRICTED_CHOICE_PLACEHOLDER);
        IRI optionA = iri("http://example.com/A");
        IRI marked = iri("https://example.com/r/~~ARTIFACTCODE~~/B");
        b.add(p, NTEMPLATE.POSSIBLE_VALUE, optionA).add(p, NTEMPLATE.POSSIBLE_VALUE, marked)
                .add(p, NTEMPLATE.POSSIBLE_VALUE, vf.createLiteral("literal option"));
        Template t = b.build();
        BindingContext c = context(t);
        PlaceholderMatcher m = PlaceholderMatcher.of(t, p);

        assertValid(m, optionA, c);
        assertRoundTrip(m, "http://example.com/A", optionA, c);
        assertBlocked(Violation.Kind.NOT_A_POSSIBLE_VALUE, m, iri("http://example.com/C"), c);
        assertValid(m, iri("https://example.com/r/RAZyXwVuTsRqPoNmLkJiHgFeDcBa9876543210_-ZyXwV/B"), c);
        // A literal among the options is given as a raw value in its text form:
        assertValid(m, vf.createLiteral("literal option"), c);
        assertRoundTrip(m, "\"literal option\"", vf.createLiteral("literal option"), c);
        assertBlocked(Violation.Kind.NOT_A_POSSIBLE_VALUE, m, vf.createLiteral("another literal"), c);
    }

    @Test
    void restrictedChoiceCanReferToOtherPlaceholders() throws Exception {
        TemplateBuilder b = new TemplateBuilder();
        IRI defined = b.placeholder("defined", NTEMPLATE.URI_PLACEHOLDER, NTEMPLATE.LOCAL_RESOURCE);
        IRI choice = b.placeholder("choice", NTEMPLATE.RESTRICTED_CHOICE_PLACEHOLDER);
        b.add(choice, NTEMPLATE.POSSIBLE_VALUE, defined).add(choice, NTEMPLATE.POSSIBLE_VALUE, iri("http://example.com/A"));
        Template t = b.build();
        BindingContext c = context(t);
        PlaceholderMatcher m = PlaceholderMatcher.of(t, choice);

        // The value the other placeholder will have is not known yet, so the value can be
        // bound, and is reported as long as nothing confirms it:
        assertReported(Violation.Kind.NOT_A_POSSIBLE_VALUE, m, iri(NS + "second"), c);
        assertTrue(c.bind(choice, iri(NS + "second")).isBindable());
        assertEquals(List.of(Violation.Kind.NOT_A_POSSIBLE_VALUE), c.getViolations().stream().map(Violation::kind).toList());

        // Any repetition of the placeholder referred to offers its value:
        assertTrue(c.bind(defined, iri(NS + "first")).isValid());
        assertFalse(c.getViolations().isEmpty());
        assertTrue(c.bind(local("defined__1"), iri(NS + "second")).isValid());
        assertEquals(List.of(), c.getViolations());
        BindingContext other = context(t);
        assertTrue(other.bind(defined, iri(NS + "first")).isValid());
        assertValid(m, iri(NS + "first"), other);
        // A fixed option stays one, and a literal is never the value of another placeholder:
        assertValid(m, iri("http://example.com/A"), context(t));
        assertBlocked(Violation.Kind.NOT_A_POSSIBLE_VALUE, m, vf.createLiteral("x"), context(t));
    }

    // ---------- placeholders that take a literal ----------

    @Test
    void literalPlaceholderTakesAPlainLiteral() throws Exception {
        TemplateBuilder b = new TemplateBuilder();
        IRI p = b.placeholder("name", NTEMPLATE.LITERAL_PLACEHOLDER);
        b.add(p, NTEMPLATE.HAS_REGEX, "[A-Z].*");
        IRI longText = b.placeholder("text", NTEMPLATE.LONG_LITERAL_PLACEHOLDER);
        Template t = b.build();
        BindingContext c = context(t);
        PlaceholderMatcher m = PlaceholderMatcher.of(t, p);

        assertValid(m, vf.createLiteral("Alice"), c);
        assertRoundTrip(m, "Alice", vf.createLiteral("Alice"), c);
        assertNull(m.toValue(new RawValue(""), c));
        assertBlocked(Violation.Kind.REGEX, m, vf.createLiteral("alice"), c);
        assertBlocked(Violation.Kind.WRONG_KIND, m, iri("http://example.com/Alice"), c);
        // A datatype or language tag the placeholder does not declare would be lost when the
        // template is filled in again:
        assertReported(Violation.Kind.DATATYPE, m, vf.createLiteral("Alice", XSD.TOKEN), c);
        assertReported(Violation.Kind.LANGUAGE_TAG, m, vf.createLiteral("Alice", "en"), c);

        PlaceholderMatcher longMatcher = PlaceholderMatcher.of(t, longText);
        assertRoundTrip(longMatcher, "two\nlines", vf.createLiteral("two\nlines"), c);
        assertThrows(IllegalArgumentException.class, () -> m.toValue(new RawValue("Alice", "en"), c));
    }

    @Test
    void declaredDatatypeIsExpected() throws Exception {
        TemplateBuilder b = new TemplateBuilder();
        IRI p = b.placeholder("count", NTEMPLATE.LITERAL_PLACEHOLDER);
        b.add(p, NTEMPLATE.HAS_DATATYPE, XSD.INT);
        IRI date = b.placeholder("date", NTEMPLATE.LITERAL_PLACEHOLDER);
        b.add(date, NTEMPLATE.HAS_DATATYPE, XSD.DATE);
        IRI dateTime = b.placeholder("dateTime", NTEMPLATE.LITERAL_PLACEHOLDER);
        b.add(dateTime, NTEMPLATE.HAS_DATATYPE, XSD.DATETIME);
        IRI year = b.placeholder("year", NTEMPLATE.LITERAL_PLACEHOLDER);
        b.add(year, NTEMPLATE.HAS_DATATYPE, XSD.GYEAR);
        Template t = b.build();
        BindingContext c = context(t);

        PlaceholderMatcher m = PlaceholderMatcher.of(t, p);
        assertValid(m, vf.createLiteral("5", XSD.INT), c);
        assertRoundTrip(m, "5", vf.createLiteral("5", XSD.INT), c);
        // Both can be read, and are reported:
        assertReported(Violation.Kind.DATATYPE, m, vf.createLiteral("5"), c);
        assertReported(Violation.Kind.ILL_TYPED, m, vf.createLiteral("abc", XSD.INT), c);
        MatchResult both = m.match(vf.createLiteral("abc"), c);
        assertTrue(both.isBindable());
        assertEquals(List.of(Violation.Kind.DATATYPE, Violation.Kind.ILL_TYPED), kinds(both));
        assertEquals("Value 'abc' of 'count' is not a valid xsd:int", both.violations().get(1).message());

        PlaceholderMatcher dateMatcher = PlaceholderMatcher.of(t, date);
        assertRoundTrip(dateMatcher, "2026-11-19", vf.createLiteral("2026-11-19", XSD.DATE), c);
        assertValid(dateMatcher, vf.createLiteral("2026-11-19", XSD.DATE), c);
        assertReported(Violation.Kind.ILL_TYPED, dateMatcher, vf.createLiteral("2026-13-45", XSD.DATE), c);

        PlaceholderMatcher dateTimeMatcher = PlaceholderMatcher.of(t, dateTime);
        // The value is kept as it is written, timezone included:
        assertRoundTrip(dateTimeMatcher, "2026-11-19T09:00:00+00:00", vf.createLiteral("2026-11-19T09:00:00+00:00", XSD.DATETIME), c);
        assertValid(dateTimeMatcher, vf.createLiteral("2026-11-19T09:00:00.000Z", XSD.DATETIME), c);
        // A date alone is not a date and time:
        assertReported(Violation.Kind.ILL_TYPED, dateTimeMatcher, vf.createLiteral("2026-11-19", XSD.DATETIME), c);

        PlaceholderMatcher yearMatcher = PlaceholderMatcher.of(t, year);
        assertRoundTrip(yearMatcher, "2026", vf.createLiteral("2026", XSD.GYEAR), c);
        assertReported(Violation.Kind.ILL_TYPED, yearMatcher, vf.createLiteral("26", XSD.GYEAR), c);
    }

    @Test
    void declaredLanguageTagIsExpected() throws Exception {
        TemplateBuilder b = new TemplateBuilder();
        IRI p = b.placeholder("title", NTEMPLATE.LITERAL_PLACEHOLDER);
        b.add(p, NTEMPLATE.HAS_LANGUAGE_TAG, "en");
        Template t = b.build();
        BindingContext c = context(t);
        PlaceholderMatcher m = PlaceholderMatcher.of(t, p);

        assertValid(m, vf.createLiteral("house", "en"), c);
        assertValid(m, vf.createLiteral("house", "EN"), c);
        assertEquals(vf.createLiteral("house", "en"), m.toValue(new RawValue("house"), c));
        assertEquals(new RawValue("house"), m.toRaw(vf.createLiteral("house", "en"), c));
        // A literal in another language can be read, to be corrected:
        assertReported(Violation.Kind.LANGUAGE_TAG, m, vf.createLiteral("maison", "fr"), c);
        assertReported(Violation.Kind.LANGUAGE_TAG, m, vf.createLiteral("house"), c);
        // The declared tag can be named, another one cannot be chosen:
        assertEquals(vf.createLiteral("house", "en"), m.toValue(new RawValue("house", "EN"), c));
        assertThrows(IllegalArgumentException.class, () -> m.toValue(new RawValue("maison", "fr"), c));
    }

    @Test
    void languageCanBeLeftOpen() throws Exception {
        TemplateBuilder b = new TemplateBuilder();
        IRI any = b.placeholder("any", NTEMPLATE.LANGUAGE_TAGGED_LITERAL_PLACEHOLDER);
        IRI some = b.placeholder("some", NTEMPLATE.LANGUAGE_TAGGED_LITERAL_PLACEHOLDER);
        b.add(some, NTEMPLATE.POSSIBLE_LANGUAGE_TAG, "en").add(some, NTEMPLATE.POSSIBLE_LANGUAGE_TAG, "de")
                .add(some, NTEMPLATE.HAS_LANGUAGE_TAG, "de");
        Template t = b.build();
        BindingContext c = context(t);

        PlaceholderMatcher anyMatcher = PlaceholderMatcher.of(t, any);
        assertValid(anyMatcher, vf.createLiteral("maison", "fr"), c);
        assertBlocked(Violation.Kind.LANGUAGE_TAG, anyMatcher, vf.createLiteral("house"), c);
        assertEquals(vf.createLiteral("maison", "fr"), anyMatcher.toValue(new RawValue("maison", "FR"), c));
        assertEquals(new RawValue("maison", "fr"), anyMatcher.toRaw(vf.createLiteral("maison", "fr"), c));
        assertThrows(IllegalArgumentException.class, () -> anyMatcher.toValue(new RawValue("house"), c));
        assertThrows(IllegalArgumentException.class, () -> anyMatcher.toValue(new RawValue("house", "not a tag"), c));

        PlaceholderMatcher someMatcher = PlaceholderMatcher.of(t, some);
        assertValid(someMatcher, vf.createLiteral("house", "en"), c);
        assertBlocked(Violation.Kind.LANGUAGE_TAG, someMatcher, vf.createLiteral("maison", "fr"), c);
        // The declared tag of such a placeholder is the one taken if none is chosen:
        assertEquals(vf.createLiteral("Haus", "de"), someMatcher.toValue(new RawValue("Haus"), c));
        assertEquals(vf.createLiteral("house", "en"), someMatcher.toValue(new RawValue("house", "en"), c));
        // A tag outside the set is turned into a value as asked, which the check refuses:
        assertFalse(someMatcher.match(someMatcher.toValue(new RawValue("maison", "fr"), c), c).isBindable());
    }

    @Test
    void sparqlHasToParse() throws Exception {
        TemplateBuilder b = new TemplateBuilder();
        IRI p = local("sparql");
        b.add(p, RDF.TYPE, NTEMPLATE.LONG_LITERAL_PLACEHOLDER);
        b.statement(SUBJECT, KPXL_GRLC.SPARQL, p);
        Template t = b.build();
        BindingContext c = context(t);
        PlaceholderMatcher m = PlaceholderMatcher.of(t, p);

        assertValid(m, vf.createLiteral("select * where { ?s ?p ?o }"), c);
        assertReported(Violation.Kind.INVALID_SPARQL, m, vf.createLiteral("select * where { ?s ?p }"), c);
    }

    // ---------- date placeholder ----------

    @Test
    void datePlaceholderTakesAPointInTimeAtAnAcceptedPrecision() throws Exception {
        TemplateBuilder b = new TemplateBuilder();
        IRI any = b.placeholder("any", NTEMPLATE.DATE_PLACEHOLDER);
        IRI day = b.placeholder("day", NTEMPLATE.DATE_PLACEHOLDER);
        b.add(day, NTEMPLATE.HAS_DATATYPE, XSD.DATE).add(day, NTEMPLATE.HAS_DATATYPE, XSD.DATETIME)
                .add(day, NTEMPLATE.HAS_REGEX, "2026.*");
        Template t = b.build();
        BindingContext c = context(t);

        PlaceholderMatcher anyMatcher = PlaceholderMatcher.of(t, any);
        assertRoundTrip(anyMatcher, "2026", vf.createLiteral("2026", XSD.GYEAR), c);
        assertRoundTrip(anyMatcher, "2026-11", vf.createLiteral("2026-11", XSD.GYEARMONTH), c);
        assertRoundTrip(anyMatcher, "2026-11-19", vf.createLiteral("2026-11-19", XSD.DATE), c);
        assertRoundTrip(anyMatcher, "2026-11-19T09:00:00+01:00", vf.createLiteral("2026-11-19T09:00:00+01:00", XSD.DATETIME), c);
        for (String lexical : List.of("2026", "2026-11", "2026-11-19", "2026-11-19T09:00:00+01:00", "2026-11-19T09:00:00")) {
            assertValid(anyMatcher, anyMatcher.toValue(new RawValue(lexical), c), c);
        }
        assertBlocked(Violation.Kind.WRONG_KIND, anyMatcher, iri("http://example.com/2026"), c);
        // The datatype has to be the one of the precision the value has:
        assertBlocked(Violation.Kind.DATE_PRECISION, anyMatcher, vf.createLiteral("2026"), c);
        assertBlocked(Violation.Kind.DATE_PRECISION, anyMatcher, vf.createLiteral("2026", XSD.DATE), c);
        assertBlocked(Violation.Kind.DATE_PRECISION, anyMatcher, vf.createLiteral("2026-02-30", XSD.DATE), c);
        assertThrows(IllegalArgumentException.class, () -> anyMatcher.toValue(new RawValue("next week"), c));
        assertThrows(IllegalArgumentException.class, () -> anyMatcher.toValue(new RawValue("2026-02-30"), c));

        PlaceholderMatcher dayMatcher = PlaceholderMatcher.of(t, day);
        assertValid(dayMatcher, vf.createLiteral("2026-11-19", XSD.DATE), c);
        assertBlocked(Violation.Kind.DATE_PRECISION, dayMatcher, vf.createLiteral("2026", XSD.GYEAR), c);
        assertBlocked(Violation.Kind.REGEX, dayMatcher, vf.createLiteral("2025-11-19", XSD.DATE), c);
        // A year is turned into a value as asked, which the check refuses for this placeholder:
        assertFalse(dayMatcher.match(dayMatcher.toValue(new RawValue("2026"), c), c).isBindable());
    }

    // ---------- value placeholder ----------

    @Test
    void valuePlaceholderTakesAnIriOrALiteral() throws Exception {
        TemplateBuilder b = new TemplateBuilder();
        IRI p = b.placeholder("value", NTEMPLATE.VALUE_PLACEHOLDER);
        Template t = b.build();
        BindingContext c = context(t);
        PlaceholderMatcher m = PlaceholderMatcher.of(t, p);

        assertRoundTrip(m, "https://example.com/thing", iri("https://example.com/thing"), c);
        assertRoundTrip(m, "thing", iri(NS + "thing"), c);
        assertRoundTrip(m, "\"some text\"", vf.createLiteral("some text"), c);
        assertRoundTrip(m, "\"Haus\"@de", vf.createLiteral("Haus", "de"), c);
        assertRoundTrip(m, "\"5\"^^<http://www.w3.org/2001/XMLSchema#integer>", vf.createLiteral("5", XSD.INTEGER), c);
        assertRoundTrip(m, "\"two\\nlines\"", vf.createLiteral("two\nlines"), c);
        for (String raw : List.of("https://example.com/thing", "thing", "\"some text\"", "\"Haus\"@de")) {
            assertValid(m, m.toValue(new RawValue(raw), c), c);
        }
        assertBlocked(Violation.Kind.SCHEME_NOT_ALLOWED, m, iri("urn:isbn:0451450523"), c);
        assertThrows(IllegalArgumentException.class, () -> m.toValue(new RawValue("\"unterminated"), c));
        assertThrows(IllegalArgumentException.class, () -> m.toValue(new RawValue("two words"), c));
    }

    // ---------- across kinds ----------

    @Test
    void boundPositionTakesNoOtherValue() throws Exception {
        TemplateBuilder b = new TemplateBuilder();
        IRI p = b.placeholder("name", NTEMPLATE.LITERAL_PLACEHOLDER);
        Template t = b.build();
        BindingContext c = context(t);
        PlaceholderMatcher m = PlaceholderMatcher.of(t, p);

        assertTrue(c.bind(p, vf.createLiteral("Alice")).isValid());
        assertValid(m, vf.createLiteral("Alice"), c);
        assertBlocked(Violation.Kind.ALREADY_BOUND, m, vf.createLiteral("Bob"), c);
        // Another repetition of the placeholder is another position:
        assertValid(PlaceholderMatcher.of(t, local("name__1")), vf.createLiteral("Bob"), c);
        assertThrows(NullPointerException.class, () -> m.match(null, c));
    }

    @Test
    void messagesNameTheFieldByItsLabel() throws Exception {
        TemplateBuilder b = new TemplateBuilder();
        IRI labelled = b.placeholder("name", NTEMPLATE.LITERAL_PLACEHOLDER);
        b.add(labelled, RDFS.LABEL, "the name").add(labelled, NTEMPLATE.HAS_REGEX, "[A-Z].*");
        IRI unlabelled = b.placeholder("code", NTEMPLATE.LITERAL_PLACEHOLDER);
        b.add(unlabelled, NTEMPLATE.HAS_REGEX, "[0-9]+");
        Template t = b.build();
        BindingContext c = context(t);

        Violation v = PlaceholderMatcher.of(t, labelled).match(vf.createLiteral("alice"), c).violations().get(0);
        assertEquals("Value 'alice' of 'the name' doesn't match the pattern '[A-Z].*'", v.message());
        assertEquals(labelled, v.templateValue());
        assertEquals(vf.createLiteral("alice"), v.value());
        assertTrue(v.blocking());
        // Without a label, the name of the placeholder is taken, without a repetition suffix:
        Violation w = PlaceholderMatcher.of(t, local("code__2")).match(vf.createLiteral("x"), c).violations().get(0);
        assertEquals("Value 'x' of 'code' doesn't match the pattern '[0-9]+'", w.message());
    }

}
