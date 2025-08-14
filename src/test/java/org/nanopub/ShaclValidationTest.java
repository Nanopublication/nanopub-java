package org.nanopub;

import org.junit.jupiter.api.Test;
import org.nanopub.fdo.ShaclValidator;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ShaclValidationTest {

    @Test
    void testValidData() throws Exception {
        Nanopub shape = new NanopubImpl(new File("./src/test/resources/fdo/shape.trig"));
        Nanopub data = new NanopubImpl(new File("./src/test/resources/fdo/validPerson.trig"));

        assertTrue(ShaclValidator.validateShacl(shape, data).isValid());
    }

    @Test
    void testInvalidData() throws Exception {
        Nanopub shape = new NanopubImpl(new File("./src/test/resources/fdo/shape.trig"));
        Nanopub data = new NanopubImpl(new File("./src/test/resources/fdo/invalidPerson.trig"));

        assertFalse(ShaclValidator.validateShacl(shape, data).isValid());
    }

    @Test
    void exampleCliValid() throws Exception {
        ShaclValidator c = CliRunner.initJc(new ShaclValidator(), new String[]{
                "-n", "./src/test/resources/fdo/validPerson.trig",
                "-s", "./src/test/resources/fdo/shape.trig"
        });
        c.run();
    }

    @Test
    void exampleCliInvalid() throws Exception {
        ShaclValidator c = CliRunner.initJc(new ShaclValidator(), new String[]{
                "-n", "./src/test/resources/fdo/invalidPerson.trig",
                "-s", "./src/test/resources/fdo/shape.trig"
        });
        c.run();
    }

}
