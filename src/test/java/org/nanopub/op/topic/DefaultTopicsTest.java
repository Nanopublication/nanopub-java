package org.nanopub.op.topic;

import org.eclipse.rdf4j.model.IRI;
import org.junit.jupiter.api.Test;
import org.nanopub.MalformedNanopubException;
import org.nanopub.Nanopub;
import org.nanopub.NanopubAlreadyFinalizedException;
import org.nanopub.NanopubCreator;
import org.nanopub.utils.TestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.nanopub.utils.TestUtils.anyIri;
import static org.nanopub.utils.TestUtils.vf;

class DefaultTopicsTest {

    private final IRI anotherIri = vf.createIRI("http://knowledgepixels.com/nanopubIri#anotherIri");

    @Test
    void getTopicWithOneAssertion() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        Nanopub nanopub = TestUtils.createNanopub();
        DefaultTopics handler = new DefaultTopics(null);
        String topic = handler.getTopic(nanopub);
        assertEquals(topic, anyIri.stringValue());
    }

    @Test
    void getTopicWithMoreAssertions() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        NanopubCreator creator = TestUtils.getNanopubCreator();
        creator.addAssertionStatements(
                vf.createStatement(anyIri, anyIri, anyIri),
                vf.createStatement(anotherIri, anyIri, anyIri),
                vf.createStatement(anotherIri, anyIri, anotherIri)
        );
        creator.addProvenanceStatements(vf.createStatement(creator.getAssertionUri(), anyIri, anyIri));
        creator.addPubinfoStatements(vf.createStatement(creator.getNanopubUri(), anyIri, anyIri));

        Nanopub nanopub = creator.finalizeNanopub();
        DefaultTopics handler = new DefaultTopics(null);
        String topic = handler.getTopic(nanopub);
        assertEquals(topic, anotherIri.stringValue());
    }

    @Test
    void getTopicWithEqualNumberOfAssertions() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        NanopubCreator creator = TestUtils.getNanopubCreator();
        creator.addAssertionStatements(
                vf.createStatement(anyIri, anyIri, anyIri),
                vf.createStatement(anotherIri, anyIri, anyIri)
        );
        creator.addProvenanceStatements(vf.createStatement(creator.getAssertionUri(), anyIri, anyIri));
        creator.addPubinfoStatements(vf.createStatement(creator.getNanopubUri(), anyIri, anyIri));

        Nanopub nanopub = creator.finalizeNanopub();
        DefaultTopics handler = new DefaultTopics(null);
        String topic = handler.getTopic(nanopub);
        assertEquals("null", topic); // Expecting "null" because both subjects have equal frequency
    }

    @Test
    void getTopicWithIgnoredProperties() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        IRI ignoredProperty = vf.createIRI("http://knowledgepixels.com/nanopubIri#ignoredProperty");
        NanopubCreator creator = TestUtils.getNanopubCreator();
        creator.addAssertionStatements(
                vf.createStatement(anyIri, ignoredProperty, anyIri),
                vf.createStatement(anotherIri, anotherIri, anyIri)
        );
        creator.addProvenanceStatements(vf.createStatement(creator.getAssertionUri(), anyIri, anyIri));
        creator.addPubinfoStatements(vf.createStatement(creator.getNanopubUri(), anyIri, anyIri));

        Nanopub nanopub = creator.finalizeNanopub();
        DefaultTopics handler = new DefaultTopics(ignoredProperty.stringValue());
        String topic = handler.getTopic(nanopub);
        assertEquals(topic, anotherIri.stringValue());

        String ignoredProperties = ignoredProperty.stringValue() + "|" + anotherIri.stringValue();
        handler = new DefaultTopics(ignoredProperties);
        topic = handler.getTopic(nanopub);
        assertEquals("null", topic);
    }

}