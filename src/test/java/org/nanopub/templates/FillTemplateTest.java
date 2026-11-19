package org.nanopub.templates;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.nanopub.MultiNanopubRdfHandler;
import org.nanopub.Nanopub;
import org.nanopub.NanopubImpl;
import org.nanopub.NanopubUtils;
import org.nanopub.trusty.MakeTrustyNanopub;
import org.nanopub.vocabulary.NPX;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.nanopub.templates.TemplateComplianceCheckerTest.RESOURCES;
import static org.nanopub.templates.TemplateComplianceCheckerTest.resourceFile;

/**
 * Tests for the {@link FillTemplate} command.
 */
class FillTemplateTest {

    private static final String QUERY_TEMPLATE = "RAEFAt-QcFK0ZhqfvlsmS10BnzGJA0xwOICZXkO-ai87k";
    private static final String ME = "https://orcid.org/0000-0000-0000-0001";

    private static final String REQUEST = """
            {
              "template": "https://w3id.org/np/RAEFAt-QcFK0ZhqfvlsmS10BnzGJA0xwOICZXkO-ai87k",
              "param_query": "get-things",
              "param_title": "Get things",
              "param_description": "Returns three things.",
              "param_endpoint": "https://w3id.org/np/l/nanopub-query-1.1/repo/full",
              "param_license": "http://www.apache.org/licenses/LICENSE-2.0",
              "param_sparql": "select * where { ?s ?p ?o } limit 3"%s
            }
            """;

    @TempDir
    Path tempDir;

    private final ByteArrayOutputStream output = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errors = new ByteArrayOutputStream();

    private FillTemplate command(NanopubResolver resolver, List<String> requests, List<String> templateFiles) {
        FillTemplate command = new FillTemplate(requests, templateFiles);
        command.setOutput(output);
        command.setErrorOutput(new PrintStream(errors, true));
        command.setResolver(resolver);
        // Not the profile of whoever runs the tests:
        command.setProfileFile(tempDir.resolve("no-profile.yaml").toFile());
        return command;
    }

    private String requestFile(String name, String more) throws Exception {
        Path file = tempDir.resolve(name);
        Files.writeString(file, REQUEST.formatted(more));
        return file.toString();
    }

    private static String creatorOf(Nanopub np) {
        return np.getPubinfo().stream().filter(st -> st.getPredicate().equals(DCTERMS.CREATOR)).map(st -> st.getObject().stringValue()).findFirst().orElse(null);
    }

    @Test
    void requestFileGivesANanopublication() throws Exception {
        int failed = command(RESOURCES, List.of(requestFile("request.json", ", \"creator\": \"" + ME + "\"")), List.of()).run();

        assertEquals(0, failed, errors.toString());
        Nanopub np = new NanopubImpl(output.toString(), RDFFormat.TRIG);
        assertEquals(6, np.getAssertion().size());
        assertEquals(ME, creatorOf(np));
        assertTrue(new TemplateComplianceChecker(RESOURCES).check(np).isCompliant());
        assertEquals("", errors.toString());
    }

    @Test
    void linkAndSeveralRequestsAreTaken() throws Exception {
        String link = "https://nanodash.example.org/publish?template=https://w3id.org/np/" + QUERY_TEMPLATE
                + "&param_query=other&param_title=Other&param_description=Another+one.&param_endpoint=https://example.org/sparql"
                + "&param_license=http://www.apache.org/licenses/LICENSE-2.0&param_sparql=select+*+where+%7B%7D";
        FillTemplate command = command(RESOURCES, List.of(requestFile("request.json", ""), link), List.of());
        command.setCreator(ME);

        assertEquals(0, command.run(), errors.toString());
        File out = tempDir.resolve("out.trig").toFile();
        Files.writeString(out.toPath(), output.toString());
        List<Nanopub> nanopubs = new ArrayList<>();
        MultiNanopubRdfHandler.process(out, nanopubs::add);
        assertEquals(2, nanopubs.size());
        assertEquals(ME, creatorOf(nanopubs.get(1)));
    }

    @Test
    void creatorOfTheCommandLineOverridesTheOneOfTheRequest() throws Exception {
        FillTemplate command = command(RESOURCES, List.of(requestFile("request.json", ", \"creator\": \"" + ME + "\"")), List.of());
        command.setCreator("https://orcid.org/0000-0000-0000-0002");
        assertEquals(0, command.run(), errors.toString());
        assertEquals("https://orcid.org/0000-0000-0000-0002", creatorOf(new NanopubImpl(output.toString(), RDFFormat.TRIG)));
    }

