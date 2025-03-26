package org.nanopub.extra.security;

import com.beust.jcommander.ParameterException;
import org.junit.jupiter.api.Test;
import org.nanopub.CliRunner;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

class SignNanopubTest {

    @Test
    void initWithoutArgs() throws IOException {
        assertThrowsExactly(ParameterException.class, () -> CliRunner.initJc(new SignNanopub(), new String[0]));
    }

    @Test
    void initWithValidArgs() throws Exception {
        String path = "src/main/resources/testsuite/valid/plain/aida1.trig";
        String[] args = new String[] {"-v", path};

        CliRunner.initJc(new SignNanopub(), args);
    }

    // For now we assume, that most signing issues were detected by other tests
    void signAndTransform() {
    }

    void signAndTransformMultiNanopub() {
    }

    void testSignAndTransformMultiNanopub() {
    }

    void writeAsSignedTrustyNanopub() {
    }

    void loadKey() {
    }

}