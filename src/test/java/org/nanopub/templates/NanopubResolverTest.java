package org.nanopub.templates;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.nanopub.Nanopub;
import org.nanopub.extra.server.GetNanopub;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

/**
 * Tests for {@link NanopubResolver#DEFAULT}.
 */
class NanopubResolverTest {

    private static final String CODE = "RAEFAt-QcFK0ZhqfvlsmS10BnzGJA0xwOICZXkO-ai87k";

    private final Nanopub nanopub = mock(Nanopub.class);

    private Nanopub retrieve(String id) {
        try (MockedStatic<GetNanopub> network = mockStatic(GetNanopub.class)) {
            network.when(() -> GetNanopub.get(CODE)).thenReturn(nanopub);
            return NanopubResolver.DEFAULT.getNanopub(id);
        }
    }

    @Test
    void nanopublicationIsRetrievedByItsUriOrArtifactCode() {
        assertSame(nanopub, retrieve("https://w3id.org/np/" + CODE));
        assertSame(nanopub, retrieve("http://purl.org/np/" + CODE));
        assertSame(nanopub, retrieve(CODE));
    }

    @Test
    void nanopublicationIsRetrievedByAnIriWithinIt() {
        // A template is named by its node where it has one of its own:
        assertSame(nanopub, retrieve("https://w3id.org/np/" + CODE + "/template"));
        assertSame(nanopub, retrieve("https://w3id.org/np/" + CODE + "/templateVersion"));
        assertSame(nanopub, retrieve("http://purl.org/np/" + CODE + "#assertion"));
    }

    @Test
    void nothingIsRetrievedWithoutAnArtifactCode() {
        try (MockedStatic<GetNanopub> network = mockStatic(GetNanopub.class)) {
            assertNull(NanopubResolver.DEFAULT.getNanopub("https://example.org/not-a-nanopub"));
            assertNull(NanopubResolver.DEFAULT.getNanopub("RAtooShort"));
            assertNull(NanopubResolver.DEFAULT.getNanopub(null));
            network.verifyNoInteractions();
        }
    }

}
