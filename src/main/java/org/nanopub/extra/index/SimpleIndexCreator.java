package org.nanopub.extra.index;

import java.util.ArrayList;
import java.util.List;

import org.nanopub.NanopubCreator;
import org.openrdf.model.URI;
import org.openrdf.model.impl.LiteralImpl;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.vocabulary.DC;
import org.openrdf.model.vocabulary.RDFS;

public abstract class SimpleIndexCreator extends NanopubIndexCreator {

	private String baseUri;
	private String title;
	private String description;
	private List<String> creators = new ArrayList<>();
	private List<URI> seeAlsoUris = new ArrayList<>();

	public SimpleIndexCreator() {
		this(null, null);
	}

	public SimpleIndexCreator(URI previousIndexUri) {
		this(null, previousIndexUri);
	}

	public SimpleIndexCreator(String baseUri) {
		this(baseUri, null);
	}

	public SimpleIndexCreator(String baseUri, URI previousIndexUri) {
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

	public void addCreator(String creatorUriOrOrcid) {
		creators.add(creatorUriOrOrcid);
	}

	public void addSeeAlsoUri(URI seeAlsoUri) {
		seeAlsoUris.add(seeAlsoUri);
	}

	@Override
	public String getBaseUri() {
		return baseUri;
	}

	@Override
	public void enrichIncompleteIndex(NanopubCreator npCreator) {
		if (title != null) {
			npCreator.addPubinfoStatement(DC.TITLE, new LiteralImpl(title));
		}
		for (String creator : creators) {
			if (creator.indexOf("://") > 0) {
				npCreator.addCreator(new URIImpl(creator));
			} else if (creator.matches("[0-9]{4}-[0-9]{4}-[0-9]{4}-[0-9]{3}[0-9X]")) {
				npCreator.addCreator(creator);
			} else {
				throw new IllegalArgumentException("Author has to be URI or ORCID: " + creator);
			}
		}
		for (URI seeAlsoUri : seeAlsoUris) {
			npCreator.addPubinfoStatement(RDFS.SEEALSO, seeAlsoUri);
		}
	}

	@Override
	public void enrichCompleteIndex(NanopubCreator npCreator) {
		enrichIncompleteIndex(npCreator);
		if (description != null) {
			npCreator.addPubinfoStatement(DC.DESCRIPTION, new LiteralImpl(description));
		}
	}

}
