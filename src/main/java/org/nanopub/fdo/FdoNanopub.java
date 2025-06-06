package org.nanopub.fdo;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.nanopub.Nanopub;

import static org.nanopub.fdo.FdoUtils.DATA_REF_IRI;
import static org.nanopub.fdo.FdoUtils.PROFILE_IRI;

/**
 * Wrapper for nanopubs which allows to extract specific FDO fields.
 */
public class FdoNanopub {

    private final Nanopub nanopub;

    /** the profile is mandatory */
    private String profile;

    /** the label is optional */
    private String label;

    /** the data-ref is optional */
    private String dataref;

    /**
     * @throws IllegalArgumentException if the nanopub does not contain a FDO profile
     */
    public FdoNanopub(Nanopub nanopub) {
        this.nanopub = nanopub;
        for (Statement st: nanopub.getAssertion()) {
            if (st.getPredicate().equals(PROFILE_IRI)) {
                this.profile = st.getObject().stringValue();
                break;
            }
        }
        if (profile == null) {
            throw new IllegalArgumentException("Not an valid FDO Nanopub. Profile is undefined.");
        }
        for (Statement st: nanopub.getAssertion()) {
            if (st.getPredicate().equals(RDFS.LABEL)) {
                this.label = st.getObject().stringValue();
            }
            if (st.getPredicate().equals(DATA_REF_IRI)) {
                this.dataref = st.getObject().stringValue();
            }
        }
    }

    /**
     * @return the profile IRI
     */
    public String getProfile() {
        return profile;
    }

    /**
     * @return the label iff it's available, null otherwise.
     */
    public String getLabel() {
        return label;
    }

    /**
     * Return FdoRecord object, extracting the triples from the assertion.
     * This can be used as a first step to update an FDO, as the FdoRecord object
     * is changeable and can later be used to create a new FdoNanopub object.
     */
    public FdoRecord getFdoRecord() {
    	return new FdoRecord(this.nanopub);
    }

}
