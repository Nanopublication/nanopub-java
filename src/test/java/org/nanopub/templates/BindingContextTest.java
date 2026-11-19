package org.nanopub.templates;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.Test;
import org.nanopub.Nanopub;
import org.nanopub.NanopubImpl;
import org.nanopub.vocabulary.KPXL;
import org.nanopub.vocabulary.NTEMPLATE;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.nanopub.templates.TemplateBuilder.PREDICATE;
import static org.nanopub.templates.TemplateBuilder.SUBJECT;
import static org.nanopub.templates.TemplateBuilder.local;

/**
 * Tests for {@link BindingContext}.
 */
class BindingContextTest {

    private static final ValueFactory vf = SimpleValueFactory.getInstance();

    private static final String NS = "http://purl.org/nanopub/temp/np001/";
    private static final IRI CREATOR = vf.createIRI("https://orcid.org/0000-0000-0000-0001");
    private static final String INSTANCE_URI = "https://w3id.org/np/RAZyXwVuTsRqPoNmLkJiHgFeDcBa9876543210_-ZyXwV";

    private static IRI iri(String s) {
        return vf.createIRI(s);
    }

    // A nanopublication with two creators, to check against a template.
    private static Nanopub instanceNanopub() throws Exception {
        String trig = """
                @prefix this: <%1$s> .
                @prefix sub: <%1$s/> .
                @prefix np: <http://www.nanopub.org/nschema#> .
                @prefix dct: <http://purl.org/dc/terms/> .
                @prefix prov: <http://www.w3.org/ns/prov#> .
                @prefix orcid: <https://orcid.org/> .
                sub:Head { this: a np:Nanopublication ; np:hasAssertion sub:assertion ;
                    np:hasProvenance sub:provenance ; np:hasPublicationInfo sub:pubinfo . }
                sub:assertion { sub:thing <http://example.com/predicate> "x" . }
                sub:provenance { sub:assertion prov:wasAttributedTo orcid:0000-0000-0000-0001 . }
                sub:pubinfo { this: dct:creator orcid:0000-0000-0000-0001 , orcid:0000-0000-0000-0002 . }
                """.formatted(INSTANCE_URI);
        return new NanopubImpl(trig, RDFFormat.TRIG);
    }

    private static Template simpleTemplate() throws Exception {
        TemplateBuilder b = new TemplateBuilder();
        b.placeholder("name", NTEMPLATE.LITERAL_PLACEHOLDER);
        return b.build();
    }

    @Test
    void contextForANewNanopublication() throws Exception {
        Template t = simpleTemplate();
        BindingContext c = BindingContext.forNewNanopub(t, NS, CREATOR);

        assertSame(t, c.getTemplate());
        assertEquals(iri(NS), c.getNanopubUri());
        assertEquals(iri(NS + "assertion"), c.getAssertionUri());
        assertEquals(NS, c.getNamespace());
        assertEquals(Set.of(CREATOR), c.getCreators());
        // The creator is what nt:CREATOR stands for from the start:
        assertEquals(Map.of(NTEMPLATE.CREATOR_PLACEHOLDER, CREATOR), c.getBindings());
        assertEquals(iri(NS + "thing"), c.mint("thing"));
        assertEquals("thing", c.getLocalName(iri(NS + "thing")));
        assertNull(c.getLocalName(iri(NS)));
        assertNull(c.getLocalName(iri("https://example.com/thing")));
        assertNull(c.getLocalName(vf.createLiteral(NS + "thing")));

        BindingContext noCreator = BindingContext.forNewNanopub(t, NS, null);
        assertEquals(Set.of(), noCreator.getCreators());
        assertEquals(Map.of(), noCreator.getBindings());
        // A URI without a separator at its end gets a slash for its namespace, one with a
        // hash keeps it:
        assertEquals("https://example.com/np1/", BindingContext.forNewNanopub(t, "https://example.com/np1", null).getNamespace());
        BindingContext hash = BindingContext.forNewNanopub(t, "https://example.com/np1#", null);
        assertEquals("https://example.com/np1#", hash.getNamespace());
        assertEquals(iri("https://example.com/np1#assertion"), hash.getAssertionUri());
    }

