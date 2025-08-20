package org.nanopub.op.topic;

import org.junit.jupiter.api.Test;
import org.nanopub.MalformedNanopubException;
import org.nanopub.Nanopub;
import org.nanopub.utils.TestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UriTailTopicsTest {

    private final String nanopubUri = "https://w3id.org/np/RAtPRuqhsgemS9yuvMWSeSWUYNwpnU5dBkSbKK_1JwrUo";

    private final String topic = "np";

    @Test
    void getTopic() throws MalformedNanopubException {
        Nanopub nanopub = TestUtils.createNanopub(nanopubUri);
        UriTailTopics handler = new UriTailTopics();
        String topic = handler.getTopic(nanopub);
        assertEquals(this.topic, topic);
    }

}