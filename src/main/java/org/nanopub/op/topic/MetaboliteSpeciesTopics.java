package org.nanopub.op.topic;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.nanopub.Nanopub;
import org.nanopub.op.Topic.TopicHandler;

/**
 * This class handles topics for nanopubs related to metabolite species.
 */
public class MetaboliteSpeciesTopics implements TopicHandler {

    /**
     * {@inheritDoc}
     * <p>
     * Returns the topic for a given nanopub.
     */
    @Override
    public String getTopic(Nanopub np) {
        return getPart1(np) + ">" + ">" + getPart2(np);
    }

    private String getPart1(Nanopub np) {
        for (Statement st : np.getAssertion()) {
            if (st.getPredicate().stringValue().equals("http://www.wikidata.org/prop/direct/P703")) {
                return st.getSubject().stringValue() + ">" + st.getObject().stringValue();
            }
        }
        return null;
    }

    private String getPart2(Nanopub np) {
        for (Statement st : np.getProvenance()) {
            if (st.getPredicate().stringValue().equals("http://semanticscience.org/resource/SIO_000253")) {
                if (!(st.getObject() instanceof IRI)) continue;
                if (st.getObject().stringValue().equals("http://www.wikidata.org/entity/Q2013")) continue;
                return st.getObject().stringValue();
            }
        }
        return null;
    }

}
