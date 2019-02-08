package org.nanopub.extra.index;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.DC;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.nanopub.NanopubCreator;

public abstract class SimpleIndexCreator extends NanopubIndexCreator {

	private String baseUri;
	private String title;
	private String description;
	private IRI license;
	private List<String> creators = new ArrayList<>();
	private List<IRI> seeAlsoUris = new ArrayList<>();

	public SimpleIndexCreator() {
		this(null, null);
	}

	public SimpleIndexCreator(IRI previousIndexUri) {
		this(null, previousIndexUri);
	}

	public SimpleIndexCreator(String baseUri) {
		this(baseUri, null);
	}

	public SimpleIndexCreator(String baseUri, IRI previousIndexUri) {
		super(previousIndexUri);
		this.baseUri = baseUri;
	}

	public void setBaseUri(String baseUri) {
		this.baseUri = baseUri;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setLicense(IRI license) {
		this.license = license;
	}

	public void addCreator(String creatorUriOrOrcid) {
		creators.add(creatorUriOrOrcid);
	}

	public void addSeeAlsoUri(IRI seeAlsoUri) {
		seeAlsoUris.add(seeAlsoUri);
	}

	@Override
	public String getBaseUri() {
		return baseUri;
	}

	@Override
	public void enrichIncompleteIndex(NanopubCreator npCreator) {
		if (title != null) {
			npCreator.addPubinfoStatement(DC.TITLE, SimpleValueFactory.getInstance().createLiteral(title));
		}
		for (String creator : creators) {
			if (creator.indexOf("://") > 0) {
				npCreator.addCreator(SimpleValueFactory.getInstance().createIRI(creator));
			} else if (creator.matches("[0-9]{4}-[0-9]{4}-[0-9]{4}-[0-9]{3}[0-9X]")) {
				npCreator.addCreator(creator);
			} else {
				throw new IllegalArgumentException("Author has to be URI or ORCID: " + creator);
			}
		}
		if (license != null) {
			npCreator.addPubinfoStatement(DCTERMS.LICENSE, license);
		}
		for (IRI seeAlsoUri : seeAlsoUris) {
			npCreator.addPubinfoStatement(RDFS.SEEALSO, seeAlsoUri);
		}
	}

	@Override
	public void enrichCompleteIndex(NanopubCreator npCreator) {
		enrichIncompleteIndex(npCreator);
		if (description != null) {
			npCreator.addPubinfoStatement(DC.DESCRIPTION, SimpleValueFactory.getInstance().createLiteral(description));
		}
	}

}
