package org.nanopub.templates;

import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.nanopub.MultiNanopubRdfHandler;
import org.nanopub.Nanopub;
import org.nanopub.NanopubImpl;

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