    @Test
    void creatorIsTakenFromTheProfile() throws Exception {
        Path profile = tempDir.resolve("profile.yaml");
        Files.writeString(profile, "orcid_id: https://orcid.org/0000-0000-0000-0003\n");
        FillTemplate command = command(RESOURCES, List.of(requestFile("request.json", "")), List.of());
        command.setProfileFile(profile.toFile());
        assertEquals(0, command.run(), errors.toString());
        assertEquals("https://orcid.org/0000-0000-0000-0003", creatorOf(new NanopubImpl(output.toString(), RDFFormat.TRIG)));
    }

    @Test
    void problemsAreListedAndNothingIsWritten() throws Exception {
        String bad = "template=https://w3id.org/np/" + QUERY_TEMPLATE + "&param_query=get-things&param_titel=x";
        int failed = command(RESOURCES, List.of(requestFile("request.json", ", \"creator\": \"" + ME + "\""), bad), List.of()).run();

        assertEquals(1, failed);
        assertEquals("", output.toString());
        String text = errors.toString();
        assertTrue(text.contains("CANNOT FILL: request 2"), text);
        assertTrue(text.contains("- No placeholder of the assertion template answers to 'param_titel'; its placeholders are: description, endpoint, license, query, sparql, title"), text);
        assertTrue(text.contains("- The statement st1 of the assertion template lacks a value for 'param_title' (title of the query)"), text);
        assertTrue(text.contains("lacks the creator (creator)"), text);
    }

    // ---------- the request of an existing nanopublication ----------

    private static final String QUERY_URI = "https://w3id.org/np/" + TemplateComplianceCheckerTest.QUERY;

    private FillTemplate requestOf(String nanopub) {
        FillTemplate command = command(RESOURCES, List.of(), List.of());
        command.setRequestOf(nanopub);
        return command;
    }

    @Test
    void requestOfAPublishedNanopublicationIsPrinted() throws Exception {
        assertEquals(0, requestOf(QUERY_URI).run(), errors.toString());

        FillRequest request = FillRequest.fromJson(output.toString());
        assertEquals("https://w3id.org/np/" + QUERY_TEMPLATE, request.getTemplate());
        assertEquals("html-test", request.getValues().get("query").text());
        assertEquals("HTML test", request.getValues().get("title").text());
        assertNull(request.getMode());
        assertEquals("", errors.toString());
    }

    @Test
    void printedRequestMakesANewVersion() throws Exception {
        assertEquals(0, requestOf(resourceFile(TemplateComplianceCheckerTest.QUERY).getPath()).run(), errors.toString());
        // Change the title and say what the new nanopublication is a version of.
        String edited = output.toString().replace("\"HTML test\"", "\"HTML test, improved\"")
                .replaceFirst("\\{", "{\n  \"supersede\": \"" + QUERY_URI + "\",");
        Path file = tempDir.resolve("edited.json");
        Files.writeString(file, edited);
        output.reset();

        assertEquals(0, command(RESOURCES, List.of(file.toString()), List.of()).run(), errors.toString());

        Nanopub np = new NanopubImpl(output.toString(), RDFFormat.TRIG);
        assertTrue(np.getAssertion().stream().anyMatch(st -> st.getObject().stringValue().equals("HTML test, improved")));
        assertEquals(List.of(QUERY_URI), np.getPubinfo().stream().filter(st -> st.getPredicate().equals(NPX.SUPERSEDES)).map(st -> st.getObject().stringValue()).toList());
        assertTrue(new TemplateComplianceChecker(RESOURCES).check(np).isCompliant());
    }

    @Test
    void requestOfANewVersionDoesNotCarryTheLinkToTheOldOne() throws Exception {
        // A second version, as it would be published:
        FillRequest second = FillRequest.of(new TemplateComplianceChecker(RESOURCES).check(new NanopubImpl(resourceFile(TemplateComplianceCheckerTest.QUERY))));
        second.setSource(FillRequest.Mode.SUPERSEDE, QUERY_URI);
        Nanopub published = MakeTrustyNanopub.transform(new TemplateFiller(RESOURCES).fill(second));
        Path file = tempDir.resolve("second.trig");
        Files.writeString(file, NanopubUtils.writeToString(published, RDFFormat.TRIG));

        assertEquals(0, requestOf(file.toString()).run(), errors.toString());

        // Neither the link nor its template: adding 'supersede' is what makes a third version.
        String json = output.toString();
        assertFalse(json.contains(TemplateComplianceCheckerTest.QUERY), json);
        assertFalse(json.contains(TemplateFiller.SUPERSEDES_PUBINFO_TEMPLATE), json);
        assertEquals(2, FillRequest.fromJson(json).getPubinfoTemplates().size());
    }

