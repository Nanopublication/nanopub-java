package org.nanopub;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.eclipse.rdf4j.rio.RDFFormat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.MockedStatic;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import org.nanopub.extra.security.SignNanopub;
import org.nanopub.extra.security.TransformContext;
import org.nanopub.extra.server.PublishNanopub;
import org.nanopub.utils.TestUtils;

/**
 * Tests for the default methods of the {@link Nanopub} interface.
 */
class NanopubTest {

    @Test
    void writeToStream() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        Nanopub nanopub = TestUtils.createNanopub();

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        nanopub.writeToStream(os, RDFFormat.TRIG);

        assertTrue(os.toString().contains(nanopub.getUri().toString()));
    }

    @Test
    void writeToString() throws MalformedNanopubException, NanopubAlreadyFinalizedException, IOException {
        Nanopub nanopub = TestUtils.createNanopub();

        String output = nanopub.writeToString(RDFFormat.TRIG);

        assertTrue(output.contains(nanopub.getUri().toString()));
    }

    @Test
    void publishToDefaultServer() throws MalformedNanopubException, NanopubAlreadyFinalizedException, IOException {
        Nanopub nanopub = TestUtils.createNanopub();

        try (MockedStatic<PublishNanopub> publishMock = mockStatic(PublishNanopub.class)) {
            publishMock.when(() -> PublishNanopub.publish(nanopub)).thenReturn("published");

            assertEquals("published", nanopub.publish());
        }
    }

    @Test
    void publishToGivenServer() throws MalformedNanopubException, NanopubAlreadyFinalizedException, IOException {
        Nanopub nanopub = TestUtils.createNanopub();
        String serverUrl = "https://example.org/np-server/";

        try (MockedStatic<PublishNanopub> publishMock = mockStatic(PublishNanopub.class)) {
            publishMock.when(() -> PublishNanopub.publish(nanopub, serverUrl)).thenReturn("published there");

            assertEquals("published there", nanopub.publish(serverUrl));
        }
    }

    @Test
    void sign() throws Exception {
        Nanopub nanopub = TestUtils.createNanopub();
        Nanopub signed = TestUtils.createNanopub("https://knowledgepixels.com/nanopubIri#signed");
        TransformContext context = mock(TransformContext.class);

        try (MockedStatic<SignNanopub> signMock = mockStatic(SignNanopub.class)) {
            signMock.when(() -> SignNanopub.signAndTransform(eq(nanopub), any(TransformContext.class))).thenReturn(signed);

            assertEquals(signed, nanopub.sign(context));
        }
    }

}