    @Test
    void contextForAnExistingNanopublication() throws Exception {
        Template t = simpleTemplate();
        Nanopub np = instanceNanopub();
        BindingContext c = BindingContext.forNanopub(t, np);

        assertEquals(np.getUri(), c.getNanopubUri());
        assertEquals(np.getAssertionUri(), c.getAssertionUri());
        assertEquals(INSTANCE_URI + "/", c.getNamespace());
        assertEquals(Set.of(CREATOR, iri("https://orcid.org/0000-0000-0000-0002")), c.getCreators());
        assertEquals(Map.of(), c.getBindings());
        assertEquals("thing", c.getLocalName(iri(INSTANCE_URI + "/thing")));
        // The nanopublication itself, and one whose URI merely starts the same, are not in
        // its namespace:
        assertNull(c.getLocalName(np.getUri()));
        assertNull(c.getLocalName(iri(INSTANCE_URI + "x/thing")));
        // Either creator can be what nt:CREATOR stands for:
        assertTrue(c.bind(NTEMPLATE.CREATOR_PLACEHOLDER, iri("https://orcid.org/0000-0000-0000-0002")).isValid());
        assertFalse(BindingContext.forNanopub(t, np).bind(NTEMPLATE.CREATOR_PLACEHOLDER, iri("https://orcid.org/0000-0000-0000-0003")).isBindable());
    }

    @Test
    void valuesAreBoundAndUnbound() throws Exception {
        Template t = simpleTemplate();
        IRI name = local("name");
        BindingContext c = BindingContext.forNewNanopub(t, NS, null);

        assertFalse(c.isBound(name));
        assertNull(c.get(name));
        assertNull(c.resolve(name));
        assertTrue(c.bind(name, vf.createLiteral("Alice")).isValid());
        assertTrue(c.isBound(name));
        assertEquals(vf.createLiteral("Alice"), c.get(name));
        assertEquals(vf.createLiteral("Alice"), c.resolve(name));
        // Binding the same value again changes nothing, another one is refused:
        assertTrue(c.bind(name, vf.createLiteral("Alice")).isValid());
        MatchResult other = c.bind(name, vf.createLiteral("Bob"));
        assertFalse(other.isBindable());
        assertEquals(vf.createLiteral("Alice"), c.get(name));
        // A value that cannot stand in the position is not bound:
        assertFalse(c.bind(local("name__1"), SUBJECT).isBindable());
        assertFalse(c.isBound(local("name__1")));
        // A value that only breaks a constraint is bound, and reported:
        MatchResult tagged = c.bind(local("name__1"), vf.createLiteral("Bob", "en"));
        assertTrue(tagged.isBindable());
        assertFalse(tagged.isValid());
        assertEquals(List.of(Violation.Kind.LANGUAGE_TAG), c.getViolations().stream().map(Violation::kind).toList());

        c.unbind(name);
        assertFalse(c.isBound(name));
        assertTrue(c.bind(name, vf.createLiteral("Bob")).isValid());
        assertThrows(UnsupportedOperationException.class, () -> c.getBindings().clear());
    }

    @Test
    void snapshotBringsBackEarlierBindings() throws Exception {
        Template t = simpleTemplate();
        IRI name = local("name");
        BindingContext c = BindingContext.forNewNanopub(t, NS, null);
        c.bind(name, vf.createLiteral("Alice"));

        Map<IRI, Value> snapshot = c.snapshot();
        c.bind(local("name__1"), vf.createLiteral("Bob"));
        c.unbind(name);
        assertEquals(Map.of(local("name__1"), vf.createLiteral("Bob")), c.getBindings());

        c.restore(snapshot);
        assertEquals(Map.of(name, vf.createLiteral("Alice")), c.getBindings());
        // The snapshot is a copy, which later bindings do not change:
        c.bind(local("name__1"), vf.createLiteral("Bob"));
        assertEquals(1, snapshot.size());
    }

    @Test
    void fixedPositionsResolveWithoutBindings() throws Exception {
        TemplateBuilder b = new TemplateBuilder();
        IRI thing = local("thing");
        IRI name = local("name");
        b.add(name, RDF.TYPE, NTEMPLATE.LITERAL_PLACEHOLDER);
        b.statement(thing, PREDICATE, name);
        b.statement(NTEMPLATE.NANOPUB_PLACEHOLDER, PREDICATE, vf.createLiteral("fixed"));
        Template t = b.build();
        BindingContext c = BindingContext.forNewNanopub(t, NS, CREATOR);

        assertEquals(iri(NS + "thing"), c.resolve(thing));
        assertEquals(PREDICATE, c.resolve(PREDICATE));
        assertNull(c.resolve(name));
        assertEquals(iri(NS), c.resolve(NTEMPLATE.NANOPUB_PLACEHOLDER));
        assertEquals(CREATOR, c.resolve(NTEMPLATE.CREATOR_PLACEHOLDER));
        assertEquals(vf.createLiteral("fixed"), c.resolve(vf.createLiteral("fixed")));
        assertNull(c.resolve(null));
    }

