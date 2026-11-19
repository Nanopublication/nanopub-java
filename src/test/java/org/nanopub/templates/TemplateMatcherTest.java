package org.nanopub.templates;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.PROV;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.nanopub.vocabulary.NTEMPLATE;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.nanopub.templates.TemplateBuilder.PREDICATE;
import static org.nanopub.templates.TemplateBuilder.SUBJECT;
import static org.nanopub.templates.TemplateBuilder.local;

/**
 * Tests for {@link TemplateMatcher}: how the triples of a nanopublication are assigned to the
 * statements of a template.
 */
class TemplateMatcherTest {

    private static final ValueFactory vf = SimpleValueFactory.getInstance();

    // The nanopublication whose triples are matched in these tests.
    private static final String NS = "http://purl.org/nanopub/temp/np001/";
    private static final IRI CREATOR = vf.createIRI("https://orcid.org/0000-0000-0000-0001");
    private static final IRI SOME_CLASS = vf.createIRI("http://example.com/SomeClass");

    private static IRI iri(String s) {
        return vf.createIRI(s);
    }

    private static IRI ex(String name) {
        return vf.createIRI("http://example.com/" + name);
    }

    private static Statement triple(IRI s, IRI p, Value o) {
        return vf.createStatement(s, p, o);
    }

    private static Value lit(String s) {
        return vf.createLiteral(s);
    }

    private static TemplateMatch match(Template template, Statement... triples) {
        return new TemplateMatcher(template).match(List.of(triples), BindingContext.forNewNanopub(template, NS, CREATOR));
    }

    // ---------- single statements ----------

    @Test
    void tripleIsAssignedToTheStatementItFits() throws Exception {
        TemplateBuilder b = new TemplateBuilder();
        IRI name = b.declare("name", NTEMPLATE.LITERAL_PLACEHOLDER);
        IRI st = b.statement(SUBJECT, PREDICATE, name);
        Template t = b.build();
        Statement fits = triple(SUBJECT, PREDICATE, lit("Alice"));

        TemplateMatch m = match(t, fits);
        assertTrue(m.isComplete());
        assertSame(t, m.getTemplate());
        assertEquals(1, m.getMatches().size());
        assertEquals(st, m.getMatches().get(0).statement());
        assertEquals(0, m.getMatches().get(0).repetition());
        assertEquals(Map.of(st, fits), m.getMatches().get(0).triples());
        assertEquals(lit("Alice"), m.getContext().get(name));
        assertEquals(List.of(), m.getUnmatchedStatements());
        assertEquals(List.of(), m.getUnusedTriples());
    }

    @Test
    void statementWithoutTripleIsMissingUnlessOptional() throws Exception {
        TemplateBuilder b = new TemplateBuilder();
        IRI required = b.statement(SUBJECT, PREDICATE, b.declare("name", NTEMPLATE.LITERAL_PLACEHOLDER));
        IRI optional = b.statement(SUBJECT, RDFS.COMMENT, b.declare("comment", NTEMPLATE.LITERAL_PLACEHOLDER), NTEMPLATE.OPTIONAL_STATEMENT);
        Template t = b.build();
        Statement other = triple(SUBJECT, RDFS.SEEALSO, ex("elsewhere"));

        TemplateMatch m = match(t, other);
        assertFalse(m.isComplete());
        assertEquals(List.of(required, optional), m.getUnmatchedStatements());
        assertEquals(List.of(required), m.getMissingStatements());
        assertEquals(List.of(other), m.getUnusedTriples());
        assertEquals(List.of(), m.getMatches());

        TemplateMatch withRequired = match(t, triple(SUBJECT, PREDICATE, lit("Alice")));
        assertTrue(withRequired.isComplete());
        assertEquals(List.of(optional), withRequired.getUnmatchedStatements());
    }

