package org.nanopub.extra.aida;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;

import org.nanopub.Nanopub;
import org.nanopub.NanopubPattern;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;

public class AidaPattern implements NanopubPattern {

	private static final long serialVersionUID = -164193908921728662L;

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
		URI aidaUri = getAidaUri(nanopub);
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

	public static URI getAidaUri(Nanopub nanopub) {
		URI aidaUri = null;
		boolean error = false;
		for (Statement st : nanopub.getAssertion()) {
			if (!st.getSubject().equals(nanopub.getAssertionUri())) continue;
			if (!st.getPredicate().equals(AS_SENTENCE)) continue;
			if (!(st.getObject() instanceof URI)) {
				error = true;
				break;
			}
			if (aidaUri != null) {
				error = true;
				break;
			}
			aidaUri = (URI) st.getObject();
		}
		if (error) return null;
		return aidaUri;
	}

	public static String getAidaText(URI aidaUri) {
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

	public static final String AIDA_URI_PREFIX = "http://purl.org/aida/";
	public static final URI AS_SENTENCE = new URIImpl("http://purl.org/nanopub/x/asSentence");

}
