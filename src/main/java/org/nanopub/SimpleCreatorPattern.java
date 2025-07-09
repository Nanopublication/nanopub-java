package org.nanopub;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

public class SimpleCreatorPattern implements NanopubPattern {

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
		IRI authorListId = null;
		for (Statement st : nanopub.getPubinfo()) {
			if (st.getSubject().equals(nanopub.getUri()) && isAuthorProperty(st.getPredicate()) && (st.getObject() instanceof IRI)) {
				authors.add((IRI) st.getObject());
			}
			if (st.getSubject().equals(nanopub.getUri()) && st.getPredicate().equals(BIBO_AUTHOR_LIST) && (st.getObject() instanceof IRI)) {
				authorListId = (IRI) st.getObject();
			}
		}
		if (authorListId != null) {
			for (Statement st : nanopub.getPubinfo()) {
				if (!st.getSubject().equals(authorListId)) continue;
				if (!st.getPredicate().stringValue().matches(RDF_ELEMENT_PROPERTY_REGEX)) continue;
				if (!(st.getObject() instanceof IRI)) continue;
				authors.add((IRI) st.getObject());
			}
		}
		return authors;
	}

	public static List<IRI> getAuthorList(Nanopub nanopub) {
		List<IRI> authorList = new ArrayList<>();
		Set<IRI> authorSet = new HashSet<>();
		Map<Integer,IRI> authorMap = new HashMap<>();
		IRI authorListId = null;
		for (Statement st : nanopub.getPubinfo()) {
			if (st.getSubject().equals(nanopub.getUri()) && isAuthorProperty(st.getPredicate()) && (st.getObject() instanceof IRI)) {
				authorSet.add((IRI) st.getObject());
			}
			if (st.getSubject().equals(nanopub.getUri()) && st.getPredicate().equals(BIBO_AUTHOR_LIST) && (st.getObject() instanceof IRI)) {
				authorListId = (IRI) st.getObject();
			}
		}
		if (authorListId != null) {
			for (Statement st : nanopub.getPubinfo()) {
				if (!st.getSubject().equals(authorListId)) continue;
				if (!st.getPredicate().stringValue().matches(RDF_ELEMENT_PROPERTY_REGEX)) continue;
				if (!(st.getObject() instanceof IRI)) continue;
				int i = Integer.parseInt(st.getPredicate().stringValue().replaceFirst(RDF_ELEMENT_PROPERTY_REGEX, "$1"));
				authorMap.put(i, (IRI) st.getObject());
			}
			int i = 1;
            while (authorMap.containsKey(i)) {
                authorList.add(authorMap.get(i));
                i = i + 1;
            }
		}
		for (IRI a : authorSet) {
			// TODO This is not efficient (but lists should be small...)
			if (!authorList.contains(a)) authorList.add(a);
		}
		
		return authorList;
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

	public static final IRI BIBO_AUTHOR_LIST = SimpleValueFactory.getInstance().createIRI("http://purl.org/ontology/bibo/authorList");
	public static final String RDF_ELEMENT_PROPERTY_REGEX = "http://www\\.w3\\.org/1999/02/22-rdf-syntax-ns#_([1-9][0-9]*)";

	public static boolean isCreatorProperty(IRI uri) {
		return uri.equals(PAV_CREATEDBY) || uri.equals(PAV_CREATEDBY_1) || uri.equals(PAV_CREATEDBY_2)
				|| uri.equals(DCT_CREATOR) || uri.equals(DCE_CREATOR) || uri.equals(PROV_WASATTRIBUTEDTO);
	}

	public static boolean isAuthorProperty(IRI uri) {
		return uri.equals(PAV_AUTHOREDBY) || uri.equals(PAV_AUTHOREDBY_1) || uri.equals(PAV_AUTHOREDBY_2);
	}

}
