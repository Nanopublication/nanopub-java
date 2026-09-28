package org.nanopub.extra.security;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for removing artifact codes from RDF statements. This is used when signing and verifying signatures of nanopublications.
 */
public class ArtifactCodeUtils {

    private static final ValueFactory vf = SimpleValueFactory.getInstance();

    private ArtifactCodeUtils() {
        // Utility class, do not instantiate
    }

    protected static List<Statement> removeArtifactCode(List<Statement> in, String ac) {
        List<Statement> out = new ArrayList<>();
        for (Statement st : in) {
            out.add(removeArtifactCode(st, ac));
        }
        return out;
    }

    protected static Statement removeArtifactCode(Statement st, String ac) {
        return vf.createStatement((Resource) removeArtifactCode(st.getSubject(), ac), (IRI) removeArtifactCode(st.getPredicate(), ac),
                removeArtifactCode(st.getObject(), ac), (Resource) removeArtifactCode(st.getContext(), ac));
    }

    protected static Value removeArtifactCode(Value v, String ac) {
        if (v instanceof IRI) {
            return vf.createIRI(removeArtifactCode(v.stringValue(), ac));
        } else {
            return v;
        }
    }

    protected static String removeArtifactCode(String s, String ac) {
        return s.replaceAll(ac + "[#/]?", "");
    }

}
