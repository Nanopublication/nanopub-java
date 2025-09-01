package org.nanopub.op.topic;

import org.junit.jupiter.api.Test;
import org.nanopub.MalformedNanopubException;
import org.nanopub.Nanopub;
import org.nanopub.NanopubAlreadyFinalizedException;
import org.nanopub.utils.TestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UriBaseTopicsTest {

    private final String nanopubUri = "https://w3id.org/np/RAtPRuqhsgemS9yuvMWSeSWUYNwpnU5dBkSbKK_1JwrUo";

    private final String topic = "https://w3id.org/np";

    @Test
    void getTopic() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        Nanopub nanopub = TestUtils.createNanopub(nanopubUri);
        UriBaseTopics handler = new UriBaseTopics();
        String topic = handler.getTopic(nanopub);
        assertEquals(this.topic, topic);
    }

}