package org.nanopub.jelly;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class JellyWriterRDFHandlerTest {

    @Test
    public void testGetFrame() {
        var handler = new JellyWriterRDFHandler(
                JellyUtils.jellyOptionsForDB
        );
        var frame = handler.getFrame();
        assertTrue(frame.getRows().isEmpty());
        assertNotNull(frame.getMetadata());
        assertTrue(frame.getMetadata().isEmpty());
    }

    @Test
    public void testGetFrameWithCounter() {
        var handler = new JellyWriterRDFHandler(
                JellyUtils.jellyOptionsForDB
        );
        var frame = handler.getFrame(42);
        assertTrue(frame.getRows().isEmpty());
        assertNotNull(frame.getMetadata());
        assertEquals(1, frame.getMetadata().size());
        var counter = JellyMetadataUtil.tryGetCounterFromMetadata(frame.getMetadata());
        assertEquals(42, counter);
    }
}
