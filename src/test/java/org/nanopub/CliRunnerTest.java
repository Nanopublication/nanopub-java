package org.nanopub;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CliRunnerTest {

    /**
     * A minimal runner, so that the argument handling can be exercised without pulling in one of
     * the real command-line tools.
     */
    private static class TestRunner extends CliRunner {

        @Parameter(names = "-x", description = "some value")
        private String value;

    }

    @Test
    void initJcParsesTheArguments() {
        TestRunner runner = CliRunner.initJc(new TestRunner(), new String[]{"-x", "hello"});

        assertEquals("hello", runner.value);
        assertNotNull(runner.getJc());
    }

    @Test
    void initJcRejectsUnknownArguments() {
        assertThrows(ParameterException.class,
                () -> CliRunner.initJc(new TestRunner(), new String[]{"-unknown"}));
    }

    @Test
    void logOrSysoutLogsWhenDebugIsEnabled() {
        Logger logger = mock(Logger.class);
        when(logger.isDebugEnabled()).thenReturn(true);

        CliRunner.logOrSysout(logger, "a message");

        verify(logger).debug("a message");
    }

    @Test
    void logOrSysoutFallsBackToStandardOutput() {
        Logger logger = mock(Logger.class);
        when(logger.isDebugEnabled()).thenReturn(false);

        CliRunner.logOrSysout(logger, "a message");

        verify(logger, never()).debug(anyString());
    }

}
