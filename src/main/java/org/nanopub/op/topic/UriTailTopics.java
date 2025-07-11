package org.nanopub.op.topic;

import org.nanopub.Nanopub;
import org.nanopub.op.Topic.TopicHandler;

/**
 * A TopicHandler that extracts the tail of the URI of a Nanopub.
 */
public class UriTailTopics implements TopicHandler {

    /**
     * Returns the topic of a Nanopub by extracting the tail of its URI.
     *
     * @param np the Nanopub for which to get the topic
     * @return the topic as a String
     */
    @Override
    public String getTopic(Nanopub np) {
        String s = np.getUri().stringValue();
        s = s.replaceFirst("RA.{43}$", "");
        s = s.replaceFirst("#.*$", "");
        s = s.replaceFirst("[\\./]$", "");
        s = s.replaceFirst("^.*/([^/]*)$", "$1");
        return s;
    }

}
