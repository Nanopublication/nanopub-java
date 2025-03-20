package org.nanopub.jelly;

import com.google.protobuf.ByteString;
import com.google.protobuf.CodedInputStream;
import com.google.protobuf.CodedOutputStream;
import scala.Some$;
import scala.Tuple2;
import scala.collection.immutable.Map$;

import java.io.IOException;

/**
 * Utilities for working with metadata of Jelly RdfStreamFrames.
 * This is used for example to serve the counter metadata in the Registry.
 */
public class JellyMetadataUtil {
    public static final String COUNTER_KEY = "c";
    public static final scala.collection.immutable.Map<String, ByteString> EMPTY_METADATA = Map$.MODULE$.empty();

    /**
     * Create a metadata map with the counter key.
     * The counter is encoded as a Protobuf varint.
     * @param counter The counter value to store
     * @return A map with the counter key and the counter value
     */
    public static scala.collection.immutable.Map<String, ByteString> getCounterMetadata(long counter) {
        int size = CodedOutputStream.computeUInt64SizeNoTag(counter);
        byte[] bytes = new byte[size];
        CodedOutputStream codedOutputStream = CodedOutputStream.newInstance(bytes);
        try {
            codedOutputStream.writeInt64NoTag(counter);
        } catch (IOException e) {
            // Should not happen, really
            throw new RuntimeException(e);
        }
        return scala.collection.immutable.Map.from(Some$.MODULE$.apply(
            Tuple2.apply(COUNTER_KEY, ByteString.copyFrom(bytes))
        ));
    }

    /**
     * Try to get the counter value from the metadata.
     * If the counter is not present or cannot be read, -1 is returned.
     * @param metadata The metadata map to read from
     * @return The counter value, or -1 if it cannot be read
     */
    public static long tryGetCounterFromMetadata(scala.collection.immutable.Map<String, ByteString> metadata) {
        var maybeCounterBytes = metadata.get(COUNTER_KEY);
        if (maybeCounterBytes.isEmpty()) {
            return -1;
        }
        var is = CodedInputStream.newInstance(maybeCounterBytes.get().asReadOnlyByteBuffer());
        try {
            return is.readInt64();
        } catch (IOException e) {
            return -1;
        }
    }
}
