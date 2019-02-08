package org.nanopub;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

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
		Set<IRI> authors = getAuthors(nanopub);
		Set<IRI> creators = getCreators(nanopub);
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

	public static Set<IRI> getAuthors(Nanopub nanopub) {
		Set<IRI> authors = new HashSet<>();
		for (Statement st : nanopub.getPubinfo()) {
			if (!st.getSubject().equals(nanopub.getUri())) continue;
			if (!isAuthorProperty(st.getPredicate())) continue;
			if (!(st.getObject() instanceof IRI)) continue;
			authors.add((IRI) st.getObject());
		}
		return authors;
	}

	public static Set<IRI> getCreators(Nanopub nanopub) {
		Set<IRI> creators = new HashSet<>();
		for (Statement st : nanopub.getPubinfo()) {
			if (!st.getSubject().equals(nanopub.getUri())) continue;
			if (!isCreatorProperty(st.getPredicate())) continue;
			if (!(st.getObject() instanceof IRI)) continue;
			creators.add((IRI) st.getObject());
		}
		return creators;
	}

	public static final IRI PAV_CREATEDBY = SimpleValueFactory.getInstance().createIRI("http://purl.org/pav/createdBy");
	public static final IRI PAV_CREATEDBY_1 = SimpleValueFactory.getInstance().createIRI("http://swan.mindinformatics.org/ontologies/1.2/pav/createdBy");
	public static final IRI PAV_CREATEDBY_2 = SimpleValueFactory.getInstance().createIRI("http://purl.org/pav/2.0/createdBy");
	public static final IRI DCT_CREATOR = SimpleValueFactory.getInstance().createIRI("http://purl.org/dc/terms/creator");
	public static final IRI DCE_CREATOR = SimpleValueFactory.getInstance().createIRI("http://purl.org/dc/elements/1.1/creator");
	public static final IRI PROV_WASATTRIBUTEDTO = SimpleValueFactory.getInstance().createIRI("http://www.w3.org/ns/prov#wasAttributedTo");

	public static final IRI PAV_AUTHOREDBY = SimpleValueFactory.getInstance().createIRI("http://purl.org/pav/authoredBy");
	public static final IRI PAV_AUTHOREDBY_1 = SimpleValueFactory.getInstance().createIRI("http://swan.mindinformatics.org/ontologies/1.2/pav/authoredBy");
	public static final IRI PAV_AUTHOREDBY_2 = SimpleValueFactory.getInstance().createIRI("http://purl.org/pav/2.0/authoredBy");

	public static boolean isCreatorProperty(IRI uri) {
		return uri.equals(PAV_CREATEDBY) || uri.equals(PAV_CREATEDBY_1) || uri.equals(PAV_CREATEDBY_2)
				|| uri.equals(DCT_CREATOR) || uri.equals(DCE_CREATOR) || uri.equals(PROV_WASATTRIBUTEDTO);
	}

	public static boolean isAuthorProperty(IRI uri) {
		return uri.equals(PAV_AUTHOREDBY) || uri.equals(PAV_AUTHOREDBY_1) || uri.equals(PAV_AUTHOREDBY_2);
	}

}