    @Test
    void statementThatIsNotRepeatableTakesOneTriple() throws Exception {
        TemplateBuilder b = new TemplateBuilder();
        IRI name = b.declare("name", NTEMPLATE.LITERAL_PLACEHOLDER);
        b.statement(SUBJECT, PREDICATE, name);
        Template t = b.build();

        TemplateMatch m = match(t, triple(SUBJECT, PREDICATE, lit("Bob")), triple(SUBJECT, PREDICATE, lit("Alice")));
        // The triples are tried in the order of subject, predicate and object:
        assertEquals(lit("Alice"), m.getContext().get(name));
        assertEquals(List.of(triple(SUBJECT, PREDICATE, lit("Bob"))), m.getUnusedTriples());
        assertFalse(m.isComplete());
    }

    @Test
    void repeatableStatementTakesAllTriplesThatFit() throws Exception {
        TemplateBuilder b = new TemplateBuilder();
        IRI name = b.declare("name", NTEMPLATE.LITERAL_PLACEHOLDER);
        IRI st = b.statement(SUBJECT, PREDICATE, name, NTEMPLATE.REPEATABLE_STATEMENT);
        Template t = b.build();

        TemplateMatch m = match(t, triple(SUBJECT, PREDICATE, lit("Carol")), triple(SUBJECT, PREDICATE, lit("Alice")),
                triple(SUBJECT, PREDICATE, lit("Bob")));
        assertTrue(m.isComplete());
        assertEquals(3, m.getMatches().size());
        assertEquals(List.of(0, 1, 2), m.getMatches().stream().map(TemplateMatch.StatementMatch::repetition).toList());
        assertTrue(m.getMatches().stream().allMatch(sm -> sm.statement().equals(st)));
        // A placeholder that occurs in this statement only has a value of its own in each
        // repetition, under a key with the repetition as suffix:
        assertEquals(lit("Alice"), m.getContext().get(name));
        assertEquals(lit("Bob"), m.getContext().get(local("name__1")));
        assertEquals(lit("Carol"), m.getContext().get(local("name__2")));
    }

    // ---------- placeholders shared between statements ----------

    @Test
    void sharedPlaceholderTiesStatementsTogether() throws Exception {
        TemplateBuilder b = new TemplateBuilder();
        IRI thing = b.declare("thing", NTEMPLATE.URI_PLACEHOLDER);
        IRI name = b.declare("name", NTEMPLATE.LITERAL_PLACEHOLDER);
        b.statement(thing, RDF.TYPE, SOME_CLASS);
        IRI labelStatement = b.statement(thing, RDFS.LABEL, name);
        Template t = b.build();

        TemplateMatch m = match(t, triple(ex("a"), RDF.TYPE, SOME_CLASS), triple(ex("a"), RDFS.LABEL, lit("thing A")));
        assertTrue(m.isComplete());
        assertEquals(ex("a"), m.getContext().get(thing));

        // A label of something else is not the label of the thing:
        Statement otherLabel = triple(ex("b"), RDFS.LABEL, lit("thing B"));
        TemplateMatch mismatch = match(t, triple(ex("a"), RDF.TYPE, SOME_CLASS), otherLabel);
        assertEquals(List.of(labelStatement), mismatch.getMissingStatements());
        assertEquals(List.of(otherLabel), mismatch.getUnusedTriples());
    }

    @Test
    void sharedPlaceholderIsTheSameInEveryRepetition() throws Exception {
        TemplateBuilder b = new TemplateBuilder();
        IRI thing = b.declare("thing", NTEMPLATE.URI_PLACEHOLDER);
        b.statement(thing, RDF.TYPE, SOME_CLASS);
        b.statement(thing, RDFS.LABEL, b.declare("name", NTEMPLATE.LITERAL_PLACEHOLDER), NTEMPLATE.REPEATABLE_STATEMENT);
        Template t = b.build();
        Statement otherLabel = triple(ex("b"), RDFS.LABEL, lit("label of B"));

        TemplateMatch m = match(t, triple(ex("a"), RDF.TYPE, SOME_CLASS), triple(ex("a"), RDFS.LABEL, lit("one")),
                triple(ex("a"), RDFS.LABEL, lit("two")), otherLabel);
        assertEquals(3, m.getMatches().size());
        assertEquals(List.of(otherLabel), m.getUnusedTriples());
        // The thing has one binding, the name one for each repetition:
        assertEquals(ex("a"), m.getContext().get(thing));
        assertFalse(m.getContext().isBound(local("thing__1")));
        assertEquals(lit("two"), m.getContext().get(local("name__1")));
    }