    @Test
    void requestThatCannotBeFilledAgainIsPrintedWithAWarning() throws Exception {
        // The templates the request needs are there for the check, but not all for a fill.
        NanopubResolver withoutLicense = id -> id.contains("RACJ58Gvyn91LqCKIO9zu1eijDQIeEff28iyDrJgjSJF8") ? null : RESOURCES.getNanopub(id);
        FillTemplate command = command(withoutLicense, List.of(), List.of());
        command.setRequestOf(QUERY_URI);

        assertEquals(0, command.run());

        assertEquals("html-test", FillRequest.fromJson(output.toString()).getValues().get("query").text());
        String text = errors.toString();
        assertTrue(text.contains("WARNING: this request cannot be filled as it is"), text);
        assertTrue(text.contains("RACJ58Gvyn91LqCKIO9zu1eijDQIeEff28iyDrJgjSJF8> cannot be used"), text);
    }

    @Test
    void noRequestForANanopublicationThatDoesNotComply() throws Exception {
        // The query without its description, which its template requires.
        Nanopub np = new NanopubImpl(resourceFile(TemplateComplianceCheckerTest.QUERY));
        List<Statement> statements = new ArrayList<>();
        for (Statement st : NanopubUtils.getStatements(np)) {
            if (!(st.getContext().equals(np.getAssertionUri()) && st.getPredicate().equals(DCTERMS.DESCRIPTION))) statements.add(st);
        }
        Path file = tempDir.resolve("changed.trig");
        Files.writeString(file, NanopubUtils.writeToString(new NanopubImpl(statements), RDFFormat.TRIG));

        assertEquals(1, requestOf(file.toString()).run());

        assertEquals("", output.toString());
        String text = errors.toString();
        assertTrue(text.startsWith("NO REQUEST: " + QUERY_URI + " does not comply with its templates"), text);
        assertTrue(text.contains("- ERROR: Nothing matches the statement st2"), text);
    }

    @Test
    void nanopublicationToReadTheRequestOfHasToBeThere() throws Exception {
        assertEquals(1, requestOf("https://w3id.org/np/RANoSuchNanopubNoSuchNanopubNoSuchNanopub000").run());
        assertTrue(errors.toString().startsWith("NO REQUEST: there is no file"), errors.toString());

        Path two = tempDir.resolve("two.trig");
        Files.writeString(two, Files.readString(resourceFile(TemplateComplianceCheckerTest.QUERY).toPath())
                + "\n" + Files.readString(resourceFile(TemplateComplianceCheckerTest.VIEW).toPath()));
        errors.reset();
        assertEquals(1, requestOf(two.toString()).run());
        assertTrue(errors.toString().contains("holds 2 nanopublications; one is needed"), errors.toString());
    }

    @Test
    void eitherRequestsOrARequestToPrint() throws Exception {
        FillTemplate both = command(RESOURCES, List.of("template=x"), List.of());
        both.setRequestOf(QUERY_URI);
        assertEquals(1, both.run());
        assertTrue(errors.toString().contains("Give either requests to fill or --request-of, not both"), errors.toString());

        errors.reset();
        assertEquals(1, command(RESOURCES, List.of(), List.of()).run());
        assertTrue(errors.toString().startsWith("No request given"), errors.toString());
    }

    @Test
    void templateFilesAreUsedBeforeAnythingIsRetrieved() throws Exception {
        List<String> asked = new ArrayList<>();
        NanopubResolver resolver = id -> {
            asked.add(id);
            return id.contains(QUERY_TEMPLATE) ? null : RESOURCES.getNanopub(id);
        };
        List<String> requests = List.of(requestFile("request.json", ", \"creator\": \"" + ME + "\""));

        assertEquals(1, command(resolver, requests, List.of()).run());
        assertTrue(errors.toString().contains("cannot be used: nanopublication could not be retrieved"), errors.toString());

        asked.clear();
        assertEquals(0, command(resolver, requests, List.of(resourceFile(QUERY_TEMPLATE).getPath())).run(), errors.toString());
        assertFalse(asked.stream().anyMatch(id -> id.contains(QUERY_TEMPLATE)));
    }

}
