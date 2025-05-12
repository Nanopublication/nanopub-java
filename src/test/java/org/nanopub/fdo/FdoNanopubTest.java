package org.nanopub.fdo;

import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.PROV;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.nanopub.*;
import org.nanopub.fdo.rest.HandleResolver;
import org.nanopub.fdo.rest.ResponsePrinter;
import org.nanopub.fdo.rest.gson.Response;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URISyntaxException;
import java.nio.charset.Charset;

import static java.lang.System.out;

public class FdoNanopubTest {

    private static final ValueFactory vf = SimpleValueFactory.getInstance();

    @Test
    void example() throws MalformedNanopubException {
        FdoNanopubCreator creator = new FdoNanopubCreator("21.T11967/39b0ec87d17a4856c5f7", "21.T11966/365ff9576c26ca6053db","NumberFdo1" );

        NanopubCreator underlyingNpCreator = creator.getNanopubCreator();
        underlyingNpCreator.addProvenanceStatement(PROV.ATTRIBUTION, vf.createIRI("https://orcid.org/0000-0000-0000-0000"));

        Nanopub np = underlyingNpCreator.finalizeNanopub(true);

        RDFWriter w = Rio.createWriter(RDFFormat.TRIG, new OutputStreamWriter(out, Charset.forName("UTF-8")));
        NanopubUtils.propagateToHandler(np, w);

    }

    @Test
    void testFdoNanopub() throws MalformedNanopubException {
        String profileHandle = "21.T11966/365ff9576c26ca6053db";
        String label = "NumberFdo1";
        FdoNanopubCreator creator = new FdoNanopubCreator("21.T11967/39b0ec87d17a4856c5f7", profileHandle, label);

        NanopubCreator underlyingNpCreator = creator.getNanopubCreator();
        underlyingNpCreator.addProvenanceStatement(PROV.ATTRIBUTION, vf.createIRI("https://orcid.org/0000-0000-0000-0000"));
        Nanopub np = underlyingNpCreator.finalizeNanopub(true);

        FdoNanopub fdoNanopub = new FdoNanopub(np);
        Assert.assertEquals(FdoUtils.toIri(profileHandle), fdoNanopub.getProfile());
        Assert.assertEquals(label, fdoNanopub.getLabel());
    }

    @Test
    void testInvalidFdoNanopub() throws MalformedNanopubException {
        Nanopub np = new NanopubUtilsTest().createNanopub();
        Assert.assertThrows(IllegalArgumentException.class, () -> new FdoNanopub(np));
    }

    @Test
    void exampleRestCall() throws URISyntaxException, IOException, InterruptedException {
//        String id = "4263537/4000";
        String id = "21.T11967/39b0ec87d17a4856c5f7";
        Response response = new HandleResolver().call(id);

        ResponsePrinter.print(response);
    }

    @Test
    void testLooksLikeHandle () {
        Assert.assertTrue(FdoUtils.looksLikeHandle("21.T11967/39b0ec87d17a4856c5f7"));
        Assert.assertTrue(FdoUtils.looksLikeHandle("21.T11966/82045bd97a0acce88378"));
        Assert.assertTrue(FdoUtils.looksLikeHandle("4263537/4000"));

        Assert.assertFalse(FdoUtils.looksLikeHandle("this is not a valid handle"));
        Assert.assertFalse(FdoUtils.looksLikeHandle("https://this_is_no_handle"));
        Assert.assertFalse(FdoUtils.looksLikeHandle("21.T11966"));
    }

    @Test
    void testLooksLikeUrl () {
        Assert.assertTrue(FdoUtils.looksLikeUrl("https://this_may_be_an_url.com"));
        Assert.assertTrue(FdoUtils.looksLikeUrl("https://www.knowledgepixesl.com"));
        Assert.assertTrue(FdoUtils.looksLikeUrl("https://hdl.handle.net/api/handles/4263537/4000"));
        Assert.assertTrue(FdoUtils.looksLikeUrl("https://hdl.handle.net"));

        Assert.assertFalse(FdoUtils.looksLikeUrl("https://this_is_no_url"));
        Assert.assertFalse(FdoUtils.looksLikeUrl("this is not a valid url"));
    }

}