    @Test
    void placeholderOfOneStatementDiffersBetweenRepetitions() throws Exception {
        TemplateBuilder b = new TemplateBuilder();
        IRI thing = b.declare("thing", NTEMPLATE.URI_PLACEHOLDER);
        b.statement(thing, RDFS.LABEL, b.declare("name", NTEMPLATE.LITERAL_PLACEHOLDER), NTEMPLATE.REPEATABLE_STATEMENT);
        Template t = b.build();

        TemplateMatch m = match(t, triple(ex("a"), RDFS.LABEL, lit("label of A")), triple(ex("b"), RDFS.LABEL, lit("label of B")));
        assertTrue(m.isComplete());
        assertEquals(ex("a"), m.getContext().get(thing));
        assertEquals(ex("b"), m.getContext().get(local("thing__1")));
    }

    @Test
    void valueBoundBeforehandIsKept() throws Exception {
        TemplateBuilder b = new TemplateBuilder();
        IRI name = b.declare("name", NTEMPLATE.LITERAL_PLACEHOLDER);
        IRI st = b.statement(SUBJECT, PREDICATE, name);
        Template t = b.build();
        BindingContext context = BindingContext.forNewNanopub(t, NS, CREATOR);
        context.bind(name, lit("Bob"));

        TemplateMatch m = new TemplateMatcher(t).match(List.of(triple(SUBJECT, PREDICATE, lit("Alice")), triple(SUBJECT, PREDICATE, lit("Bob"))), context);
        assertEquals(triple(SUBJECT, PREDICATE, lit("Bob")), m.getMatches().get(0).triples().get(st));
        assertEquals(1, m.getUnusedTriples().size());
    }

    // ---------- the order of matching ----------

    @Test
    void earlierStatementCanTakeWhatALaterOneNeeds() throws Exception {
        // The statements are gone through in template order, and a statement does not give
        // back what it took. An optional statement that takes any label therefore leaves
        // nothing for the required one that follows, although the triple would fit there.
        TemplateBuilder b = new TemplateBuilder();
        IRI any = b.statement(b.declare("anything", NTEMPLATE.URI_PLACEHOLDER), RDFS.LABEL, b.declare("anyLabel", NTEMPLATE.LITERAL_PLACEHOLDER),
                NTEMPLATE.OPTIONAL_STATEMENT);
        IRI required = b.statement(SUBJECT, RDFS.LABEL, b.declare("name", NTEMPLATE.LITERAL_PLACEHOLDER));
        Template t = b.build();

        TemplateMatch m = match(t, triple(SUBJECT, RDFS.LABEL, lit("the subject")));
        assertEquals(any, m.getMatches().get(0).statement());
        assertEquals(List.of(required), m.getMissingStatements());
        assertEquals(List.of(), m.getUnusedTriples());
    }

    // ---------- grouped statements ----------

    // A group of "?thing a SomeClass" and, optionally, "?thing rdfs:label ?name".
    private static TemplateBuilder groupTemplate(IRI... groupTypes) throws Exception {
        TemplateBuilder b = new TemplateBuilder();
        IRI thing = b.declare("thing", NTEMPLATE.URI_PLACEHOLDER);
        IRI name = b.declare("name", NTEMPLATE.LITERAL_PLACEHOLDER);
        b.group("group", new Object[][]{{thing, RDF.TYPE, SOME_CLASS}, {thing, RDFS.LABEL, name, true}}, groupTypes);
        return b;
    }

