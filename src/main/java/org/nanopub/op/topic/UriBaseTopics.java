package org.nanopub.op.topic;

import org.nanopub.Nanopub;
import org.nanopub.op.Topic.TopicHandler;

/**
 * A topic handler that generates a topic based on the base URI of the nanopub.
 */
public class UriBaseTopics implements TopicHandler {

    /**
     * Returns the topic for a nanopublication based on its URI.
     *
     * @param np The nanopublication for which to get the topic.
     * @return The topic string derived from the nanopub's URI.
     */
    @Override
    public String getTopic(Nanopub np) {
        String s = np.getUri().stringValue();
        s = s.replaceFirst("RA.{43}$", "");
        s = s.replaceFirst("#.*$", "");
        s = s.replaceFirst("[\\./]$", "");
        return s;
    }

}
