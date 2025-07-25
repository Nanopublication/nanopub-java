package org.nanopub.fdo;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.PROV;
import org.eclipse.rdf4j.model.vocabulary.RDF;
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

/**
 * Utility class for creating Nanopubs from FDO records.
 */
public class FdoNanopubCreator {

    /**
     * Prefix for FDO type IRIs.
     */
    public static final String FDO_TYPE_PREFIX = "https://w3id.org/kpxl/handle/terms/";

    private static final ValueFactory vf = SimpleValueFactory.getInstance();

    private static final Random random = new Random();

    /**
     * Creates a NanopubCreator with the given FdoRecord and the FDO IRI.
     *
     * @param fdoRecord the FdoRecord to be included in the Nanopub
     * @param fdoIri    the IRI of the FDO record
     * @return a NanopubCreator instance ready to create a Nanopub
     */
    public static NanopubCreator createWithFdoIri(FdoRecord fdoRecord, IRI fdoIri) {
        IRI npIri = vf.createIRI("http://purl.org/nanopub/temp/" + Math.abs(random.nextInt()) + "/");
        return prepareNanopubCreator(fdoRecord, fdoIri, npIri);
    }

    /**
     * Creates a NanopubCreator with the given FdoRecord and a FDO suffix.
     *
     * @param fdoRecord the FdoRecord to be included in the Nanopub
     * @param fdoSuffix the suffix to be appended to the FDO IRI
     * @return a NanopubCreator instance ready to create a Nanopub
     */
    public static NanopubCreator createWithFdoSuffix(FdoRecord fdoRecord, String fdoSuffix) {
        String npIriString = "http://purl.org/nanopub/temp/" + Math.abs(random.nextInt()) + "/";
        String fdoIriString = npIriString + fdoSuffix;
        IRI fdoIri = vf.createIRI(fdoIriString);
        IRI npIri = vf.createIRI(npIriString);

        return prepareNanopubCreator(fdoRecord, fdoIri, npIri);
    }

    private static NanopubCreator prepareNanopubCreator(FdoRecord fdoRecord, IRI fdoIri, IRI npIri) {
        fdoRecord.setId(fdoIri);
        NanopubCreator creator = new NanopubCreator(npIri);
        creator.addDefaultNamespaces();
        creator.addNamespace("fdof", "https://w3id.org/fdof/ontology#");
        creator.addAssertionStatement(fdoIri, RDF.TYPE, FdoUtils.RDF_TYPE_FDO);
        creator.addPubinfoStatement(npIri, vf.createIRI("http://purl.org/nanopub/x/introduces"), fdoIri);
        creator.addAssertionStatements(fdoRecord.buildStatements().toArray(new Statement[0]));

        return creator;
    }

    /**
     * Creation of Nanopub from handle system.
     *
     * @param id the handle system identifier
     * @return Nanopub containing the data from the handle system
     * @throws MalformedNanopubException if the Nanopub cannot be created due to malformed data
     * @throws URISyntaxException        if the handle system identifier is not a valid URI
     * @throws IOException               if there is an error during the HTTP request to the handle system
     * @throws InterruptedException      if the thread is interrupted while waiting for the HTTP request to complete
     */
    public static Nanopub createFromHandleSystem(String id) throws MalformedNanopubException, URISyntaxException, IOException, InterruptedException {
        FdoRecord record = createFdoRecordFromHandleSystem(id);

        IRI fdoIri = FdoUtils.createIri(id);
        NanopubCreator creator = createWithFdoIri(record, fdoIri);
        creator.addProvenanceStatement(PROV.WAS_DERIVED_FROM, vf.createIRI(HandleResolver.BASE_URI + id));
        Nanopub np = creator.finalizeNanopub(true);
        return np;
    }

    /**
     * Creates an FdoRecord from a handle system identifier.
     *
     * @param id the handle system identifier
     * @return FdoRecord containing the data from the handle system
     * @throws URISyntaxException   if the handle system identifier is not a valid URI
     * @throws IOException          if there is an error during the HTTP request to the handle system
     * @throws InterruptedException if the thread is interrupted while waiting for the HTTP request to complete
     */
    public static FdoRecord createFdoRecordFromHandleSystem(String id) throws URISyntaxException, IOException, InterruptedException {
        ParsedJsonResponse response = new HandleResolver().call(id);
        FdoRecord record = initFdoRecord(response);

        for (Value v : response.values) {
            if (v.type.equals(DATA_REF_HANDLE)) {
                record.setAttribute(DATA_REF_IRI, vf.createIRI(String.valueOf(v.data.value)));
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
                IRI fdoHandleIri;
                if (v.type.contains("/")) {
                    fdoHandleIri = vf.createIRI(FDO_URI_PREFIX + v.type);
                } else {
                    fdoHandleIri = vf.createIRI(FDO_TYPE_PREFIX + v.type);
                }
                if (looksLikeUrl(dataValueToImport)) {
                    record.setAttribute(fdoHandleIri, vf.createIRI(dataValueToImport));
                } else {
                    record.setAttribute(fdoHandleIri, vf.createLiteral(dataValueToImport));
                }
            }
        }
        return record;
    }

    private static FdoRecord initFdoRecord(ParsedJsonResponse response) {
        String label = null;
        IRI profile = null;

        for (Value v : response.values) {
            if (v.type.equals("name")) {
                label = String.valueOf(v.data.value);
            }
            if (v.type.equals(PROFILE_HANDLE) || v.type.equals(PROFILE_HANDLE_1) || v.type.equals(PROFILE_HANDLE_2)) {
                // TODO later remove PROFILE_HANDLE_1 and PROFILE_HANDLE_2
                String profileValue = String.valueOf(v.data.value);
                if (looksLikeHandle(profileValue)) {
                    profile = toIri(profileValue);
                } else {
                    profile = vf.createIRI(profileValue);
                }
            }
        }

        FdoRecord record = new FdoRecord(profile, label, null);
        return record;
    }

}