    @Test
    void optionalMemberOfAGroupCanBeLeftOut() throws Exception {
        Template t = groupTemplate().build();
        IRI typeMember = local("group-a");
        IRI labelMember = local("group-b");

        TemplateMatch typeOnly = match(t, triple(ex("thing1"), RDF.TYPE, SOME_CLASS));
        assertTrue(typeOnly.isComplete());
        assertEquals(Set.of(typeMember), typeOnly.getMatches().get(0).triples().keySet());

        TemplateMatch both = match(t, triple(ex("thing1"), RDF.TYPE, SOME_CLASS), triple(ex("thing1"), RDFS.LABEL, lit("thing one")));
        assertTrue(both.isComplete());
        assertEquals(local("group"), both.getMatches().get(0).statement());
        assertEquals(Set.of(typeMember, labelMember), both.getMatches().get(0).triples().keySet());

        // The optional member alone is not the group:
        Statement labelTriple = triple(ex("thing1"), RDFS.LABEL, lit("thing one"));
        TemplateMatch labelOnly = match(t, labelTriple);
        assertEquals(List.of(local("group")), labelOnly.getMissingStatements());
        assertEquals(List.of(labelTriple), labelOnly.getUnusedTriples());
    }

    @Test
    @Timeout(10)
    void groupRepeatsWithItsOwnValues() throws Exception {
        Template t = groupTemplate(NTEMPLATE.REPEATABLE_STATEMENT).build();

        TemplateMatch m = match(t, triple(ex("thing1"), RDF.TYPE, SOME_CLASS), triple(ex("thing1"), RDFS.LABEL, lit("thing one")),
                triple(ex("thing2"), RDF.TYPE, SOME_CLASS));
        assertTrue(m.isComplete());
        assertEquals(2, m.getMatches().size());
        assertEquals(2, m.getMatches().get(0).triples().size());
        assertEquals(1, m.getMatches().get(1).triples().size());
        assertEquals(ex("thing2"), m.getContext().get(local("thing__1")));

        // With a member that may be left out, the repetitions still come to an end:
        TemplateMatch single = match(t, triple(ex("thing1"), RDF.TYPE, SOME_CLASS));
        assertTrue(single.isComplete());
        assertEquals(1, single.getMatches().size());
    }

    @Test
    void membersOfAGroupAreAssignedSoThatTheyAgree() throws Exception {
        // The first member could take either of two triples, but only one of them leads to
        // a triple for the second member (nanodash#505).
        TemplateBuilder b = new TemplateBuilder();
        IRI first = b.declare("first", NTEMPLATE.URI_PLACEHOLDER);
        IRI middle = b.declare("middle", NTEMPLATE.URI_PLACEHOLDER);
        IRI last = b.declare("last", NTEMPLATE.URI_PLACEHOLDER);
        b.group("chain", new Object[][]{{first, PREDICATE, middle}, {middle, RDFS.SEEALSO, last}});
        Template t = b.build();
        Statement deadEnd = triple(ex("x"), PREDICATE, ex("y1"));

        TemplateMatch m = match(t, deadEnd, triple(ex("x"), PREDICATE, ex("y2")), triple(ex("y2"), RDFS.SEEALSO, ex("z")));
        assertEquals(1, m.getMatches().size());
        assertEquals(ex("y2"), m.getContext().get(middle));
        assertEquals(ex("z"), m.getContext().get(last));
        assertEquals(List.of(deadEnd), m.getUnusedTriples());

        // Without a triple for the second member, the group does not match and nothing stays bound:
        TemplateMatch none = match(t, deadEnd);
        assertEquals(List.of(local("chain")), none.getMissingStatements());
        assertFalse(none.getContext().isBound(first));
        assertFalse(none.getContext().isBound(middle));
    }

    @Test
    void placeholderInTwoPositionsOfOneStatementHasOneValue() throws Exception {
        TemplateBuilder b = new TemplateBuilder();
        IRI thing = b.declare("thing", NTEMPLATE.URI_PLACEHOLDER);
        b.statement(thing, RDFS.SEEALSO, thing);
        Template t = b.build();

        assertTrue(match(t, triple(ex("a"), RDFS.SEEALSO, ex("a"))).isComplete());
        assertFalse(match(t, triple(ex("a"), RDFS.SEEALSO, ex("b"))).isComplete());
    }

    // ---------- values ----------

