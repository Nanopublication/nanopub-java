package org.nanopub.op.topic;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.nanopub.Nanopub;
import org.nanopub.op.Topic.TopicHandler;

import java.util.*;

/**
 * This class implements a topic handler for Wikipathways.
 */
public class WikipathwaysTopics implements TopicHandler {

    /**
     * Returns the topic of a given Nanopub.
     *
     * @param np The Nanopub for which the topic is to be determined.
     * @return A string representing the topic of the Nanopub, formatted as
     */
    @Override
    public String getTopic(Nanopub np) {
        Map<Resource, Integer> resourceCount = new HashMap<>();
        List<String> organismNames = new ArrayList<>();
        List<String> pathwayIds = new ArrayList<>();
        for (Statement st : np.getAssertion()) {
            Resource subj = st.getSubject();
            if (subj.equals(np.getUri())) continue;
            String ps = st.getPredicate().stringValue();
            if (ps.equals(RDF.TYPE.stringValue())) continue;
            if (ps.equals("http://vocabularies.wikipathways.org/wp#pathwayOntologyTag")) continue;
            if (ps.equals("http://purl.org/dc/terms/isPartOf")) {
                pathwayIds.add(st.getObject().stringValue().replace("http://identifiers.org/wikipathways/", ""));
            }
            if (ps.equals("http://vocabularies.wikipathways.org/wp#organismName")) {
                organismNames.add(st.getObject().stringValue().replace(" ", "_"));
            }
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
        String pathways = "";
        if (!pathwayIds.isEmpty()) {
            Collections.sort(pathwayIds);
            for (String s : pathwayIds) {
                pathways += "|" + s;
            }
        }
        String organisms = "";
        if (!organismNames.isEmpty()) {
            Collections.sort(organismNames);
            for (String s : organismNames) {
                organisms += "|" + s;
            }
        }
        return topic + ":" + pathways + ":" + organisms;
    }

}
