package org.nanopub;

import net.trustyuri.TrustyUriUtils;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.Test;
import org.nanopub.extra.security.NanopubSignatureElement;

import java.io.File;
import java.util.Objects;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class StripDownTest {

    @Test
    void stripDown() throws Exception {
        String outPath = "target/test-output/strip/";
        new File(outPath).mkdirs();
        File outFile = new File(outPath, "updated.trig");
        String inFiles = Objects.requireNonNull(this.getClass().getResource("/testsuite/valid/signed")).getPath();

        for (File testFile : Objects.requireNonNull(new File(inFiles).listFiles())) {
            // create signed nanopub file
            StripDown c = CliRunner.initJc(new StripDown(), new String[]{
                    "-o", outFile.getPath(),
                    testFile.getPath()});
            c.run();

            // read created nanopub from file
            NanopubImpl testNano = new NanopubImpl(outFile, RDFFormat.TRIG);
            assertFalse(TrustyUriUtils.isPotentialTrustyUri(testNano.getUri()));
            for (Statement statement : NanopubUtils.getStatements(testNano)) {
                assertThat(statement.getPredicate()).isNotEqualTo(NanopubSignatureElement.HAS_SIGNATURE_ELEMENT);
            }

            System.out.println("Successfully removed sig: " + testFile.getName());

            // delete target file if everything was fine
            outFile.delete();
        }
    }

    @Test
    void transformWithValidResource() {
        Resource resource = SimpleValueFactory.getInstance().createIRI("http://purl.org/np/RAYskLSM5x29icArnWvo9nVrIVEN2mfPoDq3TQSgm-9kk#Head");
        String artifact = "RAYskLSM5x29icArnWvo9nVrIVEN2mfPoDq3TQSgm-9kk";
        String replacement = "http://purl.org/nanopub/temp/" + Math.abs(new Random().nextInt()) + "/";

        IRI result = new StripDown().transform(resource, artifact, replacement);

        assertTrue(result.stringValue().startsWith("http://purl.org/nanopub/temp/"));
        assertTrue(result.stringValue().endsWith("/Head"));
    }

    @Test
    void transformWithNullResource() {
        Resource resource = null;
        String artifact = "RAdf9taM_Gyq2-WavUq3CxaVIvsHockMXzonj3W_igNhM";
        String replacement = "http://purl.org/nanopub/temp/" + Math.abs(new Random().nextInt()) + "/";
        IRI result = new StripDown().transform(resource, artifact, replacement);

        assertNull(result);
    }

    @Test
    void transformWithBlankNodeThrowsException() {
        Resource resource = SimpleValueFactory.getInstance().createBNode();
        String artifact = "RAdf9taM_Gyq2-WavUq3CxaVIvsHockMXzonj3W_igNhM";
        String replacement = "http://purl.org/nanopub/temp/" + Math.abs(new Random().nextInt()) + "/";

        assertThrows(RuntimeException.class, () -> new StripDown().transform(resource, artifact, replacement));
    }

    @Test
    void transformWithNoArtifactMatch() {
        Resource resource = SimpleValueFactory.getInstance().createIRI("http://purl.org/np/RAYskLSM5x29icArnWvo9nVrIVEN2mfPoDq3TQSgm-9kk#Head");
        String artifact = "artifact123"; // No match for this artifact
        String replacement = "http://purl.org/nanopub/temp/" + Math.abs(new Random().nextInt()) + "/";

        IRI result = new StripDown().transform(resource, artifact, replacement);
        assertEquals(resource.toString(), result.stringValue());
    }

}