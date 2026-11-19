package org.nanopub.templates;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.junit.jupiter.api.Test;
import org.nanopub.vocabulary.NTEMPLATE;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link TemplateValues}.
 */
class TemplateValuesTest {

    private static final ValueFactory vf = SimpleValueFactory.getInstance();

    private static final String NP = "https://w3id.org/np/RAZyXwVuTsRqPoNmLkJiHgFeDcBa9876543210_-ZyXwV";
    private static final IRI ME = vf.createIRI("https://orcid.org/0000-0000-0000-0001");

    private static IRI ex(String name) {
        return vf.createIRI("http://example.com/" + name);
    }

    private static IRI sub(String name) {
        return vf.createIRI(NP + "/" + name);
    }

    @Test
    void valuesAreKeptUnderTheNamesOfTheirPlaceholders() {
        TemplateValues values = new TemplateValues().set("name", "Apple").set("title", new RawValue("Titel", "de"));
        values.add("keyword", "fruit").add("keyword", "red");

        assertEquals(new RawValue("Apple"), values.get("name"));
        assertEquals("red", values.get("keyword__1").text());
        assertTrue(values.has("keyword"));
        assertFalse(values.has("keyword__2"));
        assertNull(values.get("other"));
        assertEquals(List.of("name", "title", "keyword", "keyword__1"), List.copyOf(values.getNames()));
        assertEquals("keyword__2", TemplateValues.getName("keyword", 2));
        assertEquals("keyword", TemplateValues.getName("keyword", 0));
        assertEquals("keyword__2", TemplateValues.getName(vf.createIRI(NP + "/keyword__2")));
    }

    @Test
    void valuesAreWrittenAndReadAsParameters() {
        TemplateValues values = new TemplateValues().set("name", "Apple").set("title", new RawValue("Titel", "de")).set("comment", "");
        Map<String, String> params = values.toParams();

        assertEquals(Map.of("name", "Apple", "title", "Titel", "title__lang", "de", "comment", ""), params);
        assertEquals(values, TemplateValues.fromParams(params));
        assertTrue(TemplateValues.fromParams(params).get("comment").isEmpty());
    }

    @Test
    void languageTagWithoutAValueIsKeptAsAName() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("title__lang", "de");
        assertEquals(Set.of("title__lang"), TemplateValues.fromParams(params).getNames());
    }

    @Test
    void valuesAreReadOutOfAMatch() throws Exception {
        // ?thing rdfs:label ?name ; rdfs:comment ?comment (optional, with a default) ;
        //   dct:subject ?keyword (repeatable) ; dct:creator nt:CREATOR ; ex:hasRoot ?root .
        TemplateBuilder t = new TemplateBuilder();
        IRI thing = t.declare("thing", NTEMPLATE.URI_PLACEHOLDER, NTEMPLATE.LOCAL_RESOURCE);
        t.statement(thing, RDFS.LABEL, t.declare("name", NTEMPLATE.LITERAL_PLACEHOLDER));
        IRI comment = t.declare("comment", NTEMPLATE.LITERAL_PLACEHOLDER);
        t.add(comment, NTEMPLATE.HAS_DEFAULT_VALUE, "No comment.");
        t.statement(thing, RDFS.COMMENT, comment, NTEMPLATE.OPTIONAL_STATEMENT);
        t.statement(thing, DCTERMS.SUBJECT, t.declare("keyword", NTEMPLATE.LITERAL_PLACEHOLDER), NTEMPLATE.REPEATABLE_STATEMENT);
        t.statement(thing, DCTERMS.CREATOR, NTEMPLATE.CREATOR_PLACEHOLDER);
        t.statement(thing, ex("hasRoot"), t.declare("root", NTEMPLATE.ROOT_NANOPUB_PLACEHOLDER));
        t.statement(thing, RDF.TYPE, ex("Thing"));
        Template template = t.build();

        List<Statement> triples = List.of(
                vf.createStatement(sub("apple"), RDFS.LABEL, vf.createLiteral("Apple")),
                vf.createStatement(sub("apple"), DCTERMS.SUBJECT, vf.createLiteral("fruit")),
                vf.createStatement(sub("apple"), DCTERMS.SUBJECT, vf.createLiteral("red")),
                vf.createStatement(sub("apple"), DCTERMS.CREATOR, ME),
                vf.createStatement(sub("apple"), ex("hasRoot"), vf.createIRI(NP)),
                vf.createStatement(sub("apple"), RDF.TYPE, ex("Thing")));
        BindingContext context = BindingContext.forNewNanopub(template, NP, ME);
        TemplateMatch match = new TemplateMatcher(template).match(triples, context);
        assertTrue(match.isComplete());

        TemplateValues values = TemplateValues.of(match);

        // The thing by its name, no creator, no root as it is the nanopublication itself,
        // and an empty comment so that the default is not taken.
        assertEquals(Map.of("thing", "apple", "name", "Apple", "keyword", "fruit", "keyword__1", "red", "comment", ""), values.toParams());
    }

    @Test
    void rootDefinitionElsewhereIsAValue() throws Exception {
        TemplateBuilder t = new TemplateBuilder();
        t.statement(ex("thing"), ex("hasRoot"), t.declare("root", NTEMPLATE.ROOT_NANOPUB_PLACEHOLDER));
        Template template = t.build();
        IRI firstVersion = vf.createIRI("https://w3id.org/np/RAFirstFirstFirstFirstFirstFirstFirstFirst000");

        TemplateMatch match = new TemplateMatcher(template).match(List.of(vf.createStatement(ex("thing"), ex("hasRoot"), firstVersion)),
                BindingContext.forNewNanopub(template, NP, ME));

        assertEquals(Map.of("root", firstVersion.stringValue()), TemplateValues.of(match).toParams());
    }

}
