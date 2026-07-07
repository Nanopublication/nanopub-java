package org.nanopub.extra.server;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.junit.jupiter.api.Test;
import org.nanopub.*;
import org.nanopub.extra.security.SignNanopub;
import org.nanopub.extra.security.TransformContext;
import org.nanopub.testsuite.NanopubTestSuite;
import org.nanopub.testsuite.SigningKeyPair;
import org.nanopub.vocabulary.NTEMPLATE;
import org.nanopub.vocabulary.NPX;

import java.io.File;
import java.net.URL;
import java.security.KeyPair;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.nanopub.extra.security.SignatureAlgorithm.RSA;
import static org.nanopub.utils.TestUtils.*;

class NanopubVerifierTest {

    private static final ValueFactory vf = SimpleValueFactory.getInstance();
    private static final IRI SIGNER_IRI = vf.createIRI("https://orcid.org/0000-0002-4808-1845");

    // -- helpers --

    private NanopubCreator baseCreator() throws NanopubAlreadyFinalizedException {
        NanopubCreator c = new NanopubCreator(vf.createIRI(NANOPUB_URI));
        c.addAssertionStatement(anyIri, anyIri, anyIri);
        c.addProvenanceStatement(anyIri, anyIri);
        c.addPubinfoStatement(anyIri, anyIri);
        return c;
    }

    private Nanopub signedNanopub(Nanopub np, IRI signerIri) throws Exception {
        SigningKeyPair signingKeyPair = NanopubTestSuite.getLatest().getSigningKey("rsa-key2");
        KeyPair key = SignNanopub.loadKey(signingKeyPair.getPrivateKeyFile().getPath(), RSA);
        TransformContext ctx = new TransformContext(RSA, key, signerIri, false, false, false);
        return SignNanopub.signAndTransform(np, ctx);
    }

    // -- checkTimestamp --

    @Test
    void checkTimestamp_recentTimestamp_noTimestampProblem() throws Exception {
        NanopubCreator c = baseCreator();
        c.addTimestampNow();
        Nanopub np = c.finalizeNanopub();

        NanopubVerifier verifier = new NanopubVerifier(np);
        verifier.verify();
        assertFalse(verifier.getIssues().contains("Nanopub has no creation time."));
        assertFalse(verifier.getIssues().contains("Nanopub creation time is in the future."));
        assertFalse(verifier.getIssues().contains("Nanopub creation time is older than one hour."));
    }

    @Test
    void checkTimestamp_noTimestamp_reportsProblem() throws Exception {
        Nanopub np = baseCreator().finalizeNanopub();

        NanopubVerifier verifier = new NanopubVerifier(np);
        verifier.verify();
        assertTrue(verifier.getIssues().contains("Nanopub has no creation time."));
    }

    @Test
    void checkTimestamp_futureTimestamp_reportsProblem() throws Exception {
        NanopubCreator c = baseCreator();
        c.addTimestamp(new Date(new Date().getTime() + 60 * 60 * 1000)); // 1h in the future
        Nanopub np = c.finalizeNanopub();

        NanopubVerifier verifier = new NanopubVerifier(np);
        verifier.verify();
        assertTrue(verifier.getIssues().contains("Nanopub creation time is in the future."));
    }

    @Test
    void checkTimestamp_oldTimestamp_reportsProblem() throws Exception {
        NanopubCreator c = baseCreator();
        c.addTimestamp(new Date(0)); // epoch = 1970, definitely > 1h ago
        Nanopub np = c.finalizeNanopub();

        NanopubVerifier verifier = new NanopubVerifier(np);
        verifier.verify();
        assertTrue(verifier.getIssues().contains("Nanopub creation time is older than one hour."));
    }

    // -- checkLabel --

    @Test
    void checkLabel_withLabel_noHasLabelProblem() throws Exception {
        NanopubCreator c = baseCreator();
        c.addTimestampNow();
        c.addPubinfoStatement(RDFS.LABEL, vf.createLiteral("My label")); // subject = nanopub URI
        Nanopub np = c.finalizeNanopub();

        NanopubVerifier verifier = new NanopubVerifier(np);
        verifier.verify();
        assertFalse(verifier.getIssues().contains("Nanopub has no label."));
    }

    @Test
    void checkLabel_withoutHasLabel_reportsProblem() throws Exception {
        NanopubCreator c = baseCreator();
        c.addTimestampNow();
        Nanopub np = c.finalizeNanopub();

        NanopubVerifier verifier = new NanopubVerifier(np);
        verifier.verify();
        assertTrue(verifier.getIssues().contains("Nanopub has no label."));
    }

    // -- checkType --

    @Test
    void checkType_withType_noHasTypeProblem() throws Exception {
        NanopubCreator c = baseCreator();
        c.addTimestampNow();
        c.addPubinfoStatement(RDF.TYPE, NPX.EXAMPLE_NANOPUB);
        Nanopub np = c.finalizeNanopub();

        NanopubVerifier verifier = new NanopubVerifier(np);
        verifier.verify();
        assertFalse(verifier.getIssues().contains("Nanopub has no types."));
    }

