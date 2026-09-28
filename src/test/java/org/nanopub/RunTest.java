package org.nanopub;

import jakarta.xml.bind.DatatypeConverter;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Tests the command dispatcher. Only the dispatching path is covered here: every other branch of
 * {@link Run#run(String[])} ends in {@code System.exit}, which would take the test JVM down with it.
 */
class RunTest {

    private static String captureStandardOutput(Runnable action) {
        PrintStream originalOut = System.out;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        try {
            System.setOut(new PrintStream(captured, true, StandardCharsets.UTF_8));
            action.run();
        } finally {
            System.setOut(originalOut);
        }
        return captured.toString(StandardCharsets.UTF_8);
    }

    @Test
    void runsACommandByItsShortcut() {
        String printed = captureStandardOutput(() -> assertDoesNotThrow(() -> Run.run(new String[]{"now"})));

        assertDoesNotThrow(() -> DatatypeConverter.parseDateTime(printed.trim()));
    }

    @Test
    void runsACommandByItsClassName() {
        String printed = captureStandardOutput(() -> assertDoesNotThrow(() -> Run.run(new String[]{"TimestampNow"})));

        assertDoesNotThrow(() -> DatatypeConverter.parseDateTime(printed.trim()));
    }

    @Test
    void passesTheRemainingArgumentsOnToTheCommand() {
        // "now" takes no arguments of its own, so the dispatcher has to hand it an empty array
        String printed = captureStandardOutput(() -> assertDoesNotThrow(() -> Run.run(new String[]{"now"})));

        assertFalse(printed.isBlank());
    }

    @Test
    void mainDispatchesToTheNamedCommand() {
        String printed = captureStandardOutput(() -> assertDoesNotThrow(() -> Run.main(new String[]{"now"})));

        assertDoesNotThrow(() -> DatatypeConverter.parseDateTime(printed.trim()));
    }

}
