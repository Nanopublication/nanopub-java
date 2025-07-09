package org.nanopub.jelly;

import org.junit.Test;

public class JellyWriterRDFHandlerTest {

    @Test
    public void testGetFrame() {
        var handler = new JellyWriterRDFHandler(
                JellyUtils.jellyOptionsForDB
        );
        var frame = handler.getFrame();
        assert frame.getRows().isEmpty();
        assert frame.getMetadata() != null;
        assert frame.getMetadata().isEmpty();
    }

    @Test
    public void testGetFrameWithCounter() {
        var handler = new JellyWriterRDFHandler(
                JellyUtils.jellyOptionsForDB
        );
        var frame = handler.getFrame(42);
        assert frame.getRows().isEmpty();
        assert frame.getMetadata() != null;
        assert frame.getMetadata().size() == 1;
        var counter = JellyMetadataUtil.tryGetCounterFromMetadata(frame.getMetadata());
        assert counter == 42;
    }
}
