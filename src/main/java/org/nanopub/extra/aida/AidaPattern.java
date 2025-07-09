package org.nanopub.extra.aida;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.nanopub.Nanopub;
import org.nanopub.NanopubPattern;

public class AidaPattern implements NanopubPattern {

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

	public static final String AIDA_URI_PREFIX = "http://purl.org/aida/";
	public static final IRI AS_SENTENCE = SimpleValueFactory.getInstance().createIRI("http://purl.org/nanopub/x/asSentence");

}
