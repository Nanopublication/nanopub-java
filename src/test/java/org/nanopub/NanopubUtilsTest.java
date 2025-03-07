package org.nanopub;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Statements;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nanopub.extra.index.NanopubIndex;

import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;

class NanopubUtilsTest {

    private final ValueFactory vf = SimpleValueFactory.getInstance();
    private final IRI anyIri = vf.createIRI("http://knowledgepixels.com/nanopubIri#any");


    @BeforeEach
    void setUp() {
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void getDefaultNamespaces() {
        assertThat(NanopubUtils.getDefaultNamespaces()).isNotEmpty();
    }

    @Test
    void getStatementsMinimal() throws MalformedNanopubException {
        NanopubCreator creator = new NanopubCreator(vf.createIRI("http://knowledgepixels.com/nanopubIri#title"));

        // Create valid nanopub
        Statement assertionStatement = vf.createStatement(anyIri, anyIri, anyIri);
        creator.addAssertionStatements(assertionStatement);

        Statement provenanceStatement = vf.createStatement(
                creator.getAssertionUri(),
                anyIri,
                anyIri);
        creator.addProvenanceStatements(provenanceStatement);

        Statement pubinfoStatement = vf.createStatement(creator.getNanopubUri(), anyIri, anyIri);
        creator.addPubinfoStatements(pubinfoStatement);

        Nanopub nanopub = creator.finalizeNanopub();

        // Check that getStatements returns the expected Statements
        assertThat(NanopubUtils.getStatements(nanopub).contains(provenanceStatement));
        assertThat(NanopubUtils.getStatements(nanopub).contains(assertionStatement));
        assertThat(NanopubUtils.getStatements(nanopub).contains(pubinfoStatement));

        // there are 4 header statements, we do not check them here
        assertThat(NanopubUtils.getStatements(nanopub).size()).isEqualTo(7);
    }

    @Test
    void writeToStream() {
    }

    @Test
    void writeToString() {
    }

    @Test
    void propagateToHandler() {
    }

    @Test
    void getParser() {
    }

    @Test
    void getUsedPrefixes() {
    }

    @Test
    void getLabel() {
    }

    @Test
    void getDescription() {
    }

    @Test
    void getTypes() {
    }

    @Test
    void updateXorChecksum() {
    }

    @Test
    void getHttpClient() {
    }
}