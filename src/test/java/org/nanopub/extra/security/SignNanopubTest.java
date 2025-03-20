package org.nanopub.extra.security;

import com.beust.jcommander.ParameterException;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class SignNanopubTest {

    @Test
    void initWithoutArgs() throws IOException {
        String[] args = new String[0];
        try {
            SignNanopub obj = SignNanopub.init(args);
            fail("Should have thrown a Parameter exception (Which main translates to Exit Code: 1)");
        } catch (ParameterException e) {
            // We assume the Usage String printed out
            System.out.println("All good!");
            System.out.println(e.getMessage());
            assertTrue(true);
        }

    }

    @Test
    void initWithValidArgs() throws Exception {
        String[] args = new String[] {"-v", "/Users/zip/repos/pixels/nanopub-java/src/main/resources/testsuite/valid/plain/aida1.trig"};

        SignNanopub obj = SignNanopub.init(args);
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