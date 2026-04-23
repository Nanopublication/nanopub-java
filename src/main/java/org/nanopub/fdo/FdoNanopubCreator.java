package org.nanopub.fdo;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.PROV;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.nanopub.MalformedNanopubException;
import org.nanopub.Nanopub;
import org.nanopub.NanopubAlreadyFinalizedException;
import org.nanopub.NanopubCreator;
import org.nanopub.fdo.rest.HandleResolver;
import org.nanopub.fdo.rest.gson.ParsedJsonResponse;
import org.nanopub.fdo.rest.gson.Value;
import org.nanopub.trusty.TempUriReplacer;
import org.nanopub.vocabulary.FDOF;
import org.nanopub.vocabulary.HDL;
import org.nanopub.vocabulary.NPX;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
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
     * @throws NanopubAlreadyFinalizedException if the Nanopub has already been finalized
     */
    public static NanopubCreator createWithFdoIri(FdoRecord fdoRecord, IRI fdoIri) throws NanopubAlreadyFinalizedException {
        IRI npIri = vf.createIRI(TempUriReplacer.tempUri + Math.abs(random.nextInt()) + "/");
        return prepareNanopubCreator(fdoRecord, fdoIri, npIri);
    }

    /**
     * Creates a NanopubCreator with the given FdoRecord and a FDO suffix.
     *
     * @param fdoRecord the FdoRecord to be included in the Nanopub
     * @param fdoSuffix the suffix to be appended to the FDO IRI
     * @return a NanopubCreator instance ready to create a Nanopub
     * @throws NanopubAlreadyFinalizedException if the Nanopub has already been finalized
     */
    public static NanopubCreator createWithFdoSuffix(FdoRecord fdoRecord, String fdoSuffix) throws NanopubAlreadyFinalizedException {
        String npIriString = TempUriReplacer.tempUri + Math.abs(random.nextInt()) + "/";
        String fdoIriString = npIriString + fdoSuffix;
        IRI fdoIri = vf.createIRI(fdoIriString);
        IRI npIri = vf.createIRI(npIriString);

        return prepareNanopubCreator(fdoRecord, fdoIri, npIri);
    }

    static NanopubCreator prepareNanopubCreator(FdoRecord fdoRecord, IRI fdoIri, IRI npIri) throws NanopubAlreadyFinalizedException {
        fdoRecord.setId(fdoIri);
        NanopubCreator creator = new NanopubCreator(npIri);
        creator.addDefaultNamespaces();
        creator.addNamespace(FDOF.PREFIX, FDOF.NAMESPACE);
        creator.addAssertionStatement(fdoIri, RDF.TYPE, FDOF.FAIR_DIGITAL_OBJECT);
        creator.addPubinfoStatement(npIri, NPX.INTRODUCES, fdoIri);
        creator.addAssertionStatements(fdoRecord.buildStatements().toArray(new Statement[0]));

        return creator;
    }

    /**
     * Creation of Nanopub from the handle system.
     *
     * @param id the handle system identifier
     * @return Nanopub containing the data from the handle system
     * @throws org.nanopub.MalformedNanopubException        if the Nanopub cannot be created due to malformed data
     * @throws java.net.URISyntaxException                  if the handle system identifier is not a valid URI
     * @throws java.io.IOException                          if there is an error during the HTTP request to the handle system
     * @throws java.lang.InterruptedException               if the thread is interrupted while waiting for the HTTP request to complete
     * @throws org.nanopub.NanopubAlreadyFinalizedException if the Nanopub has already been finalized
     */
    public static Nanopub createFromHandleSystem(String id) throws MalformedNanopubException, URISyntaxException, IOException, InterruptedException, NanopubAlreadyFinalizedException {
        return createFromHandleSystem(id, false);
    }

    /**
     * Creation of Nanopub from the handle system, optionally enriching predicates using the
     * profile's Data Type Registry entry.
     *
     * <p>When {@code enrichFromSchema} is {@code true}, the FDO profile handle is resolved and its
     * property list is used to turn handle-record field names into handle IRIs (predicates in the
     * assertion). For each mapped predicate, an {@code rdfs:label} with the original field name is
     * added to the pubinfo graph. If the profile schema cannot be resolved, behaviour falls back
     * to the default mapping.</p>
     *
     * @param id               the handle system identifier
     * @param enrichFromSchema whether to enrich predicates via the profile's DTR entry
     * @return Nanopub containing the data from the handle system
     * @throws org.nanopub.MalformedNanopubException        if the Nanopub cannot be created due to malformed data
     * @throws java.net.URISyntaxException                  if the handle system identifier is not a valid URI
     * @throws java.io.IOException                          if there is an error during the HTTP request to the handle system
     * @throws java.lang.InterruptedException               if the thread is interrupted while waiting for the HTTP request to complete
     * @throws org.nanopub.NanopubAlreadyFinalizedException if the Nanopub has already been finalized
     */
    public static Nanopub createFromHandleSystem(String id, boolean enrichFromSchema) throws MalformedNanopubException, URISyntaxException, IOException, InterruptedException, NanopubAlreadyFinalizedException {
        ParsedJsonResponse response = new HandleResolver().call(id);
        Map<IRI, String> labelSink = new LinkedHashMap<>();
        FdoRecord record = buildFdoRecord(id, response, enrichFromSchema, labelSink);

        IRI fdoIri = FdoUtils.createIri(id);
        NanopubCreator creator = createWithFdoIri(record, fdoIri);
        creator.addProvenanceStatement(PROV.WAS_DERIVED_FROM, vf.createIRI(HandleResolver.BASE_URI + id));
        for (Map.Entry<IRI, String> e : labelSink.entrySet()) {
            creator.addPubinfoStatement(e.getKey(), RDFS.LABEL, vf.createLiteral(e.getValue()));
        }
        creator.addPubinfoStatement(fdoIri, RDFS.LABEL, vf.createLiteral(resolveFdoLabel(response, id)));
        return creator.finalizeNanopub(true);
    }

    private static String resolveFdoLabel(ParsedJsonResponse response, String id) {
        if (response != null && response.values != null) {
            for (Value v : response.values) {
                if ("referentName".equals(v.type) && v.data != null && v.data.value != null) {
                    String s = String.valueOf(v.data.value);
                    if (!s.isEmpty()) return s;
                }
            }
        }
        return id;
    }

    /**
     * Creates an FdoRecord from a handle system identifier.
     *
     * @param id the handle system identifier
     * @return FdoRecord containing the data from the handle system
     * @throws java.net.URISyntaxException    if the handle system identifier is not a valid URI
     * @throws java.io.IOException            if there is an error during the HTTP request to the handle system
     * @throws java.lang.InterruptedException if the thread is interrupted while waiting for the HTTP request to complete
     */
    public static FdoRecord createFdoRecordFromHandleSystem(String id) throws URISyntaxException, IOException, InterruptedException, MalformedNanopubException {
        return createFdoRecordFromHandleSystem(id, false, null);
    }

    /**
     * Creates an FdoRecord from a handle system identifier, optionally enriching predicates
     * via the profile's Data Type Registry entry.
     *
     * @param id               the handle system identifier
     * @param enrichFromSchema whether to fetch the profile schema and map property names to handle IRIs
     * @param labelSink        if non-null, populated with {@code predicateIri → originalName} pairs
     *                         for every predicate sourced from the profile schema
     * @return FdoRecord containing the data from the handle system
     * @throws java.net.URISyntaxException            if the handle system identifier is not a valid URI
     * @throws java.io.IOException                    if there is an error during the HTTP request to the handle system
     * @throws java.lang.InterruptedException         if the thread is interrupted while waiting for the HTTP request to complete
     * @throws org.nanopub.MalformedNanopubException  if the handle record has no recognised profile type
     */
    public static FdoRecord createFdoRecordFromHandleSystem(String id, boolean enrichFromSchema, Map<IRI, String> labelSink) throws URISyntaxException, IOException, InterruptedException, MalformedNanopubException {
        ParsedJsonResponse response = new HandleResolver().call(id);
        return buildFdoRecord(id, response, enrichFromSchema, labelSink);
    }

    private static FdoRecord buildFdoRecord(String id, ParsedJsonResponse response, boolean enrichFromSchema, Map<IRI, String> labelSink) throws MalformedNanopubException {
        FdoRecord record = initFdoRecord(response);
        if (record.getAttribute(DCTERMS.CONFORMS_TO) == null) {
            throw new MalformedNanopubException("Handle record for '" + id + "' has no recognised FDO profile type (expected one of: "
                    + PROFILE_HANDLE + ", " + PROFILE_HANDLE_1 + ", " + PROFILE_HANDLE_2 + ", " + PROFILE_HANDLE_3 + ")");
        }

        Map<String, IRI> nameToHandle = enrichFromSchema
                ? FdoProfileSchemaLoader.loadPropertyMap((IRI) record.getAttribute(DCTERMS.CONFORMS_TO))
                : new HashMap<>();

        for (Value v : response.values) {
            if (v.type.equals(DATA_REF_HANDLE)) {
                record.setAttribute(FDOF.IS_MATERIALIZED_BY, vf.createIRI(String.valueOf(v.data.value)));
                continue;
            }
            if (!v.type.equals("HS_ADMIN") && !v.type.equals("name") && !v.type.equals("id") && !v.type.equals(PROFILE_HANDLE) && !v.type.equals(PROFILE_HANDLE_1) && !v.type.equals(PROFILE_HANDLE_2) && !v.type.equals(PROFILE_HANDLE_3)) {
                // TODO later remove PROFILE_HANDLE_1, PROFILE_HANDLE_2 and PROFILE_HANDLE_3
                String dataValue = String.valueOf(v.data.value);
                String dataValueToImport;
                if (looksLikeHandle(dataValue)) {
                    dataValueToImport = toIri(dataValue).stringValue();
                } else {
                    dataValueToImport = dataValue;
                }
                IRI fdoHandleIri;
                if (nameToHandle.containsKey(v.type)) {
                    fdoHandleIri = nameToHandle.get(v.type);
                    if (labelSink != null) {
                        labelSink.putIfAbsent(fdoHandleIri, v.type);
                    }
                } else if (v.type.contains("/")) {
                    fdoHandleIri = vf.createIRI(HDL.NAMESPACE + v.type);
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
            if (v.type.equals(PROFILE_HANDLE) || v.type.equals(PROFILE_HANDLE_1) || v.type.equals(PROFILE_HANDLE_2) || v.type.equals(PROFILE_HANDLE_3)) {
                // TODO later remove PROFILE_HANDLE_1, PROFILE_HANDLE_2 and PROFILE_HANDLE_3
                String profileValue = String.valueOf(v.data.value);
                if (looksLikeHandle(profileValue)) {
                    profile = toIri(profileValue);
                } else {
                    profile = vf.createIRI(profileValue);
                }
            }
        }

        return new FdoRecord(profile, label, null);
    }

}
