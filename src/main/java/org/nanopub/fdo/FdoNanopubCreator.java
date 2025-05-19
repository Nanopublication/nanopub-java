package org.nanopub.fdo;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.PROV;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.nanopub.MalformedNanopubException;
import org.nanopub.Nanopub;
import org.nanopub.NanopubCreator;
import org.nanopub.fdo.rest.HandleResolver;
import org.nanopub.fdo.rest.gson.Response;
import org.nanopub.fdo.rest.gson.Value;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Random;

public class FdoNanopubCreator {

    public static final String FDO_TYPE_PREFIX = "https://w3id.org/kpxl/handle/terms/";

    private static final ValueFactory vf = SimpleValueFactory.getInstance();

    private static final Random random = new Random();

    public static NanopubCreator createWithFdoIri(IRI fdoIri, IRI profileIri, String label) {
        IRI npIri = vf.createIRI("http://purl.org/nanopub/temp/" + Math.abs(random.nextInt()) + "/");
        return prepareNanopubCreator(profileIri, label, fdoIri, npIri);
    }

    public static NanopubCreator createWithFdoSuffix(String fdoSuffix, IRI profileIri, String label) {
        String npIriString = "http://purl.org/nanopub/temp/" + Math.abs(random.nextInt()) + "/";
        String fdoIriString = npIriString + fdoSuffix;
        IRI fdoIri = vf.createIRI(fdoIriString);
        IRI npIri = vf.createIRI(npIriString);

        return prepareNanopubCreator(profileIri, label, fdoIri, npIri);
    }

    private static NanopubCreator prepareNanopubCreator(IRI profileIri, String label, IRI fdoIri, IRI npIri) {
        NanopubCreator creator = new NanopubCreator(npIri);
        creator.addDefaultNamespaces();
        creator.addNamespace("fdof", "https://w3id.org/fdof/ontology#");
        creator.addAssertionStatement(fdoIri, RDF.TYPE, FdoUtils.RDF_TYPE_FDO);
        creator.addAssertionStatement(fdoIri, FdoUtils.RDF_FDO_PROFILE, profileIri);
        creator.addAssertionStatement(fdoIri, RDFS.LABEL, vf.createLiteral(label));
        creator.addPubinfoStatement(npIri, vf.createIRI("http://purl.org/nanopub/x/introduces"), fdoIri);
        return creator;
    }

    // TODO Additional method to connect to FdoMetadta class.
    public static NanopubCreator createWithMetadata(FdoMetadata fdoMetadata) {
    	// TODO Create with given metadata. This can also be used to update an FDO nanopub (Op.Update).
    	return null;
    }

    /**
     * Experimental creation of Nanopub from handle system.
     */
    public static Nanopub createFromHandleSystem(String id) throws MalformedNanopubException, URISyntaxException, IOException, InterruptedException {
        IRI fdoIri = FdoUtils.createIri(id);

        Response response = new HandleResolver().call(id);
        String label = null;
        String profile = null;

        for (Value v: response.values) {
            if (v.type.equals("HS_ADMIN")) {
                continue;
            }
            if (v.type.equals("name")) {
                label = String.valueOf(v.data.value);
            }
            if (v.type.equals("FdoProfile")) {
                profile = String.valueOf(v.data.value);
            }
        }
        NanopubCreator creator = createWithFdoIri(fdoIri, FdoUtils.createIri(profile), label);

        for (Value v: response.values) {
            if (!v.type.equals("HS_ADMIN") && !v.type.equals("name") && !v.type.equals("FdoProfile") && !v.type.equals("id")) {
                creator.addAssertionStatement(fdoIri, vf.createIRI(FDO_TYPE_PREFIX + v.type), vf.createLiteral(String.valueOf(v.data.value)));
            }
        }

        creator.addProvenanceStatement(PROV.WAS_DERIVED_FROM, vf.createIRI(HandleResolver.BASE_URI+id));
        Nanopub np = creator.finalizeNanopub(true);
        return np;
    }

}
