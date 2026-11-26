package org.nanopub.templates;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.nanopub.Nanopub;
import org.nanopub.NanopubImpl;
import org.nanopub.NanopubUtils;
import org.nanopub.extra.server.GetNanopub;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.nanopub.templates.TemplateComplianceCheckerTest.QUERY;
import static org.nanopub.templates.TemplateComplianceCheckerTest.RESOURCES;
import static org.nanopub.templates.TemplateComplianceCheckerTest.VIEW;
import static org.nanopub.templates.TemplateComplianceCheckerTest.resourceFile;

/**
 * Tests for the {@link CheckTemplateCompliance} command.
 */
class CheckTemplateComplianceTest {

    private static final ValueFactory vf = SimpleValueFactory.getInstance();

    @TempDir
    Path tempDir;

    private final ByteArrayOutputStream output = new ByteArrayOutputStream();

    private int run(NanopubResolver resolver, List<String> files, List<String> templateFiles) throws Exception {
        CheckTemplateCompliance command = new CheckTemplateCompliance(files, templateFiles);
        command.setOutput(new PrintStream(output, true));
        command.setResolver(resolver);
        return command.run();
    }

    // The query nanopublication without its description, which its template requires.
    private String fileWithoutDescription() throws Exception {
        Nanopub np = new NanopubImpl(resourceFile(QUERY));
        List<Statement> statements = new ArrayList<>();
        for (Statement st : NanopubUtils.getStatements(np)) {
            if (!(st.getContext().equals(np.getAssertionUri()) && st.getPredicate().equals(DCTERMS.DESCRIPTION))) statements.add(st);
        }
        Path file = tempDir.resolve("changed.trig");
        Files.writeString(file, NanopubUtils.writeToString(new NanopubImpl(statements), RDFFormat.TRIG));
        return file.toString();
    }

    @Test
    void compliantNanopublicationsAreReportedAsSuch() throws Exception {
        int notCompliant = run(RESOURCES, List.of(resourceFile(QUERY).getPath(), resourceFile(VIEW).getPath()), List.of());
        assertEquals(0, notCompliant);
        String text = output.toString();
        assertTrue(text.contains("COMPLIES: https://w3id.org/np/" + QUERY), text);
        assertTrue(text.contains("COMPLIES: https://w3id.org/np/" + VIEW), text);
        assertTrue(text.contains("Summary: 2 checked; 2 comply; 0 do not comply"), text);
        assertFalse(text.contains("ERROR"), text);
    }

    @Test
    void nanopublicationThatDoesNotComplyIsCounted() throws Exception {
        int notCompliant = run(RESOURCES, List.of(resourceFile(QUERY).getPath(), fileWithoutDescription()), List.of());
        assertEquals(1, notCompliant);
        String text = output.toString();
        assertTrue(text.contains("DOES NOT COMPLY: https://w3id.org/np/" + QUERY), text);
        assertTrue(text.contains("- ERROR: Nothing matches the statement st2"), text);
        assertTrue(text.contains("Summary: 2 checked; 1 comply; 1 do not comply"), text);
    }

    @Test
    void templatesCanBeGivenAsFiles() throws Exception {
        List<String> templateFiles = new ArrayList<>();
        for (String code : List.of("RAEFAt-QcFK0ZhqfvlsmS10BnzGJA0xwOICZXkO-ai87k", "RA7lSq6MuK_TIC6JMSHvLtee3lpLoZDOqLJCLXevnrPoU",
                "RA0J4vUn_dekg-U1kK3AOEt02p9mT2WO03uGxLDec1jLw", "RAukAcWHRDlkqxk7H2XNSegc1WnHI569INvNr-xdptDGI")) {
            templateFiles.add(resourceFile(code).getPath());
        }
        // Nothing is retrieved from elsewhere:
        assertEquals(0, run(NanopubResolver.NONE, List.of(resourceFile(QUERY).getPath()), templateFiles));
        assertTrue(output.toString().contains("COMPLIES: "), output.toString());

        // Without the files the templates cannot be had, and the nanopublication does not pass:
        output.reset();
        assertEquals(1, run(NanopubResolver.NONE, List.of(resourceFile(QUERY).getPath()), List.of()));
        assertTrue(output.toString().contains("cannot be used: nanopublication could not be retrieved"), output.toString());
    }

    @Test
    void templateNamedByItsNodeIsRetrievedFromTheNetwork() throws Exception {
        // The query nanopublication, naming its assertion template by an IRI within the
        // template nanopublication, as nanopublications made from newer templates do.
        Nanopub np = new NanopubImpl(resourceFile(QUERY));
        IRI link = vf.createIRI("https://w3id.org/np/o/ntemplate/wasCreatedFromTemplate");
        List<Statement> statements = new ArrayList<>();
        for (Statement st : NanopubUtils.getStatements(np)) {
            if (st.getPredicate().equals(link)) {
                st = vf.createStatement(st.getSubject(), link, vf.createIRI(st.getObject().stringValue() + "/template"), st.getContext());
            }
            statements.add(st);
        }
        Path file = tempDir.resolve("node-link.trig");
        Files.writeString(file, NanopubUtils.writeToString(new NanopubImpl(statements), RDFFormat.TRIG));

        // No resolver is set, so the command asks the network, which is stood in for here:
        CheckTemplateCompliance command = new CheckTemplateCompliance(List.of(file.toString()), List.of());
        command.setOutput(new PrintStream(output, true));
        int notCompliant;
        try (MockedStatic<GetNanopub> network = mockStatic(GetNanopub.class)) {
            network.when(() -> GetNanopub.get(anyString())).thenAnswer(call -> {
                String id = call.getArgument(0);
                // As the network does, it only takes what ends with an artifact code:
                if (!id.matches("(.*[^A-Za-z0-9_-])?RA[A-Za-z0-9_-]{43}")) throw new IllegalArgumentException("Not a well-formed trusty URI");
                return RESOURCES.getNanopub(id);
            });
            notCompliant = command.run();
        }
        String text = output.toString();
        assertEquals(0, notCompliant, text);
        assertTrue(text.contains("COMPLIES: https://w3id.org/np/" + QUERY), text);
        assertFalse(text.contains("cannot be used"), text);
    }

    @Test
    void fileThatIsNoNanopublicationIsCounted() throws Exception {
        Path empty = tempDir.resolve("empty.trig");
        Files.writeString(empty, "");
        Path broken = tempDir.resolve("broken.trig");
        Files.writeString(broken, "this is not TriG {");

        assertEquals(2, run(RESOURCES, List.of(empty.toString(), broken.toString()), List.of()));
        String text = output.toString();
        assertTrue(text.contains("NO NANOPUB FOUND: " + empty), text);
        assertTrue(text.contains("INVALID NANOPUB: " + broken), text);
        assertTrue(text.contains("2 could not be read"), text);
    }

    @Test
    void unreadableTemplateFileIsAnError() {
        File missing = tempDir.resolve("no-such-template.trig").toFile();
        assertThrows(java.io.IOException.class, () -> run(RESOURCES, List.of(), List.of(missing.getPath())));
    }

}
