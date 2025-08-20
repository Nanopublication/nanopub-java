package org.nanopub.jelly;

import com.google.protobuf.ByteString;
import eu.neverblink.jelly.core.proto.v1.RdfStreamFrame;
import org.junit.jupiter.api.Test;

import java.util.List;

public class JellyMetadataUtilTest {

    private final long[] testCases = {0, 1, 200, Integer.MAX_VALUE, Long.MAX_VALUE};

    @Test
    public void testCounterMetadataRoundTrip() {
        assert JellyMetadataUtil.COUNTER_KEY == "c";

        for (long testCase : testCases) {
            var m = JellyMetadataUtil.getCounterMetadata(testCase);
            assert m.getKey().equals(JellyMetadataUtil.COUNTER_KEY);
            assert m.getValue() != null;
            // Parsing
            var counter = JellyMetadataUtil.tryGetCounterFromMetadata(List.of(m));
            assert counter == testCase;
        }
    }

    @Test
    public void testGetCounterNoKey() {
        var counter = JellyMetadataUtil.tryGetCounterFromMetadata(List.of());
        assert counter == -1;
    }

    @Test
    public void testGetCounterEmptyArray() {
        var m = List.<RdfStreamFrame.MetadataEntry>of(
                RdfStreamFrame.MetadataEntry.newInstance()
                        .setKey(JellyMetadataUtil.COUNTER_KEY)
                        .setValue(ByteString.copyFrom(new byte[0]))
        );

        var counter = JellyMetadataUtil.tryGetCounterFromMetadata(m);
        assert counter == -1;
    }
}
