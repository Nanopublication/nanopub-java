package org.nanopub;

import org.apache.commons.io.IOUtils;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.PROV;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.nanopub.extra.security.SignNanopub;
import org.nanopub.extra.security.SignatureAlgorithm;
import org.nanopub.extra.security.TransformContext;
import org.nanopub.extra.server.GetNanopub;
import org.nanopub.extra.server.PublishNanopub;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.ApiResponseEntry;
import org.nanopub.extra.services.FailedApiCallException;
import org.nanopub.extra.services.QueryAccess;
import org.nanopub.fdo.*;
import org.nanopub.fdo.rest.HandleResolver;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.security.KeyPair;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.lang.System.out;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.nanopub.NanopubUtils.HAS_NANOPUB_TYPE;
import static org.nanopub.fdo.ValidateFdo.createShaclValidationShapeFromJson;

/**
 * Integration Tests must have suffix "IT".
 */
public class TheseTestsRequireOtherSystemsIT {

    final ValueFactory vf = SimpleValueFactory.getInstance();

    @Test
    public void testSimpleQuery() throws Exception {

        // url = "https://w3id.org/np/RAPf3B_OK7X4oN7c-PLztmiuL0dIV94joR6WydTjA6Asc#get-most-frequent-a-pred"
        String queryId = "RAPf3B_OK7X4oN7c-PLztmiuL0dIV94joR6WydTjA6Asc/get-most-frequent-a-pred";
        ApiResponse apiResponse = QueryAccess.get(queryId, null);
        List<ApiResponseEntry> data = apiResponse.getData();
        for (ApiResponseEntry entry : data) {
            String result = entry.get("pred");
            assertNotNull(result);
        }
    }

    @Test
    public void testQueryNanopubNetworkWithFdoHandle () throws Exception {
        Map<String, String> params = new HashMap<>();
        params.put("fdoid", "21.T11967/39b0ec87d17a4856c5f7");
        ApiResponse apiResponse = QueryAccess.get(RetrieveFdo.GET_FDO_QUERY_ID, params);
        List<ApiResponseEntry> data = apiResponse.getData();
        String npref = data.get(0).get("np");

        Nanopub np = GetNanopub.get(npref);
        System.out.println("npref: " + npref);
        assertEquals(npref, np.getUri().stringValue());
    }

    @Test
    public void testQueryNanopubNetworkNpUriForFdo () throws FailedApiCallException {
        Map<String, String> params = new HashMap<>();
        params.put("fdoid", "https://w3id.org/np/RAsSeIyT03LnZt3QvtwUqIHSCJHWW1YeLkyu66Lg4FeBk/nanodash-readme");
        ApiResponse apiResponse = QueryAccess.get(RetrieveFdo.GET_FDO_QUERY_ID, params);
        List<ApiResponseEntry> data = apiResponse.getData();
        String npref = data.get(0).get("np");

        Nanopub np = GetNanopub.get(npref);
        System.out.println("npref: " + npref);
        assertEquals(npref, np.getUri().stringValue());
    }

    @Test
    void createNanopubFromHandleSystem() throws URISyntaxException, IOException, InterruptedException, MalformedNanopubException {
        String id = "21.T11967/39b0ec87d17a4856c5f7";
        Nanopub np = FdoNanopubCreator.createFromHandleSystem(id);

        RDFWriter w = Rio.createWriter(RDFFormat.TRIG, new OutputStreamWriter(out, Charset.forName("UTF-8")));
        NanopubUtils.propagateToHandler(np, w);
    }

    void exampleForPublishingFdoNanopub() throws Exception {
        String id = "21.T11967/39b0ec87d17a4856c5f7"; // TODO enter the handle id
        Nanopub np = FdoNanopubCreator.createFromHandleSystem(id);

        ValueFactory vf = SimpleValueFactory.getInstance();
        String signer = "https://orcid.org/0009-0008-3635-347X"; // TODO enter your orcid

        KeyPair key = SignNanopub.loadKey("src/test/resources/testsuite/transform/signed/rsa-key1/key/id_rsa", SignatureAlgorithm.RSA);
        TransformContext context = new TransformContext(SignatureAlgorithm.RSA, key, vf.createIRI(signer), true, true, true);
        Nanopub signedNp = SignNanopub.signAndTransform(np, context);
        PublishNanopub.publish(signedNp);
    }

