package org.nanopub.fdo;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.PROV;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.nanopub.MalformedNanopubException;
import org.nanopub.Nanopub;
import org.nanopub.NanopubCreator;
import org.nanopub.fdo.rest.HandleResolver;
import org.nanopub.fdo.rest.gson.ParsedJsonResponse;
import org.nanopub.fdo.rest.gson.Value;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Random;

import static org.nanopub.fdo.FdoUtils.*;

public class FdoNanopubCreator {

    public static final String FDO_TYPE_PREFIX = "https://w3id.org/kpxl/handle/terms/";

    private static final ValueFactory vf = SimpleValueFactory.getInstance();

    private static final Random random = new Random();

    public static NanopubCreator createWithFdoIri(IRI fdoIri, String profile, String label) {
        IRI npIri = vf.createIRI("http://purl.org/nanopub/temp/" + Math.abs(random.nextInt()) + "/");
        return prepareNanopubCreator(profile, npIri, fdoIri, label, null);
    }

    /**
     * Builds a NanopubCreator with the given values.
     * @param fdoSuffix
     * @param profile
     * @param label may be null
     * @param dataRef may be null
     * @return
     */
    public static NanopubCreator createWithFdoSuffix(String fdoSuffix, String profile, String label, String dataRef) {
        String npIriString = "http://purl.org/nanopub/temp/" + Math.abs(random.nextInt()) + "/";
        String fdoIriString = npIriString + fdoSuffix;
        IRI fdoIri = vf.createIRI(fdoIriString);
        IRI npIri = vf.createIRI(npIriString);

        return prepareNanopubCreator(profile, npIri, fdoIri, label, dataRef);
    }

    private static NanopubCreator prepareNanopubCreator(String profile, IRI npIri, IRI fdoIri, String label, String dataRef) {
        NanopubCreator creator = new NanopubCreator(npIri);
        creator.addDefaultNamespaces();
        creator.addNamespace("fdof", "https://w3id.org/fdof/ontology#");
        creator.addAssertionStatement(fdoIri, RDF.TYPE, FdoUtils.RDF_TYPE_FDO);
        creator.addAssertionStatement(fdoIri, FdoUtils.PROFILE_IRI, vf.createLiteral(profile));
        if (label != null) {
            creator.addAssertionStatement(fdoIri, RDFS.LABEL, vf.createLiteral(label));
        }
        if (dataRef != null) {
            creator.addAssertionStatement(fdoIri, DATA_REF_IRI, vf.createLiteral(dataRef));
        }
        creator.addPubinfoStatement(npIri, vf.createIRI("http://purl.org/nanopub/x/introduces"), fdoIri);
        return creator;
    }

    public static NanopubCreator createWithFdoRecord(FdoRecord fdoRecord) {
        IRI fdoIri = vf.createIRI(fdoRecord.getId());
        IRI npIri = vf.createIRI("http://purl.org/nanopub/temp/" + Math.abs(random.nextInt()) + "/");

        NanopubCreator creator = new NanopubCreator(npIri);
        creator.addDefaultNamespaces();
        creator.addNamespace("fdof", "https://w3id.org/fdof/ontology#");
        creator.addAssertionStatement(fdoIri, RDF.TYPE, FdoUtils.RDF_TYPE_FDO);
        creator.addPubinfoStatement(npIri, vf.createIRI("http://purl.org/nanopub/x/introduces"), fdoIri);
        creator.addAssertionStatements(fdoRecord.getStatements().toArray(new Statement[0]));

        return creator;
    }

    /**
     * Experimental creation of Nanopub from handle system.
     */
    public static Nanopub createFromHandleSystem(String id) throws MalformedNanopubException, URISyntaxException, IOException, InterruptedException {
        IRI fdoIri = FdoUtils.createIri(id);

        ParsedJsonResponse response = new HandleResolver().call(id);
        String label = null;
        String profile = null;

        for (Value v: response.values) {
            if (v.type.equals("name")) {
                label = String.valueOf(v.data.value);
            }
            if (v.type.equals(PROFILE_HANDLE) || v.type.equals(PROFILE_HANDLE_1) || v.type.equals(PROFILE_HANDLE_2)) {
                // TODO later remove PROFILE_HANDLE_1 and PROFILE_HANDLE_2
                String profileValue = String.valueOf(v.data.value);
                if (looksLikeHandle(profileValue)) {
                    profile = toIri(profileValue).stringValue();
                } else {
                    profile = profileValue;
                }
            }
        }
        NanopubCreator creator = createWithFdoIri(fdoIri, profile, label);

        for (Value v: response.values) {
            if (v.type.equals(DATA_REF_HANDLE)) {
                creator.addAssertionStatement(fdoIri, DATA_REF_IRI, vf.createLiteral(String.valueOf(v.data.value)));
                continue;
            }
            if (!v.type.equals("HS_ADMIN") && !v.type.equals("name") && !v.type.equals("id") &&
                    !v.type.equals(PROFILE_HANDLE) && !v.type.equals(PROFILE_HANDLE_1) && !v.type.equals(PROFILE_HANDLE_2)) {
                // TODO later remove PROFILE_HANDLE_1 and PROFILE_HANDLE_2
                String dataValue = String.valueOf(v.data.value);
                String dataValueToImport;
                if (looksLikeHandle(dataValue)) {
                    dataValueToImport = toIri(dataValue).stringValue();
                } else {
                    dataValueToImport = dataValue;
                }
                creator.addAssertionStatement(fdoIri, vf.createIRI(FDO_TYPE_PREFIX + v.type), vf.createLiteral(dataValueToImport));
            }
        }

        creator.addProvenanceStatement(PROV.WAS_DERIVED_FROM, vf.createIRI(HandleResolver.BASE_URI+id));
        Nanopub np = creator.finalizeNanopub(true);
        return np;
    }

}
