package org.nanopub;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;

import java.util.Random;

public class FdoNanopubCreator {

    private static final String FDO_URI_PREFIX = "https://hdl.handle.net/";

    private static final ValueFactory vf = SimpleValueFactory.getInstance();

    private static final IRI RDF_TYPE_FDO = vf.createIRI("https://w3id.org/fdof/ontology#FAIRDigitalObject");

    private static final IRI RDF_FDO_PROFILE = vf.createIRI("https://hdl.handle.net/0.FDO/Profile");

    private Random random = new Random();

    private NanopubCreator creator;

    public FdoNanopubCreator(IRI handleIri, IRI profileIri, String label) {
        String npUri = "http://purl.org/nanopub/temp/" + Math.abs(random.nextInt()) + "/";
        creator = new NanopubCreator(npUri);
        creator.addDefaultNamespaces();
        creator.addNamespace("fdof", "https://w3id.org/fdof/ontology#");
        creator.addAssertionStatement(handleIri, RDF.TYPE, RDF_TYPE_FDO);
        creator.addAssertionStatement(handleIri, RDF_FDO_PROFILE, profileIri);
        creator.addAssertionStatement(handleIri, RDFS.LABEL, vf.createLiteral(label));
    }

    public FdoNanopubCreator(String fdoHandle, String profileHandle, String label) throws MalformedNanopubException {
        this(toIri(fdoHandle), toIri(profileHandle), label);
    }

    private static IRI toIri(String fdoHandle) {
        return vf.createIRI(FDO_URI_PREFIX + fdoHandle);
    }

    public NanopubCreator getNanopubCreator() {
        return creator;
    }

}
