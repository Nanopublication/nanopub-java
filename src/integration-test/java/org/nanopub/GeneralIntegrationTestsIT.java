package org.nanopub;

import org.apache.commons.io.IOUtils;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.PROV;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.nanopub.extra.security.MakeKeys;
import org.nanopub.extra.security.SignNanopub;
import org.nanopub.extra.security.SignatureAlgorithm;
import org.nanopub.extra.security.TransformContext;
import org.nanopub.extra.server.PublishNanopub;
import org.nanopub.fdo.FdoNanopubCreator;
import org.nanopub.fdo.FdoRecord;
import org.nanopub.fdo.FdoUtils;
import org.nanopub.fdo.RetrieveFdo;
import org.nanopub.fdo.rest.HandleResolver;
import org.nanopub.vocabulary.FDOC;
import org.nanopub.vocabulary.NPX;
import org.nanopub.vocabulary.NTEMPLATE;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.security.KeyPair;
import java.util.Random;
import java.util.Set;

import static java.lang.System.out;
import static org.eclipse.rdf4j.model.util.Values.literal;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.nanopub.fdo.ValidateFdo.createShaclValidationShapeFromJson;

/**
 * Integration Tests must have the suffix "IT".
 */
public class GeneralIntegrationTestsIT {

    Random random = new Random();

    @BeforeAll
    public static void makeSureKeysAreAvailable() throws IOException {
        String keyPath = System.getProperty("user.home") + "/.nanopub/id";
        try {
            MakeKeys.make(keyPath, SignatureAlgorithm.RSA);
        } catch (FileAlreadyExistsException e) {
            // all fine, the key does exist
        }
    }

    @Test
    void createNanopubFromHandleSystem() throws URISyntaxException, IOException, InterruptedException, MalformedNanopubException, NanopubAlreadyFinalizedException {
        String id = "21.T11967/39b0ec87d17a4856c5f7";
        Nanopub np = FdoNanopubCreator.createFromHandleSystem(id);

        RDFWriter w = Rio.createWriter(RDFFormat.TRIG, new OutputStreamWriter(out, StandardCharsets.UTF_8));
        NanopubUtils.propagateToHandler(np, w);
    }

    //    @Test
    void exampleForPublishingFdoNanopub() throws Exception {
        String id = "21.T11967/39b0ec87d17a4856c5f7"; // TODO enter the handle id
        Nanopub np = FdoNanopubCreator.createFromHandleSystem(id);

        String signer = "https://orcid.org/0009-0008-3635-347X"; // TODO enter your orcid

        KeyPair key = SignNanopub.loadKey(this.getClass().getResource("/testsuite/transform/signed/rsa-key1/key/id_rsa").getPath(), SignatureAlgorithm.RSA);
        TransformContext context = new TransformContext(SignatureAlgorithm.RSA, key, Values.iri(signer), true, true, true);
        Nanopub signedNp = SignNanopub.signAndTransform(np, context);
        PublishNanopub.publish(signedNp);
    }

    @Test
    void testRetrieveContentFromNpNetwork() throws Exception {
        String id = "https://w3id.org/np/RAsSeIyT03LnZt3QvtwUqIHSCJHWW1YeLkyu66Lg4FeBk/nanodash-readme";
        InputStream in = RetrieveFdo.retrieveContentFromId(id);
        byte[] buffer = new byte[256];
        IOUtils.readFully(in, buffer);
        String result = new String(buffer, StandardCharsets.UTF_8);
        assertTrue(result.startsWith("Nanodash"));
    }

    @Test
    void retrieveRecordFromHandleSystem() throws Exception {
        String id = "21.T11967/39b0ec87d17a4856c5f7";
        FdoRecord record = RetrieveFdo.resolveId(id);
        Assertions.assertEquals(FdoUtils.createIri(id), record.getId());

        Nanopub np = FdoNanopubCreator.createFromHandleSystem(id);

        for (Statement st : np.getAssertion()) {
//            assertTrue(record.buildStatements().contains(st));
            // TODO we do need a new example here
        }
    }

    //     @Test
    void exampleForUpdatingFdoNanopub() throws Exception {
        String id = "https://w3id.org/np/RAproAPfRNhcGoaa0zJ1lsZ_-fRsnlDLLC3nv5guyUWRo/FdoExample";
        FdoRecord record = RetrieveFdo.resolveId(id);
        record.setAttribute(FdoUtils.toIri("handleToUpdate"),
                literal("New-Value-" + random.nextInt()));
        String signer = "https://orcid.org/0009-0008-3635-347X"; // TODO enter your orcid
        // for updating the original nanopub must be signed with the same key
//        KeyPair key = SignNanopub.loadKey(this.getClass().getResource("/testsuite/transform/signed/rsa-key1/key/id_rsa").getPath(), SignatureAlgorithm.RSA);
//        TransformContext context = new TransformContext(SignatureAlgorithm.RSA, key, vf.createIRI(signer), true, true, true);
        TransformContext context = TransformContext.makeDefault();
        NanopubCreator creator = record.createUpdatedNanopub(context);

        Nanopub newNp = creator.finalizeNanopub(true);

        Nanopub signedNp = SignNanopub.signAndTransform(newNp, context);
        PublishNanopub.publish(signedNp);
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
        creator.addProvenanceStatement(PROV.WAS_DERIVED_FROM, Values.iri(HandleResolver.BASE_URI + profileId));

        String shapeIri = creator.getNanopubUri().stringValue();
        Set<Statement> shaclShape = createShaclValidationShapeFromJson(httpResponse, shapeIri);
        creator.addAssertionStatements(shaclShape);

        creator.addAssertionStatement(fdoIri, RDF.TYPE, FDOC.FDO_PROFILE);
        creator.addAssertionStatement(fdoIri, FDOC.HAS_SHAPE, Values.iri(shapeIri + "nodeShape"));

        creator.addPubinfoStatement(NPX.HAS_NANOPUB_TYPE, NTEMPLATE.ASSERTION_TEMPLATE);
        creator.addNamespace(SHACL.PREFIX, SHACL.NAMESPACE);
        Nanopub np = creator.finalizeNanopub(true);
        Nanopub signedNp = SignNanopub.signAndTransform(np, TransformContext.makeDefault());

        NanopubUtils.writeToStream(signedNp, System.err, RDFFormat.TRIG);

//        PublishNanopub.publish(signedNp);
    }

}