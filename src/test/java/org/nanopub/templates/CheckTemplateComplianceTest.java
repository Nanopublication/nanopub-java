package org.nanopub.templates;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.nanopub.Nanopub;
import org.nanopub.NanopubImpl;
import org.nanopub.NanopubUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.nanopub.templates.TemplateComplianceCheckerTest.QUERY;
import static org.nanopub.templates.TemplateComplianceCheckerTest.RESOURCES;
import static org.nanopub.templates.TemplateComplianceCheckerTest.VIEW;
import static org.nanopub.templates.TemplateComplianceCheckerTest.resourceFile;

/**
 * Tests for the {@link CheckTemplateCompliance} command.
 */
class CheckTemplateComplianceTest {

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
