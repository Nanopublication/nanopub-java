package org.nanopub.op;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.beust.jcommander.ParameterException;

/**
 * Tests the plumbing that every tool in this package shares. The tools
 * themselves only need to check their own behaviour, since the mapping from an
 * outcome to an exit code lives here.
 */
class CliSupportTest {

    @Test
    void aCompletedRunSucceeds() {
        assertEquals(0, CliSupport.execute(() -> {
        }));
    }

    @Test
    void badArgumentsFailWithoutFurtherReporting() {
        String printed = captureStandardError(()
                -> assertEquals(1, CliSupport.execute(() -> {
                    throw new ParameterException("missing argument");
                })));

        // JCommander has already printed the usage, so nothing is added here
        assertTrue(printed.isBlank(), printed);
    }

    @Test
    void anyOtherFailureIsReportedWithItsStackTrace() {
        String printed = captureStandardError(()
                -> assertEquals(1, CliSupport.execute(() -> {
                    throw new IOException("no such file");
                })));

        assertTrue(printed.contains("java.io.IOException: no such file"), printed);
    }

    @Test
    void exitWithDoesNothingForASuccessfulStatus() {
        // if this called System.exit the test JVM would not survive to make the assertion
        assertDoesNotThrow(() -> CliSupport.exitWith(0));
    }

    private static String captureStandardError(Runnable action) {
        PrintStream originalErr = System.err;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        try {
            System.setErr(new PrintStream(captured, true, StandardCharsets.UTF_8));
            action.run();
        } finally {
            System.setErr(originalErr);
        }
        return captured.toString(StandardCharsets.UTF_8);
    }

}
