package org.nanopub.extra.server;

import net.trustyuri.ArtifactCode;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.nanopub.CliRunner;
import org.nanopub.Nanopub;
import org.nanopub.NanopubImpl;
import org.nanopub.testsuite.NanopubTestSuite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Tests the exit status the {@link NanopubStatus} command reports.
 */
class NanopubStatusExitStatusTest {

    private static final ArtifactCode ARTIFACT_CODE = ArtifactCode.of("RAWH0fe1RCpoOgaJE1B2qfTzzdTiBUUK7iIk6l7Zll9mg");

    @Test
    void exitStatus_withNanopubFound_isZero() throws Exception {
        Nanopub nanopub = new NanopubImpl(NanopubTestSuite.getLatest()
                .getByArtifactCode(ARTIFACT_CODE.toString()).getFirst().toFile());
        try (MockedConstruction<ServerIterator> ignoredIterator = mockConstruction(ServerIterator.class, NanopubStatusExitStatusTest::yieldOneRegistry);
             MockedStatic<GetNanopub> getNanopub = mockStatic(GetNanopub.class)) {
            getNanopub.when(() -> GetNanopub.get(anyString(), any(RegistryInfo.class))).thenReturn(nanopub);

            assertEquals(0, runStatus().exitStatus());
        }
    }

    @Test
    void exitStatus_withNanopubNotFound_isOne() throws Exception {
        try (MockedConstruction<ServerIterator> ignoredIterator = mockConstruction(ServerIterator.class, NanopubStatusExitStatusTest::yieldOneRegistry);
             MockedStatic<GetNanopub> ignoredGet = mockStatic(GetNanopub.class)) {
            assertEquals(1, runStatus().exitStatus());
        }
    }

    @Test
    void exitStatus_withNoRegistryAvailable_isOne() throws Exception {
        try (MockedConstruction<ServerIterator> ignoredIterator = mockConstruction(ServerIterator.class)) {
            assertEquals(1, runStatus().exitStatus());
        }
    }

    /**
     * Runs the status command on the test artifact code.
     *
     * @return the command after its run
     * @throws Exception if the command cannot be set up or run
     */
    private static NanopubStatus runStatus() throws Exception {
        NanopubStatus status = CliRunner.initJc(new NanopubStatus(), new String[]{ARTIFACT_CODE.toString()});
        status.run();
        return status;
    }

    /**
     * Makes a mocked server iterator yield a single registry.
     *
     * @param iterator the mocked iterator
     * @param context  the construction context
     */
    private static void yieldOneRegistry(ServerIterator iterator, MockedConstruction.Context context) {
        when(iterator.hasNext()).thenReturn(true, false);
        when(iterator.next()).thenReturn(mock(RegistryInfo.class));
    }

}
