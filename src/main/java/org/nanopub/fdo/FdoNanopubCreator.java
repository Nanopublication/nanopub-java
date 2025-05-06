package org.nanopub.fdo;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.nanopub.MalformedNanopubException;
import org.nanopub.NanopubCreator;

import java.util.Random;

public class FdoNanopubCreator {

    private static final ValueFactory vf = SimpleValueFactory.getInstance();

    private Random random = new Random();

    private NanopubCreator creator;

    public FdoNanopubCreator(IRI fdoIri, IRI profileIri, String label) {
        IRI npIri = vf.createIRI("http://purl.org/nanopub/temp/" + Math.abs(random.nextInt()) + "/");
        creator = new NanopubCreator(npIri);
        creator.addDefaultNamespaces();
        creator.addNamespace("fdof", "https://w3id.org/fdof/ontology#");
        creator.addAssertionStatement(fdoIri, RDF.TYPE, FdoUtils.RDF_TYPE_FDO);
        creator.addAssertionStatement(fdoIri, FdoUtils.RDF_FDO_PROFILE, profileIri);
        creator.addAssertionStatement(fdoIri, RDFS.LABEL, vf.createLiteral(label));
        creator.addPubinfoStatement(npIri, vf.createIRI("http://purl.org/nanopub/x/introduces"), fdoIri);
    }

    public FdoNanopubCreator(String fdoHandle, String profileHandle, String label) throws MalformedNanopubException {
        this(FdoUtils.toIri(fdoHandle), FdoUtils.toIri(profileHandle), label);
    }

    public NanopubCreator getNanopubCreator() {
        return creator;
    }

}
