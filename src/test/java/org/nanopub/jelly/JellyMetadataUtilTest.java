package org.nanopub.jelly;

import com.google.protobuf.ByteString;
import org.junit.Test;
import scala.Some$;
import scala.Tuple2;
import scala.collection.immutable.Map;
import scala.collection.immutable.Map$;

public class JellyMetadataUtilTest {

    private final long[] testCases = {0, 1, 200, Integer.MAX_VALUE, Long.MAX_VALUE};

    @Test
    public void testCounterMetadataRoundTrip() {
        assert JellyMetadataUtil.COUNTER_KEY == "c";

        for (long testCase : testCases) {
            var m = JellyMetadataUtil.getCounterMetadata(testCase);
            assert m.contains(JellyMetadataUtil.COUNTER_KEY);
            assert m.size() == 1;
            // Parsing
            var counter = JellyMetadataUtil.tryGetCounterFromMetadata(m);
            assert counter == testCase;
        }
    }

    @Test
    public void testGetCounterNoKey() {
        var m = (Object) Map$.MODULE$.empty();
        var counter = JellyMetadataUtil.tryGetCounterFromMetadata((Map<String, ByteString>) m);
        assert counter == -1;
    }

    @Test
    public void testGetCounterEmptyArray() {
        var m = Map.from(Some$.MODULE$.apply(
                Tuple2.apply(JellyMetadataUtil.COUNTER_KEY, ByteString.copyFrom(new byte[0]))
        ));
        var counter = JellyMetadataUtil.tryGetCounterFromMetadata(m);
        assert counter == -1;
    }
}
