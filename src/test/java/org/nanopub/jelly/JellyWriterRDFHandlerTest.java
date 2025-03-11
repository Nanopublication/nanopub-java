package org.nanopub.jelly;

import org.junit.Test;

public class JellyWriterRDFHandlerTest {

    @Test
    public void testGetFrame() {
        var handler = new JellyWriterRDFHandler(
                JellyUtils.jellyOptionsForDB
        );
        var frame = handler.getFrame();
        assert frame.rows().isEmpty();
        assert frame.metadata() != null;
        assert frame.metadata() == JellyMetadataUtil.EMPTY_METADATA;
    }

    @Test
    public void testGetFrameWithCounter() {
        var handler = new JellyWriterRDFHandler(
                JellyUtils.jellyOptionsForDB
        );
        var frame = handler.getFrame(42);
        assert frame.rows().isEmpty();
        assert frame.metadata() != null;
        assert frame.metadata().contains(JellyMetadataUtil.COUNTER_KEY);
        assert frame.metadata().size() == 1;
        var counter = JellyMetadataUtil.tryGetCounterFromMetadata(frame.metadata());
        assert counter == 42;
    }
}
