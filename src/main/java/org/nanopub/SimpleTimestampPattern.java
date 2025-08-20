package org.nanopub;

import jakarta.xml.bind.DatatypeConverter;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.PROV;
import org.nanopub.vocabulary.PAV;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.DateFormat;
import java.util.Calendar;

/**
 * A simple timestamp pattern.
 */
public class SimpleTimestampPattern implements NanopubPattern {

    /**
     * {@inheritDoc}
     * <p>
     * Returns the name of the pattern
     */
    @Override
    public String getName() {
        return "Basic timestamp information";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean appliesTo(Nanopub nanopub) {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isCorrectlyUsedBy(Nanopub nanopub) {
        return getCreationTime(nanopub) != null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescriptionFor(Nanopub nanopub) {
        Calendar timestamp = getCreationTime(nanopub);
        if (timestamp == null) {
            return "No timestamp found";
        } else {
            return "Timestamp: " + DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG).format(timestamp.getTime());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public URL getPatternInfoUrl() throws MalformedURLException, URISyntaxException {
        return new URI("https://github.com/Nanopublication/nanopub-java/blob/master/src/main/java/org/nanopub/SimpleTimestampPattern.java").toURL();
    }

    /**
     * Returns the creation time of the nanopublication, if available.
     *
     * @param nanopub the nanopublication to check
     * @return the creation time as a Calendar object, or null if not found
     */
    public static Calendar getCreationTime(Nanopub nanopub) {
        String s = null;
        for (Statement st : nanopub.getPubinfo()) {
            if (!st.getSubject().equals(nanopub.getUri())) continue;
            if (!isCreationTimeProperty(st.getPredicate())) continue;
            if (!(st.getObject() instanceof Literal l)) continue;
            if (!l.getDatatype().equals(XSD_DATETIME)) continue;
            s = l.stringValue();
            break;
        }
        if (s == null) return null;
        return DatatypeConverter.parseDateTime(s);
    }

    /**
     * Constant <code>XSD_DATETIME</code>
     */
    public static final IRI XSD_DATETIME = SimpleValueFactory.getInstance().createIRI("http://www.w3.org/2001/XMLSchema#dateTime");

    /**
     * Checks if the given IRI is a property that indicates the creation time of a nanopublication.
     *
     * @param uri the IRI to check
     * @return true if the IRI is a creation time property, false otherwise
     */
    public static boolean isCreationTimeProperty(IRI uri) {
        return uri.equals(DCTERMS.CREATED) || uri.equals(PROV.GENERATED_AT_TIME) || uri.equals(PAV.CREATED_ON);
    }

}
