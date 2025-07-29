package org.nanopub.op.topic;

import org.nanopub.Nanopub;

/**
 * Class that represents Disgenet topics.
 */
public class DisgenetTopics extends DefaultTopics {

    /**
     * Constructor for DisgenetTopics.
     */
    public DisgenetTopics() {
        super("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
    }

    /**
     * {@inheritDoc}
     * <p>
     * Get the topic for a given nanopublication.
     */
    @Override
    public String getTopic(Nanopub np) {
        String t = super.getTopic(np);
        t = t.replaceFirst("^http://rdf.disgenet.org/gene-disease-association.ttl#", "http://rdf.disgenet.org/resource/gda/");
        return t;
    }

}