    @Test
    void valueThatCannotStandInAPositionDoesNotMatch() throws Exception {
        TemplateBuilder b = new TemplateBuilder();
        IRI code = b.declare("code", NTEMPLATE.LITERAL_PLACEHOLDER);
        b.add(code, NTEMPLATE.HAS_REGEX, "[0-9]+");
        IRI st = b.statement(SUBJECT, PREDICATE, code);
        Template t = b.build();
        Statement wrong = triple(SUBJECT, PREDICATE, lit("abc"));

        TemplateMatch m = match(t, wrong);
        assertEquals(List.of(st), m.getMissingStatements());
        assertEquals(List.of(wrong), m.getUnusedTriples());
        assertEquals(List.of(), m.getViolations());
    }

    @Test
    void valueThatBreaksAConstraintMatchesAndIsReported() throws Exception {
        TemplateBuilder b = new TemplateBuilder();
        IRI count = b.declare("count", NTEMPLATE.LITERAL_PLACEHOLDER);
        b.add(count, NTEMPLATE.HAS_DATATYPE, XSD.INT);
        b.statement(SUBJECT, PREDICATE, count);
        Template t = b.build();

        TemplateMatch m = match(t, triple(SUBJECT, PREDICATE, lit("5")));
        assertEquals(1, m.getMatches().size());
        assertEquals(List.of(), m.getUnusedTriples());
        assertEquals(List.of(Violation.Kind.DATATYPE), m.getViolations().stream().map(Violation::kind).toList());
        assertFalse(m.isComplete());
    }

    @Test
    void choiceAmongOtherPlaceholdersIsCheckedAtTheEnd() throws Exception {
        // The choice comes before the statement that gives the value it refers to.
        TemplateBuilder b = new TemplateBuilder();
        IRI defined = b.declare("defined", NTEMPLATE.URI_PLACEHOLDER);
        IRI choice = b.declare("choice", NTEMPLATE.RESTRICTED_CHOICE_PLACEHOLDER);
        b.add(choice, NTEMPLATE.POSSIBLE_VALUE, defined);
        b.statement(SUBJECT, PREDICATE, choice);
        b.statement(defined, RDF.TYPE, SOME_CLASS);
        Template t = b.build();

        assertTrue(match(t, triple(SUBJECT, PREDICATE, ex("a")), triple(ex("a"), RDF.TYPE, SOME_CLASS)).isComplete());
        TemplateMatch other = match(t, triple(SUBJECT, PREDICATE, ex("b")), triple(ex("a"), RDF.TYPE, SOME_CLASS));
        assertEquals(List.of(Violation.Kind.NOT_A_POSSIBLE_VALUE), other.getViolations().stream().map(Violation::kind).toList());
    }

    @Test
    void languageTagsAreMatchedAsThePlaceholderAllows() throws Exception {
        TemplateBuilder b = new TemplateBuilder();
        IRI comment = b.declare("comment", NTEMPLATE.LANGUAGE_TAGGED_LITERAL_PLACEHOLDER);
        b.add(comment, NTEMPLATE.POSSIBLE_LANGUAGE_TAG, "en").add(comment, NTEMPLATE.POSSIBLE_LANGUAGE_TAG, "de");
        b.statement(SUBJECT, RDFS.COMMENT, comment);
        Template t = b.build();

        assertTrue(match(t, triple(SUBJECT, RDFS.COMMENT, vf.createLiteral("Haus", "de"))).isComplete());
        assertEquals(1, match(t, triple(SUBJECT, RDFS.COMMENT, vf.createLiteral("maison", "fr"))).getUnusedTriples().size());
        assertEquals(1, match(t, triple(SUBJECT, RDFS.COMMENT, lit("house"))).getUnusedTriples().size());
    }

