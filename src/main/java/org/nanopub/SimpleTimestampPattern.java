package org.nanopub;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.util.Calendar;

import javax.xml.bind.DatatypeConverter;

import org.openrdf.model.Literal;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;

public class SimpleTimestampPattern implements NanopubPattern {

	private static final long serialVersionUID = -3210304976446322675L;

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
			if (!(st.getObject() instanceof Literal)) continue;
			Literal l = (Literal) st.getObject();
			if (!l.getDatatype().equals(XSD_DATETIME)) continue;
			s = l.stringValue();
			break;
		}
		if (s == null) return null;
		return DatatypeConverter.parseDateTime(s);
	}

	public static final URI XSD_DATETIME = new URIImpl("http://www.w3.org/2001/XMLSchema#dateTime");

	public static final URI DCT_CREATED = new URIImpl("http://purl.org/dc/terms/created");
	public static final URI PROV_GENERATEDATTIME = new URIImpl("http://www.w3.org/ns/prov#generatedAtTime");
	public static final URI PAV_CREATEDON = new URIImpl("http://purl.org/pav/createdOn");

	public static boolean isCreationTimeProperty(URI uri) {
		return uri.equals(DCT_CREATED) || uri.equals(PROV_GENERATEDATTIME) || uri.equals(PAV_CREATEDON);
	}

}
