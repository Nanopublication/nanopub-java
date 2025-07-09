package org.nanopub.extra.index;

import org.nanopub.MalformedNanopubException;
import org.nanopub.Nanopub;
import org.nanopub.NanopubPattern;

import java.net.MalformedURLException;
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

    @Override
    public String getName() {
        return "Nanopublication index";
    }

    @Override
    public boolean appliesTo(Nanopub nanopub) {
        return IndexUtils.isIndex(nanopub);
    }

    @Override
    public boolean isCorrectlyUsedBy(Nanopub nanopub) {
        try {
            new NanopubIndexImpl(nanopub);
        } catch (MalformedNanopubException ex) {
            return false;
        }
        return true;
    }

    @Override
    public String getDescriptionFor(Nanopub nanopub) {
        try {
            new NanopubIndexImpl(nanopub);
        } catch (MalformedNanopubException ex) {
            return ex.getMessage();
        }
        return "This is a valid nanopublication index.";
    }

    @Override
    public URL getPatternInfoUrl() throws MalformedURLException {
        return new URL("http://arxiv.org/abs/1411.2749");
    }

}
