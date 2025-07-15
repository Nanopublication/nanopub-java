package org.nanopub;

import net.trustyuri.TrustyUriException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.PROV;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.junit.jupiter.api.Test;
import org.nanopub.extra.security.SignNanopub;
import org.nanopub.extra.security.TransformContext;
import org.nanopub.extra.server.GetNanopub;
import org.nanopub.extra.server.PublishNanopub;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.ApiResponseEntry;
import org.nanopub.extra.services.QueryAccess;
import org.nanopub.fdo.FdoNanopubCreator;
import org.nanopub.fdo.FdoRecord;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.SignatureException;
import java.util.List;
import java.util.Map;

public class MySandboxIT {

    final ValueFactory vf = SimpleValueFactory.getInstance();

    @Test
    public void doNothing() {
        // it's just here for the @Test annotation to not disappear from the imports
    }

//     @Test
    public void exampleCreateWithFdoIri() throws MalformedNanopubException, TrustyUriException, SignatureException, InvalidKeyException, IOException {
        IRI fdoProfile = vf.createIRI("https://hdl.handle.net/21.T11966/365ff9576c26ca6053db");
        String fdoLabel = "ExampleFdoToUpdate";
        FdoRecord record = new FdoRecord(fdoProfile, fdoLabel, null);
        NanopubCreator creator = FdoNanopubCreator.createWithFdoSuffix(record, "FdoExample");

        creator.addProvenanceStatement(PROV.WAS_ATTRIBUTED_TO, vf.createIRI("https://orcid.org/0009-0008-3635-347X"));
        Nanopub np = creator.finalizeNanopub(true);

//        RDFWriter w = Rio.createWriter(RDFFormat.TRIG, new OutputStreamWriter(out, Charset.forName("UTF-8")));
//        NanopubUtils.propagateToHandler(np, w);
        Nanopub signedNp = SignNanopub.signAndTransform(np, TransformContext.makeDefault());
        String result = PublishNanopub.publish(signedNp);
        System.out.println(result);
    }

    /**
     * Example for Op.Create aggregation FDO
     */
//    @Test
    public void createComplexFdo() throws Exception {

        IRI fdoProfile = vf.createIRI("https://w3id.org/np/RAudZj6DD7iByABdBXAWSsNh3kd6kj5P2bmHi6hzg1xcE/aggregate-fdo-profile");

        String aggregate1 = "https://w3id.org/np/RAbb0pvoFGiNwcY8nL-qSR93O4AAcfsQRS_TNvLqt0VHg/FdoExample";
        String aggregate2 = "https://w3id.org/np/RAwCj8sM9FkB8Wyz3-i0Fh9Dcq1NniH1sErJBVEkoRQ-o/FdoExample";
        String aggregate3 = "https://w3id.org/np/RADTajQ3RJ8RNklhV8_W7B0pcJswCmm25zJPp7M-K0BRg/FdoExample";

        FdoRecord record = new FdoRecord(fdoProfile, "ComplexNanopub002", null);
        record.addAggregatedFdo(aggregate1);
        record.addAggregatedFdo(aggregate2);
        record.addAggregatedFdo(aggregate3);
        NanopubCreator creator = FdoNanopubCreator.createWithFdoSuffix(record, "complexFdoExample001");

        creator.addProvenanceStatement(PROV.WAS_ATTRIBUTED_TO, vf.createIRI("https://orcid.org/0009-0008-3635-347X"));
        Nanopub np = creator.finalizeNanopub(true);

        RDFWriter w = Rio.createWriter(RDFFormat.TRIG, new OutputStreamWriter(System.out, Charset.forName("UTF-8")));
        NanopubUtils.propagateToHandler(np, w);

        Nanopub signedNp = SignNanopub.signAndTransform(np, TransformContext.makeDefault());
        String result = PublishNanopub.publish(signedNp);
    }

//        @Test
    public void createComplexFdo2() throws Exception {

        IRI fdoProfile = vf.createIRI("https://w3id.org/np/RAudZj6DD7iByABdBXAWSsNh3kd6kj5P2bmHi6hzg1xcE/aggregate-fdo-profile");

        String aggregate1 = "12345/example1";
        String aggregate2 = "12345/example2";

        FdoRecord record = new FdoRecord(fdoProfile, "ComplexNanopub003", null);
        record.addAggregatedFdo(aggregate1);
        record.addAggregatedFdo(aggregate2);

        NanopubCreator creator = FdoNanopubCreator.createWithFdoSuffix(record, "complexFdoExample003");

        creator.addProvenanceStatement(PROV.WAS_ATTRIBUTED_TO, vf.createIRI("https://orcid.org/0009-0008-3635-347X"));
        Nanopub np = creator.finalizeNanopub(true);

        RDFWriter w = Rio.createWriter(RDFFormat.TRIG, new OutputStreamWriter(System.out, Charset.forName("UTF-8")));
        NanopubUtils.propagateToHandler(np, w);

        Nanopub signedNp = SignNanopub.signAndTransform(np, TransformContext.makeDefault());
        String result = PublishNanopub.publish(signedNp);
    }