    @Test
    void sequenceElementsCountTheRepetitions() throws Exception {
        TemplateBuilder b = new TemplateBuilder();
        IRI element = b.declare("element", NTEMPLATE.SEQUENCE_ELEMENT_PLACEHOLDER);
        IRI item = b.declare("item", NTEMPLATE.URI_PLACEHOLDER);
        // The placeholder occurs in two statements, and still counts with each repetition:
        b.statement(SUBJECT, element, item, NTEMPLATE.REPEATABLE_STATEMENT);
        b.statement(SUBJECT, RDFS.SEEALSO, element, NTEMPLATE.OPTIONAL_STATEMENT);
        Template t = b.build();
        IRI first = iri(RDF.NAMESPACE + "_1");
        IRI second = iri(RDF.NAMESPACE + "_2");
        IRI fourth = iri(RDF.NAMESPACE + "_4");

        TemplateMatch m = match(t, triple(SUBJECT, first, ex("a")), triple(SUBJECT, second, ex("b")));
        assertTrue(m.isComplete());
        assertEquals(2, m.getMatches().size());
        assertEquals(ex("b"), m.getContext().get(local("item__1")));

        // A gap ends the sequence:
        Statement afterGap = triple(SUBJECT, fourth, ex("d"));
        TemplateMatch gap = match(t, triple(SUBJECT, first, ex("a")), triple(SUBJECT, second, ex("b")), afterGap);
        assertEquals(2, gap.getMatches().size());
        assertEquals(List.of(afterGap), gap.getUnusedTriples());
    }

    @Test
    void partsOfTheNanopublicationAreRecognised() throws Exception {
        TemplateBuilder b = new TemplateBuilder(NTEMPLATE.PROVENANCE_TEMPLATE);
        b.statement(NTEMPLATE.ASSERTION_PLACEHOLDER, PROV.WAS_ATTRIBUTED_TO, NTEMPLATE.CREATOR_PLACEHOLDER);
        IRI part = local("part");
        b.statement(NTEMPLATE.NANOPUB_PLACEHOLDER, RDFS.SEEALSO, part);
        Template t = b.build();

        TemplateMatch m = match(t, triple(iri(NS + "assertion"), PROV.WAS_ATTRIBUTED_TO, CREATOR), triple(iri(NS), RDFS.SEEALSO, iri(NS + "part")));
        assertTrue(m.isComplete());
        // Somebody else is not the creator, and another nanopublication's part not this one's:
        assertFalse(match(t, triple(iri(NS + "assertion"), PROV.WAS_ATTRIBUTED_TO, ex("somebody")), triple(iri(NS), RDFS.SEEALSO, iri(NS + "part"))).isComplete());
        assertFalse(match(t, triple(iri(NS + "assertion"), PROV.WAS_ATTRIBUTED_TO, CREATOR), triple(iri(NS), RDFS.SEEALSO, part)).isComplete());
    }

    // ---------- the matcher itself ----------

    @Test
    void templateWithoutStatementsLeavesAllTriples() throws Exception {
        Template t = new TemplateBuilder().build();
        Statement triple = triple(SUBJECT, PREDICATE, lit("x"));

        TemplateMatch m = match(t, triple);
        assertEquals(List.of(), m.getMatches());
        assertEquals(List.of(triple), m.getUnusedTriples());
        assertTrue(match(t).isComplete());
    }

    @Test
    void contextHasToBeForTheTemplate() throws Exception {
        Template t = new TemplateBuilder().build();
        Template other = new TemplateBuilder().build();
        TemplateMatcher matcher = new TemplateMatcher(t);

        assertSame(t, matcher.getTemplate());
        assertThrows(IllegalArgumentException.class, () -> matcher.match(new ArrayList<>(), BindingContext.forNewNanopub(other, NS, null)));
        assertThrows(NullPointerException.class, () -> matcher.match(new ArrayList<>(), null));
    }

    @Test
    void statementWithAnUndefinedPositionMatchesNothing() throws Exception {
        TemplateBuilder b = new TemplateBuilder();
        IRI st = local("st1");
        b.add(b.templateNode, NTEMPLATE.HAS_STATEMENT, st).add(st, RDF.PREDICATE, PREDICATE).add(st, RDF.OBJECT, lit("x"));
        Template t = b.build();

        TemplateMatch m = match(t, triple(SUBJECT, PREDICATE, lit("x")));
        assertEquals(List.of(st), m.getMissingStatements());
        assertEquals(1, m.getUnusedTriples().size());
    }

}
