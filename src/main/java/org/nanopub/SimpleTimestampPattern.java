package org.nanopub;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.util.Calendar;

import jakarta.xml.bind.DatatypeConverter;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

public class SimpleTimestampPattern implements NanopubPattern {

	@Override
	public String getName() {
		return "Basic timestamp information";
	}

	@Override
	public boolean appliesTo(Nanopub nanopub) {
		return true;
	}

	@Override
	public boolean isCorrectlyUsedBy(Nanopub nanopub) {
		return getCreationTime(nanopub) != null;
	}

	@Override
	public String getDescriptionFor(Nanopub nanopub) {
		Calendar timestamp = getCreationTime(nanopub);
		if (timestamp == null) {
			return "No timestamp found";
		} else {
			return "Timestamp: " + DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG).format(timestamp.getTime());
		}
	}

	@Override
	public URL getPatternInfoUrl() throws MalformedURLException {
		return new URL("https://github.com/Nanopublication/nanopub-java/blob/master/src/main/java/org/nanopub/SimpleTimestampPattern.java");
	}

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

	public static final IRI XSD_DATETIME = SimpleValueFactory.getInstance().createIRI("http://www.w3.org/2001/XMLSchema#dateTime");

	public static final IRI DCT_CREATED = SimpleValueFactory.getInstance().createIRI("http://purl.org/dc/terms/created");
	public static final IRI PROV_GENERATEDATTIME = SimpleValueFactory.getInstance().createIRI("http://www.w3.org/ns/prov#generatedAtTime");
	public static final IRI PAV_CREATEDON = SimpleValueFactory.getInstance().createIRI("http://purl.org/pav/createdOn");

	public static boolean isCreationTimeProperty(IRI uri) {
		return uri.equals(DCT_CREATED) || uri.equals(PROV_GENERATEDATTIME) || uri.equals(PAV_CREATEDON);
	}

}
