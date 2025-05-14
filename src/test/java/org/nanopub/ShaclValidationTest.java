package org.nanopub;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.nanopub.fdo.FdoUtils;

import java.io.File;

public class ShaclValidationTest {

    @Test
    void testValidData() throws Exception {
        Nanopub shape = new NanopubImpl(new File("./src/test/resources/fdo/shape.trig"));
        Nanopub data = new NanopubImpl(new File("./src/test/resources/fdo/validPerson.trig"));

        Assert.assertTrue(FdoUtils.validateShacl(shape, data));
    }

    @Test
    void testInvalidData() throws Exception {
        Nanopub shape = new NanopubImpl(new File("./src/test/resources/fdo/shape.trig"));
        Nanopub data = new NanopubImpl(new File("./src/test/resources/fdo/invalidPerson.trig"));

        Assert.assertFalse(FdoUtils.validateShacl(shape, data));
    }

}
