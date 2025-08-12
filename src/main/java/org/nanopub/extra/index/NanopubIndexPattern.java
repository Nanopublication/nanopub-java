package org.nanopub.extra.index;

import org.nanopub.MalformedNanopubException;
import org.nanopub.Nanopub;
import org.nanopub.NanopubPattern;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * A pattern for nanopublication indexes.
 */
public class NanopubIndexPattern implements NanopubPattern {

    /**
     * Default constructor.
     */
    public NanopubIndexPattern() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "Nanopublication index";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean appliesTo(Nanopub nanopub) {
        return IndexUtils.isIndex(nanopub);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isCorrectlyUsedBy(Nanopub nanopub) {
        try {
            new NanopubIndexImpl(nanopub);
        } catch (MalformedNanopubException ex) {
            return false;
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescriptionFor(Nanopub nanopub) {
        try {
            new NanopubIndexImpl(nanopub);
        } catch (MalformedNanopubException ex) {
            return ex.getMessage();
        }
        return "This is a valid nanopublication index.";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public URL getPatternInfoUrl() throws MalformedURLException, URISyntaxException {
        return new URI("http://arxiv.org/abs/1411.2749").toURL();
    }

}
