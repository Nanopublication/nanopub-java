package org.nanopub.op.topic;

import org.nanopub.Nanopub;
import org.nanopub.op.Topic.TopicHandler;

/**
 * A TopicHandler that returns the Nanopub URI as the topic.
 */
public class NoTopics implements TopicHandler {

    /**
     * {@inheritDoc}
     * <p>
     * Returns the Nanopub URI as the topic.
     */
    @Override
    public String getTopic(Nanopub np) {
        return np.getUri().stringValue();
    }

}
