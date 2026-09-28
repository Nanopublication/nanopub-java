package org.nanopub.extra.services;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Statement;
import org.nanopub.Nanopub;
import org.nanopub.NanopubPattern;
import org.nanopub.NanopubUtils;
import org.nanopub.vocabulary.KPXL_GRLC;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

/**
 * A nanopublication pattern for grlc query nanopublications, checking that their SPARQL parses.
 * <p>
 * A nanopublication cannot be edited after the fact, so a query whose SPARQL is broken is broken
 * permanently: it can never run, and the only remedy is publishing a corrected version.
 */
public class GrlcQueryPattern implements NanopubPattern {

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "grlc query nanopublication";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean appliesTo(Nanopub nanopub) {
        for (Statement st : NanopubUtils.getStatements(nanopub)) {
            if (st.getPredicate().equals(KPXL_GRLC.SPARQL) && st.getObject() instanceof Literal) return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isCorrectlyUsedBy(Nanopub nanopub) {
        return NanopubUtils.getInvalidSparqlStatements(nanopub).isEmpty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescriptionFor(Nanopub nanopub) {
        List<Statement> invalid = NanopubUtils.getInvalidSparqlStatements(nanopub);
        if (invalid.isEmpty()) {
            return "grlc query with valid SPARQL";
        }
        return NanopubUtils.describeInvalidSparql(invalid.getFirst());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public URL getPatternInfoUrl() throws MalformedURLException, URISyntaxException {
        return new URI(KPXL_GRLC.NAMESPACE).toURL();
    }

}