    /**
     * Example for Op.Create derived FDO
     */
//    @Test
    public void createDerivedFdo() throws Exception {

        IRI fdoProfile = vf.createIRI("https://w3id.org/np/RABPR2eJ7dbuf_OPDLztvRZI-el2_wBFkVBiPCLmr1Q50/test-fdo-profile");

        IRI deriveFrom1 = vf.createIRI("https://w3id.org/np/RAbb0pvoFGiNwcY8nL-qSR93O4AAcfsQRS_TNvLqt0VHg/FdoExample");
        IRI deriveFrom2 = vf.createIRI("https://w3id.org/np/RAwCj8sM9FkB8Wyz3-i0Fh9Dcq1NniH1sErJBVEkoRQ-o/FdoExample");

        FdoRecord record = new FdoRecord(fdoProfile, "ExampleDerivedFdo001", null);
        record.addDerivedFromFdo(deriveFrom1);
        record.addDerivedFromFdo(deriveFrom2);
        NanopubCreator creator = FdoNanopubCreator.createWithFdoSuffix(record, "exampleDerivedFdo");

        creator.addProvenanceStatement(PROV.WAS_ATTRIBUTED_TO, vf.createIRI("https://orcid.org/0009-0008-3635-347X"));
        Nanopub np = creator.finalizeNanopub(true);

        RDFWriter w = Rio.createWriter(RDFFormat.TRIG, new OutputStreamWriter(System.out, Charset.forName("UTF-8")));
        NanopubUtils.propagateToHandler(np, w);

        Nanopub signedNp = SignNanopub.signAndTransform(np, TransformContext.makeDefault());
        PublishNanopub.publish(signedNp);
    }

//    @Test
    void specificQuery() throws Exception {
        String textSearch = "RAkYh4UPJryajbtIDbLG-Bfd6A4JD2SbU9bmZdvaEdFRY/fdo-text-search";
        String findByRef = "RAQiQjx3OiO9ra9ImWl9kpuDpT8d3EiBSrftckOAAwGKc/find-fdos-by-ref";
        String getFeed = "RAP1G35VvTs3gfMaucv_xZUMZuvjB9lxM8tWUGttr5mmo/get-fdo-feed";
        String getFavorites = "RAsyc6zFFnE8mblnDfdCCNRsrcN1CSCBDW9I4Ppidgk9g/get-favorite-things";

        ApiResponse apiResponse = QueryAccess.get(getFeed, Map.of("creator","https://orcid.org/0009-0008-3635-347X"));
        List<ApiResponseEntry> data = apiResponse.getData();
        for (ApiResponseEntry entry : data) {
            for (String k: entry.getKeys()) {
                System.out.println(k + ": " + entry.get(k));
            }
            System.out.println();
        }
    }

//    @Test
    void retractNanopubTest() throws Exception {
        Nanopub original = GetNanopub.get("https://w3id.org/np/RA1Lhd0Rt5xuz63vjeUYgGJrgeUvH-7QKwAiPgj44WWgg");
        Nanopub retraction = NanopubRetractor.createRetraction(original, TransformContext.makeDefault());
//        RDFWriter w = Rio.createWriter(RDFFormat.TRIG, new OutputStreamWriter(System.out, Charset.forName("UTF-8")));
//        NanopubUtils.propagateToHandler(retraction, w);
        PublishNanopub.publish(retraction);
    }

}
