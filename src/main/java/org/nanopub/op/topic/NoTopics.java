package org.nanopub.op.topic;

import org.nanopub.Nanopub;
import org.nanopub.op.Topic.TopicHandler;

/**
 * A TopicHandler that returns the Nanopub URI as the topic.
 */
public class NoTopics implements TopicHandler {

    /**
     * Returns the Nanopub URI as the topic.
     *
     * @param np The nanopublication for which to get the topic.
     * @return The URI of the nanopublication as a string.
     */
    @Override
    public String getTopic(Nanopub np) {
        return np.getUri().stringValue();
    }

}
