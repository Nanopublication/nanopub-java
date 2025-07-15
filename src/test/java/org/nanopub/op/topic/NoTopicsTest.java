package org.nanopub.op.topic;

import org.junit.jupiter.api.Test;
import org.nanopub.MalformedNanopubException;
import org.nanopub.Nanopub;
import org.nanopub.utils.TestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NoTopicsTest {

    @Test
    void getTopicReturnsUriStringValue() throws MalformedNanopubException {
        Nanopub nanopub = TestUtils.createNanopub(TestUtils.NANOPUB_URI);
        NoTopics handler = new NoTopics();
        String topic = handler.getTopic(nanopub);
        assertEquals(topic, TestUtils.NANOPUB_URI);
    }

    @Test
    void getTopicHandlesNullUriGracefully() {
        NoTopics handler = new NoTopics();
        assertThrows(NullPointerException.class, () -> handler.getTopic(null));
    }
}