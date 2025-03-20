package org.nanopub.extra.security;

import com.beust.jcommander.ParameterException;
import com.google.common.collect.Lists;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class MakeKeysTest {

    @Test
    void initWithoutParams() {
        MakeKeys.init(new String[0]);
        // LATER we may test if keys at default location don't get overwritten
    }

    @Test
    void initInvalidParams() {
        assertThrows(ParameterException.class, () -> {
            MakeKeys.init(new String[] { "AnyWrong" });
        });
    }

    @Test
    void initValidParams() {

        // sig alg specified
        MakeKeys.init(new String[] { "-a", "RSA" });
        MakeKeys.init(new String[] { "-a", "DSA" });

        // path specified
        final String homeFolder = System.getProperty("user.home"); // Windows compatible since Java 8
        MakeKeys.init(new String[] { "-f", homeFolder });

        // both specified
        MakeKeys.init(new String[] { "-a", "DSA", "-f", homeFolder });
    }

    // for now, we expect the making of keys is tested elsewhere
    void make() {
    }
}