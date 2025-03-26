package org.nanopub.extra.security;

import com.beust.jcommander.ParameterException;
import org.junit.jupiter.api.Test;
import org.nanopub.CliRunner;

import static org.junit.jupiter.api.Assertions.assertThrows;

class MakeKeysTest {

    @Test
    void initWithoutParams() {
        CliRunner.initJc(new MakeKeys(), new String[0]);
        // LATER we may test if keys at default location don't get overwritten
    }

    @Test
    void initInvalidParams() {
        assertThrows(ParameterException.class, () -> {
            CliRunner.initJc(new MakeKeys(), new String[] { "AnyWrong" });
        });
    }

    @Test
    void initValidParams() {

        // sig alg specified
        CliRunner.initJc(new MakeKeys(), new String[] { "-a", "RSA" });
        CliRunner.initJc(new MakeKeys(), new String[] { "-a", "DSA" });

        // path specified
        final String homeFolder = System.getProperty("user.home"); // Windows compatible since Java 8
        CliRunner.initJc(new MakeKeys(), new String[] { "-f", homeFolder });

        // both specified
        CliRunner.initJc(new MakeKeys(), new String[] { "-a", "DSA", "-f", homeFolder });
    }

    // for now, we expect the making of keys is tested elsewhere
    void make() {
    }
}