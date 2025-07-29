package org.nanopub.op.topic;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.nanopub.Nanopub;
import org.nanopub.op.Topic.TopicHandler;

import java.util.HashMap;
import java.util.Map;

/**
 * Default topic handler.
 */
public class DefaultTopics implements TopicHandler {

    private Map<String, Boolean> ignore = new HashMap<>();

    /**
     * Create a new DefaultTopics instance.
     * This constructor allows specifying a list of property URIs to ignore when determining the topic.
     *
     * @param ignoreProperties A pipe-separated list of property URIs to ignore when determining the topic.
     */
    public DefaultTopics(String ignoreProperties) {
        if (ignoreProperties != null) {
            for (String s : ignoreProperties.trim().split("\\|")) {
                if (!s.isEmpty()) ignore.put(s, true);
            }
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Get the topic of a nanopublication.
     * This method analyzes the assertions in the nanopublication and determines the topic based on the most frequently occurring subject.
     */
    @Override
    public String getTopic(Nanopub np) {
        Map<Resource, Integer> resourceCount = new HashMap<>();
        for (Statement st : np.getAssertion()) {
            Resource subj = st.getSubject();
            if (subj.equals(np.getUri())) continue;
            if (ignore.containsKey(st.getPredicate().stringValue())) continue;
            if (!resourceCount.containsKey(subj)) resourceCount.put(subj, 0);
            resourceCount.put(subj, resourceCount.get(subj) + 1);
        }
        int max = 0;
        Resource topic = null;
        for (Resource r : resourceCount.keySet()) {
            int c = resourceCount.get(r);
            if (c > max) {
                topic = r;
                max = c;
            } else if (c == max) {
                topic = null;
            }
        }
        // TODO return null instead of "null" string?
        return String.valueOf(topic);
    }

}