    @Test
    void testRetrieveContentFromNpNetwork() throws Exception {
        String id = "https://w3id.org/np/RAsSeIyT03LnZt3QvtwUqIHSCJHWW1YeLkyu66Lg4FeBk/nanodash-readme";
        InputStream in = RetrieveFdo.retrieveContentFromId(id);
        byte[] buffer = new byte[256];
        IOUtils.readFully(in, buffer);
        String result = new String(buffer, Charset.forName("UTF-8"));
        Assert.assertTrue(result.startsWith("Nanodash"));
    }

    @Test
    void retrieveRecordFromHandleSystem() throws Exception {
        String id = "21.T11967/39b0ec87d17a4856c5f7";
        FdoRecord record = RetrieveFdo.resolveId(id);
        assertEquals(FdoUtils.createIri(id), record.getId());

        Nanopub np = FdoNanopubCreator.createFromHandleSystem(id);
        assertEquals(record.buildStatements().size(), np.getAssertion().size());
        // it would be great to compare all statements here
    }

    @Test
    void validateValidFdo() throws Exception {
        String id = "21.T11966/82045bd97a0acce88378";
        FdoRecord record = RetrieveFdo.resolveId(id);

        Assert.assertTrue(ValidateFdo.validate(record).isValid());
    }

    @Test
    void validateInvalidFdo() throws Exception {
        String id = "21.T11967/39b0ec87d17a4856c5f7";
        FdoRecord record = RetrieveFdo.resolveId(id);

        Assert.assertFalse(ValidateFdo.validate(record).isValid());
    }

    @Test
    void createNpFromProfileJson() throws Exception {
        String profileId = "21.T11966/82045bd97a0acce88378"; // the handle of the profile

        FdoRecord fdoRecord = FdoNanopubCreator.createFdoRecordFromHandleSystem(profileId);
        String schemaUrl = fdoRecord.getSchemaUrl();

        // Get the spec from profile.json
        HttpRequest req = HttpRequest.newBuilder().GET().uri(new URI(schemaUrl)).build();
        HttpResponse<String> httpResponse = HttpClient.newHttpClient().send(req, HttpResponse.BodyHandlers.ofString());

        // create nanopub
        IRI fdoIri = FdoUtils.createIri(profileId);
        NanopubCreator creator = FdoNanopubCreator.createWithFdoIri(fdoRecord, fdoIri);
        creator.addProvenanceStatement(PROV.WAS_DERIVED_FROM, vf.createIRI(HandleResolver.BASE_URI+profileId));

        String shapeIri = creator.getNanopubUri().stringValue();
        Set<Statement> shaclShape = createShaclValidationShapeFromJson(httpResponse, shapeIri);
        creator.addAssertionStatements(shaclShape);

        creator.addAssertionStatement(fdoIri, RDF.TYPE, FdoUtils.PROFILE_CLASS_IRI);
        creator.addAssertionStatement(fdoIri, FdoUtils.SHAPE_LINK_IRI, vf.createIRI(shapeIri+"nodeShape"));

        creator.addPubinfoStatement(HAS_NANOPUB_TYPE, vf.createIRI("https://w3id.org/np/o/ntemplate/AssertionTemplate"));
        creator.addNamespace("shacl", vf.createIRI("http://www.w3.org/ns/shacl#"));
        Nanopub np = creator.finalizeNanopub(true);
        Nanopub signedNp = SignNanopub.signAndTransform(np, TransformContext.makeDefault());

        NanopubUtils.writeToStream(signedNp, System.err, RDFFormat.TRIG);

//        PublishNanopub.publish(signedNp);
    }

}