package org.nanopub.extra.server;

import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;

/**
 * Tests that {@link FetchIndex} counts the nanopubs it gives up on.
 */
class FetchIndexFailedCountTest {

    private static final String INDEX = "https://w3id.org/np/RAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";

    @Test
    void getFailedCount_isZeroBeforeRunning() {
        try (MockedConstruction<ServerIterator> ignoredIterator = mockConstruction(ServerIterator.class);
             MockedStatic<ServerIterator> ignoredCache = mockStatic(ServerIterator.class)) {
            FetchIndex fetchIndex = newFetchIndex();

            assertEquals(0, fetchIndex.getFailedCount());
        }
    }

    @Test
    void getFailedCount_withIndexUnavailableOnEveryRegistry_isOne() {
        try (MockedConstruction<ServerIterator> ignoredIterator = mockConstruction(ServerIterator.class);
             MockedStatic<ServerIterator> ignoredCache = mockStatic(ServerIterator.class)) {
            FetchIndex fetchIndex = newFetchIndex();

            fetchIndex.run();

            assertEquals(1, fetchIndex.getFailedCount());
            assertEquals(0, fetchIndex.getNanopubCount());
        }
    }

    /**
     * Creates a fetch of the test index that writes content and index to a discarded stream.
     *
     * @return the new fetch
     */
    private static FetchIndex newFetchIndex() {
        return new FetchIndex(INDEX, new ByteArrayOutputStream(), RDFFormat.TRIG, true, true, null);
    }

}
