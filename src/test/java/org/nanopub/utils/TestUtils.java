package org.nanopub.utils;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.nanopub.MalformedNanopubException;
import org.nanopub.Nanopub;
import org.nanopub.NanopubAlreadyFinalizedException;
import org.nanopub.NanopubCreator;

public class TestUtils {

    public final static String NANOPUB_URI = "https://knowledgepixels.com/nanopubIri#title";
    public final static ValueFactory vf = SimpleValueFactory.getInstance();
    public final static IRI anyIri = vf.createIRI("https://knowledgepixels.com/nanopubIri#any");
    public final static String ORCID = "https://orcid.org/0000-0000-0000-0000";

    public static Nanopub createNanopub() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        return createNanopub(NANOPUB_URI);
    }

    public static NanopubCreator getNanopubCreator() throws NanopubAlreadyFinalizedException {
        return new NanopubCreator(NANOPUB_URI);
    }

    public static NanopubCreator getNanopubCreator(String nanopubUri) throws NanopubAlreadyFinalizedException {
        return new NanopubCreator(nanopubUri);
    }

    public static Nanopub createNanopub(String nanopubUri) throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        NanopubCreator creator = getNanopubCreator(nanopubUri);

        // Create valid nanopub
        Statement assertionStatement = vf.createStatement(anyIri, anyIri, anyIri);
        creator.addAssertionStatements(assertionStatement);

        Statement provenanceStatement = vf.createStatement(creator.getAssertionUri(), anyIri, anyIri);
        creator.addProvenanceStatements(provenanceStatement);

        Statement pubinfoStatement = vf.createStatement(creator.getNanopubUri(), anyIri, anyIri);
        creator.addPubinfoStatements(pubinfoStatement);

        return creator.finalizeNanopub();
    }

}
