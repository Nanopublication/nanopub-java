package org.nanopub;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;

public class SimpleCreatorPattern implements NanopubPattern {

	private static final long serialVersionUID = -3210304976446322675L;

	@Override
	public String getName() {
		return "Basic creator information";
	}

	@Override
	public boolean appliesTo(Nanopub nanopub) {
		return true;
	}

	@Override
	public boolean isCorrectlyUsedBy(Nanopub nanopub) {
		return !getCreators(nanopub).isEmpty() || !getAuthors(nanopub).isEmpty();
	}

	@Override
	public String getDescriptionFor(Nanopub nanopub) {
		Set<URI> authors = getAuthors(nanopub);
		Set<URI> creators = getCreators(nanopub);
		if (authors.isEmpty() && creators.isEmpty()) {
			return "No authors or creators found";
		} else {
			String s;
			if (authors.size() == 1) {
				s = "1 author; ";
			} else {
				s = authors.size() + " authors; ";
			}
			if (creators.size() == 1) {
				s += "1 creator";
			} else {
				s = creators.size() + " creator";
			}
			return s;
		}
	}

	@Override
	public URL getPatternInfoUrl() throws MalformedURLException {
		return new URL("https://github.com/Nanopublication/nanopub-java/blob/master/src/main/java/org/nanopub/SimpleCreatorPattern.java");
	}

	public static Set<URI> getAuthors(Nanopub nanopub) {
		Set<URI> authors = new HashSet<>();
		for (Statement st : nanopub.getPubinfo()) {
			if (!st.getSubject().equals(nanopub.getUri())) continue;
			if (!isAuthorProperty(st.getPredicate())) continue;
			if (!(st.getObject() instanceof URI)) continue;
			authors.add((URI) st.getObject());
		}
		return authors;
	}

	public static Set<URI> getCreators(Nanopub nanopub) {
		Set<URI> creators = new HashSet<>();
		for (Statement st : nanopub.getPubinfo()) {
			if (!st.getSubject().equals(nanopub.getUri())) continue;
			if (!isCreatorProperty(st.getPredicate())) continue;
			if (!(st.getObject() instanceof URI)) continue;
			creators.add((URI) st.getObject());
		}
		return creators;
	}

	public static final URI PAV_CREATEDBY = new URIImpl("http://purl.org/pav/createdBy");
	public static final URI PAV_CREATEDBY_1 = new URIImpl("http://swan.mindinformatics.org/ontologies/1.2/pav/createdBy");
	public static final URI DCT_CREATOR = new URIImpl("http://purl.org/dc/terms/creator");
	public static final URI DCE_CREATOR = new URIImpl("http://purl.org/dc/elements/1.1/creator");
	public static final URI PROV_WASATTRIBUTEDTO = new URIImpl("http://www.w3.org/ns/prov#wasAttributedTo");

	public static final URI PAV_AUTHOREDBY = new URIImpl("http://purl.org/pav/authoredBy");
	public static final URI PAV_AUTHOREDBY_1 = new URIImpl("http://swan.mindinformatics.org/ontologies/1.2/pav/authoredBy");

	public static boolean isCreatorProperty(URI uri) {
		return uri.equals(PAV_CREATEDBY) || uri.equals(PAV_CREATEDBY_1) || uri.equals(DCT_CREATOR) || uri.equals(DCE_CREATOR)
				|| uri.equals(PROV_WASATTRIBUTEDTO);
	}

	public static boolean isAuthorProperty(URI uri) {
		return uri.equals(PAV_AUTHOREDBY) || uri.equals(PAV_AUTHOREDBY_1);
	}

}
