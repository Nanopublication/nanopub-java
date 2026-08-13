package org.nanopub.jelly;

import org.junit.jupiter.api.Test;
import org.nanopub.MalformedNanopubException;
import org.nanopub.Nanopub;
import org.nanopub.NanopubImpl;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class JellyUtilsTest {

    @Test
    public void testDbRoundTrip() throws MalformedNanopubException, IOException {
        Nanopub np = new NanopubImpl(new File("./src/test/resources/fdo/validPerson.trig"));
        byte[] bytes = JellyUtils.writeNanopubForDB(np);
        Nanopub np2 = JellyUtils.readFromDB(bytes);
        assertEquals(np, np2);
    }

    @Test
    public void testInputStreamRoundTrip() throws Exception {
        Nanopub np = new NanopubImpl(new File("./src/test/resources/fdo/validPerson.trig"));
        JellyWriterRDFHandler handler = new JellyWriterRDFHandler(JellyUtils.jellyOptionsForDB);
        org.nanopub.NanopubUtils.propagateToHandler(np, handler);

        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        handler.getFrame().writeDelimitedTo(out);

        Nanopub read = JellyUtils.readFromInputStream(new java.io.ByteArrayInputStream(out.toByteArray()));

        assertEquals(np, read);
    }

    @Test
    public void testReadFromDbWithUnparseableBytes() {
        byte[] notJelly = {(byte) 0xFF, (byte) 0xFF, (byte) 0xFF};

        MalformedNanopubException ex = assertThrows(MalformedNanopubException.class,
                () -> JellyUtils.readFromDB(notJelly));
        assertTrue(ex.getMessage().startsWith("Failed to parse Jelly RDF bytes as a Nanopub: "), ex.getMessage());
    }

    @Test
    public void testReadFromInputStreamWithAFailingStream() {
        java.io.InputStream failing = new java.io.InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("connection reset");
            }
        };

        MalformedNanopubException ex = assertThrows(MalformedNanopubException.class,
                () -> JellyUtils.readFromInputStream(failing));
        assertTrue(ex.getMessage().startsWith("Failed to read Jelly RDF from InputStream: "), ex.getMessage());
    }

    @Test
    public void testUtilityClassesCannotBeInstantiated() throws Exception {
        // both only hold static members, so they declare a private constructor rather than
        // leaving the implicit public one in their API
        for (Class<?> type : java.util.List.of(JellyUtils.class, JellyMetadataUtil.class)) {
            var constructors = type.getDeclaredConstructors();
            assertEquals(1, constructors.length, type + " should declare exactly one constructor");
            assertTrue(java.lang.reflect.Modifier.isPrivate(constructors[0].getModifiers()),
                    type + " must declare a private constructor");
            assertThrows(IllegalAccessException.class, () -> type.getDeclaredConstructor().newInstance());
        }
    }

}