    @Test
    void introducedAndEmbeddedResourcesAreReadOffTheBindings() throws Exception {
        TemplateBuilder b = new TemplateBuilder();
        IRI introduced = local("resource");
        IRI embedded = local("version");
        IRI other = local("other");
        b.add(introduced, RDF.TYPE, NTEMPLATE.URI_PLACEHOLDER).add(introduced, RDF.TYPE, NTEMPLATE.INTRODUCED_RESOURCE);
        b.add(embedded, RDF.TYPE, NTEMPLATE.EMBEDDED_RESOURCE).add(embedded, RDF.TYPE, NTEMPLATE.LOCAL_RESOURCE);
        b.add(other, RDF.TYPE, NTEMPLATE.URI_PLACEHOLDER);
        b.statement(embedded, DCTERMS.IS_VERSION_OF, introduced);
        b.statement(embedded, RDFS.SEEALSO, other);
        Template t = b.build();
        BindingContext c = BindingContext.forNewNanopub(t, NS, null);

        assertEquals(Set.of(), c.getIntroducedIris());
        assertEquals(Set.of(), c.getEmbeddedIris());
        assertTrue(c.bind(introduced, iri("https://example.com/resource")).isValid());
        assertTrue(c.bind(other, iri("https://example.com/other")).isValid());
        // A position with a fixed value is recorded when its value is bound, like a placeholder:
        assertTrue(c.bind(embedded, c.resolve(embedded)).isValid());
        assertTrue(c.bind(local("version__1"), c.resolve(local("version__1"))).isValid());

        assertEquals(Set.of(iri("https://example.com/resource")), c.getIntroducedIris());
        assertEquals(Set.of(iri(NS + "version"), iri(NS + "version__1")), c.getEmbeddedIris());
    }

    @Test
    void roleDirectionPinsAreReadOffTheBindings() throws Exception {
        TemplateBuilder b = new TemplateBuilder();
        IRI fixedRole = iri("http://example.com/hasOrganizer");
        IRI chosenRole = local("role");
        b.add(fixedRole, RDF.TYPE, KPXL.INVERSE_ROLE_PROPERTY);
        b.add(chosenRole, RDF.TYPE, NTEMPLATE.URI_PLACEHOLDER).add(chosenRole, RDF.TYPE, KPXL.REGULAR_ROLE_PROPERTY);
        b.statement(SUBJECT, fixedRole, vf.createLiteral("x"));
        b.statement(SUBJECT, chosenRole, vf.createLiteral("y"));
        Template t = b.build();
        BindingContext c = BindingContext.forNewNanopub(t, NS, null);

        assertEquals(Map.of(), c.getRoleDirectionPins());
        assertTrue(c.bind(fixedRole, fixedRole).isValid());
        assertTrue(c.bind(chosenRole, iri("http://example.com/isMemberOf")).isValid());
        assertEquals(Map.of(fixedRole, KPXL.INVERSE_ROLE_PROPERTY, iri("http://example.com/isMemberOf"), KPXL.REGULAR_ROLE_PROPERTY),
                c.getRoleDirectionPins());
    }

    @Test
    void contextSurvivesSerialization() throws Exception {
        Template t = simpleTemplate();
        BindingContext c = BindingContext.forNewNanopub(t, NS, CREATOR);
        c.bind(local("name"), vf.createLiteral("Alice", "en"));
        c.setPrefixBase(BindingContext.SPACE_TOKEN, "https://w3id.org/spaces/example");

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(c);
            out.writeObject(PlaceholderMatcher.of(t, local("name")));
        }
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            BindingContext copy = (BindingContext) in.readObject();
            PlaceholderMatcher matcher = (PlaceholderMatcher) in.readObject();
            assertEquals(c.getBindings(), copy.getBindings());
            assertEquals(c.getNamespace(), copy.getNamespace());
            assertEquals(c.getCreators(), copy.getCreators());
            assertFalse(matcher.match(vf.createLiteral("Bob"), copy).isBindable());
        }
    }

}
