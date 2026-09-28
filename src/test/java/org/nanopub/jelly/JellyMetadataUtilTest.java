package org.nanopub.jelly;

import com.google.protobuf.ByteString;
import eu.neverblink.jelly.core.proto.v1.RdfStreamFrame;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class JellyMetadataUtilTest {

    private final long[] testCases = {0, 1, 200, Integer.MAX_VALUE, Long.MAX_VALUE};

    @Test
    public void testCounterMetadataRoundTrip() {
        assertEquals(JellyMetadataUtil.COUNTER_KEY, "c");

        for (long testCase : testCases) {
            var m = JellyMetadataUtil.getCounterMetadata(testCase);
            assertEquals(JellyMetadataUtil.COUNTER_KEY, m.getKey());
            assertNotNull(m.getValue());
            // Parsing
            var counter = JellyMetadataUtil.tryGetCounterFromMetadata(List.of(m));
            assertEquals(testCase, counter);
        }
    }

    @Test
    public void testGetCounterNoKey() {
        var counter = JellyMetadataUtil.tryGetCounterFromMetadata(List.of());
        assertEquals(-1, counter);
    }

    @Test
    public void testGetCounterEmptyArray() {
        var m = List.<RdfStreamFrame.MetadataEntry>of(
                RdfStreamFrame.MetadataEntry.newInstance()
                        .setKey(JellyMetadataUtil.COUNTER_KEY)
                        .setValue(ByteString.copyFrom(new byte[0]))
        );

        var counter = JellyMetadataUtil.tryGetCounterFromMetadata(m);
        assertEquals(-1, counter);
    }
}
