package org.nanopub.extra.aida;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.nanopub.Nanopub;
import org.nanopub.NanopubPattern;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;

/**
 * A nanopublication pattern for AIDA nanopublications.
 */
public class AidaPattern implements NanopubPattern {

    /**
     * The prefix for AIDA URIs.
     */
    public static final String AIDA_URI_PREFIX = "http://purl.org/aida/";

    /**
     * The IRI for the AIDA sentence predicate.
     */
    public static final IRI AS_SENTENCE = SimpleValueFactory.getInstance().createIRI("http://purl.org/nanopub/x/asSentence");

    @Override
    public String getName() {
        return "AIDA nanopublication";
    }

    @Override
    public boolean appliesTo(Nanopub nanopub) {
        for (Statement st : nanopub.getAssertion()) {
            if (st.getPredicate().equals(AS_SENTENCE)) return true;
        }
        return false;
    }

    @Override
    public boolean isCorrectlyUsedBy(Nanopub nanopub) {
        IRI aidaUri = getAidaUri(nanopub);
        return aidaUri != null && aidaUri.toString().startsWith(AIDA_URI_PREFIX);
    }

    @Override
    public String getDescriptionFor(Nanopub nanopub) {
        if (isCorrectlyUsedBy(nanopub)) {
            return "AIDA sentence: " + getAidaText(getAidaUri(nanopub));
        } else {
            return "Not a valid AIDA nanopublication";
        }
    }

    @Override
    public URL getPatternInfoUrl() throws MalformedURLException {
        return new URL("https://github.com/tkuhn/aida");
    }

    /**
     * Get the AIDA URI from a nanopublication.
     *
     * @param nanopub the nanopublication to extract the AIDA URI from
     * @return the AIDA URI if it exists and is valid, null otherwise
     */
    public static IRI getAidaUri(Nanopub nanopub) {
        IRI aidaUri = null;
        boolean error = false;
        for (Statement st : nanopub.getAssertion()) {
            if (!st.getSubject().equals(nanopub.getAssertionUri())) continue;
            if (!st.getPredicate().equals(AS_SENTENCE)) continue;
            if (!(st.getObject() instanceof IRI)) {
                error = true;
                break;
            }
            if (aidaUri != null) {
                error = true;
                break;
            }
            aidaUri = (IRI) st.getObject();
        }
        if (error) return null;
        return aidaUri;
    }

    /**
     * Get the AIDA text from an AIDA URI.
     *
     * @param aidaUri the AIDA URI to extract the text from
     * @return the AIDA text if the URI is valid, null otherwise
     */
    public static String getAidaText(IRI aidaUri) {
        if (aidaUri == null) return null;
        if (!aidaUri.toString().startsWith(AIDA_URI_PREFIX)) return null;
        String aidaSentence = aidaUri.toString().substring(AIDA_URI_PREFIX.length());
        try {
            aidaSentence = URLDecoder.decode(aidaSentence, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            ex.printStackTrace();
        }
        return aidaSentence;
    }

}
