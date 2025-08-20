package org.nanopub.op.topic;

import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.nanopub.Nanopub;

/**
 * Class that represents Disgenet topics.
 */
public class DisgenetTopics extends DefaultTopics {

    /**
     * Constructor for DisgenetTopics.
     */
    public DisgenetTopics() {
        super(String.valueOf(RDF.TYPE));
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
