package org.nanopub.jelly;

import org.junit.jupiter.api.Test;
import org.nanopub.MalformedNanopubException;
import org.nanopub.Nanopub;
import org.nanopub.NanopubImpl;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JellyUtilsTest {

    @Test
    public void testDbRoundTrip() throws MalformedNanopubException, IOException {
        Nanopub np = new NanopubImpl(new File("./src/test/resources/fdo/validPerson.trig"));
        byte[] bytes = JellyUtils.writeNanopubForDB(np);
        Nanopub np2 = JellyUtils.readFromDB(bytes);
        assertEquals(np, np2);
    }
}
