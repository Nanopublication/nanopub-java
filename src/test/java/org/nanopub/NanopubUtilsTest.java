package org.nanopub;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NanopubUtilsTest {

    @BeforeEach
    void setUp() {
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void getDefaultNamespaces() {
        assertThat(NanopubUtils.getDefaultNamespaces()).isNotEmpty();
    }

    @Test
    void getStatements() {
//        assertThat(NanopubUtils.getStatements())
    }

    @Test
    void writeToStream() {
    }

    @Test
    void writeToString() {
    }

    @Test
    void propagateToHandler() {
    }

    @Test
    void getParser() {
    }

    @Test
    void getUsedPrefixes() {
    }

    @Test
    void getLabel() {
    }

    @Test
    void getDescription() {
    }

    @Test
    void getTypes() {
    }

    @Test
    void updateXorChecksum() {
    }

    @Test
    void getHttpClient() {
    }
}