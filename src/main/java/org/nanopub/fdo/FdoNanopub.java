package org.nanopub.fdo;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.nanopub.Nanopub;

import static org.nanopub.fdo.FdoUtils.RDF_FDO_PROFILE;

/**
 * Wrapper for nanopubs which allows to extract specific FDO fields.
 */
public class FdoNanopub {

    private final Nanopub nanopub;

    /** the profile is mandatory */
    private IRI profile;

    /** the label is optional */
    private String label;

    /**
     * @throws IllegalArgumentException if the nanopub does not contain a FDO profile
     */
    public FdoNanopub(Nanopub nanopub) {
        this.nanopub = nanopub;
        for (Statement st: nanopub.getAssertion()) {
            if (st.getPredicate().equals(RDF_FDO_PROFILE)) {
                if (st.getObject() instanceof IRI) {
                    this.profile = (IRI) st.getObject();
                    break;
                }
            }
        }
        if (profile == null) {
            throw new IllegalArgumentException("Not an valid FDO Nanopub. Profile is undefined.");
        }
        for (Statement st: nanopub.getAssertion()) {
            if (st.getPredicate().equals(RDFS.LABEL)) {
                this.label = st.getObject().stringValue();
                break;
            }
        }
    }

    /**
     * @return the profile IRI
     */
    public IRI getProfile() {
        return profile;
    }

    /**
     * @return the label iff it's available, null otherwise.
     */
    public String getLabel() {
        return label;
    }

}