    @Test
    void checkType_withoutHasType_reportsProblem() throws Exception {
        NanopubCreator c = baseCreator();
        c.addTimestampNow();
        // Add a second assertion with a different predicate so getTypes() doesn't infer the unique predicate as a type
        c.addAssertionStatement(anyIri, vf.createIRI("https://example.org/otherPred"), anyIri);
        Nanopub np = c.finalizeNanopub();

        NanopubVerifier verifier = new NanopubVerifier(np);
        verifier.verify();
        assertTrue(verifier.getIssues().contains("Nanopub has no types."));
    }

    // -- checkTemplate --

    @Test
    void checkTemplate_withBothTemplates_noTemplateProblem() throws Exception {
        NanopubCreator c = baseCreator();
        c.addTimestampNow();
        c.addPubinfoStatement(NTEMPLATE.WAS_CREATED_FROM_TEMPLATE, anyIri);
        c.addPubinfoStatement(NTEMPLATE.WAS_CREATED_FROM_PROVENANCE_TEMPLATE, anyIri);
        Nanopub np = c.finalizeNanopub();

        NanopubVerifier verifier = new NanopubVerifier(np);
        verifier.verify();
        assertFalse(verifier.getIssues().contains("Nanopub has no assertion template."));
        assertFalse(verifier.getIssues().contains("Nanopub has no provenance template."));
    }

    @Test
    void checkTemplate_missingAssertionTemplate_reportsProblem() throws Exception {
        NanopubCreator c = baseCreator();
        c.addTimestampNow();
        c.addPubinfoStatement(NTEMPLATE.WAS_CREATED_FROM_PROVENANCE_TEMPLATE, anyIri);
        Nanopub np = c.finalizeNanopub();

        NanopubVerifier verifier = new NanopubVerifier(np);
        verifier.verify();
        assertTrue(verifier.getIssues().contains("Nanopub has no assertion template."));
        assertFalse(verifier.getIssues().contains("Nanopub has no provenance template."));
    }

    @Test
    void checkTemplate_missingProvenanceTemplate_reportsProblem() throws Exception {
        NanopubCreator c = baseCreator();
        c.addTimestampNow();
        c.addPubinfoStatement(NTEMPLATE.WAS_CREATED_FROM_TEMPLATE, anyIri);
        Nanopub np = c.finalizeNanopub();

        NanopubVerifier verifier = new NanopubVerifier(np);
        verifier.verify();
        assertFalse(verifier.getIssues().contains("Nanopub has no assertion template."));
        assertTrue(verifier.getIssues().contains("Nanopub has no provenance template."));
    }

    @Test
    void checkTemplate_missingBothTemplates_reportsBothProblems() throws Exception {
        NanopubCreator c = baseCreator();
        c.addTimestampNow();
        Nanopub np = c.finalizeNanopub();

        NanopubVerifier verifier = new NanopubVerifier(np);
        verifier.verify();
        assertTrue(verifier.getIssues().contains("Nanopub has no assertion template."));
        assertTrue(verifier.getIssues().contains("Nanopub has no provenance template."));
    }

    // -- checkSigner --

    @Test
    void checkSigner_noSignatureElement_reportsProblem() throws Exception {
        Nanopub np = baseCreator().finalizeNanopub();

        NanopubVerifier verifier = new NanopubVerifier(np);
        verifier.verify();
        assertTrue(verifier.getIssues().contains("Nanopub has no signature element."));
    }

    @Test
    void checkSigner_signerIsCreator_noSignerProblem() throws Exception {
        NanopubCreator c = baseCreator();
        c.addTimestampNow();
        c.addCreator(SIGNER_IRI);
        Nanopub np = signedNanopub(c.finalizeNanopub(), SIGNER_IRI);

        NanopubVerifier verifier = new NanopubVerifier(np);
        verifier.verify();
        assertFalse(verifier.getIssues().contains("Nanopub has no signer."));
        assertFalse(verifier.getIssues().contains("The signer is not a creator."));
        assertFalse(verifier.getIssues().contains("Nanopub has no signature element."));
    }

    @Test
    void checkSigner_signerNotCreator_reportsProblem() throws Exception {
        IRI otherCreator = vf.createIRI("https://orcid.org/0000-0000-0000-9999");
        NanopubCreator c = baseCreator();
        c.addTimestampNow();
        c.addCreator(otherCreator); // creator is different from signer
        Nanopub np = signedNanopub(c.finalizeNanopub(), SIGNER_IRI);

        NanopubVerifier verifier = new NanopubVerifier(np);
        verifier.verify();
        assertTrue(verifier.getIssues().contains("The signer is not a creator."));
    }

    // -- checkGraph --

    @Test
    void checkGraph_standardGraphUris_noGraphProblem() throws Exception {
        URL resource = getClass().getClassLoader().getResource("valid_graph.trig");
        Nanopub np = new NanopubImpl(new File(resource.toURI()));

        NanopubVerifier verifier = new NanopubVerifier(np);
        verifier.verify();
        assertTrue(verifier.getIssues().stream().noneMatch(p -> p.startsWith("Unexpected graph uri:")));
    }
}
