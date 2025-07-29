package org.nanopub.extra.index;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.DC;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.nanopub.NanopubCreator;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple index creator for nanopublications.
 */
public abstract class SimpleIndexCreator extends NanopubIndexCreator {

    private String baseUri;
    private String title;
    private String description;
    private IRI license;
    private List<String> creators = new ArrayList<>();
    private List<IRI> seeAlsoUris = new ArrayList<>();

    /**
     * Creates a new simple index creator with default settings.
     */
    public SimpleIndexCreator() {
        this(null, null, true);
    }

    /**
     * Creates a new simple index creator with the given previous index URI and trustiness setting.
     *
     * @param previousIndexUri the URI of the previous index
     * @param makeTrusty       if true the created nanopub will be trusty
     */
    public SimpleIndexCreator(IRI previousIndexUri, boolean makeTrusty) {
        this(null, previousIndexUri, makeTrusty);
    }

    /**
     * Creates a new simple index creator with trustiness setting.
     *
     * @param makeTrusty if true the created nanopub will be trusty
     */
    public SimpleIndexCreator(boolean makeTrusty) {
        this(null, null, makeTrusty);
    }

    /**
     * Creates a new simple index creator with the given base URI, previous index URI, and trustiness setting.
     *
     * @param baseUri          the base URI for the index nanopub
     * @param previousIndexUri the URI of the previous index
     * @param makeTrusty       if true the created nanopub will be trusty
     */
    public SimpleIndexCreator(String baseUri, IRI previousIndexUri, boolean makeTrusty) {
        super(previousIndexUri, makeTrusty);
        this.baseUri = baseUri;
    }

    /**
     * Sets the base URI for the nanopublication index.
     *
     * @param baseUri the base URI to set
     */
    public void setBaseUri(String baseUri) {
        this.baseUri = baseUri;
    }

    /**
     * Sets the title of the nanopublication index.
     *
     * @param title the title to set
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Sets the description of the nanopublication index.
     *
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Sets the license for the nanopublication index.
     *
     * @param license the license IRI to set
     */
    public void setLicense(IRI license) {
        this.license = license;
    }

    /**
     * Adds a creator to the nanopublication index.
     *
     * @param creatorUriOrOrcid the URI or ORCID of the creator to add
     */
    public void addCreator(String creatorUriOrOrcid) {
        creators.add(creatorUriOrOrcid);
    }

    /**
     * Adds a seeAlso URI to the nanopublication index.
     *
     * @param seeAlsoUri the seeAlso URI to add
     */
    public void addSeeAlsoUri(IRI seeAlsoUri) {
        seeAlsoUris.add(seeAlsoUri);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getBaseUri() {
        return baseUri;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Enriches an incomplete nanopublication index with metadata.
     */
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

    /**
     * {@inheritDoc}
     * <p>
     * Enriches a complete nanopublication index with additional metadata.
     */
    @Override
    public void enrichCompleteIndex(NanopubCreator npCreator) {
        enrichIncompleteIndex(npCreator);
        if (description != null) {
            npCreator.addPubinfoStatement(DC.DESCRIPTION, SimpleValueFactory.getInstance().createLiteral(description));
        }
    }

}
