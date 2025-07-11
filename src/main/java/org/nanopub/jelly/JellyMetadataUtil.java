package org.nanopub.jelly;

import com.google.protobuf.ByteString;
import com.google.protobuf.CodedInputStream;
import com.google.protobuf.CodedOutputStream;
import eu.neverblink.jelly.core.proto.v1.RdfStreamFrame;

import java.io.IOException;
import java.util.Collection;

/**
 * Utilities for working with metadata of Jelly RdfStreamFrames.
 * This is used for example to serve the counter metadata in the Registry.
 */
public class JellyMetadataUtil {

    /**
     * Key for the counter in the metadata map.
     */
    public static final String COUNTER_KEY = "c";

    /**
     * Create a metadata map with the counter key.
     * The counter is encoded as a Protobuf varint.
     *
     * @param counter The counter value to store
     * @return A map with the counter key and the counter value
     */
    public static RdfStreamFrame.MetadataEntry getCounterMetadata(long counter) {
        int size = CodedOutputStream.computeUInt64SizeNoTag(counter);
        byte[] bytes = new byte[size];
        CodedOutputStream codedOutputStream = CodedOutputStream.newInstance(bytes);
        try {
            codedOutputStream.writeInt64NoTag(counter);
        } catch (IOException e) {
            // Should not happen, really
            throw new RuntimeException(e);
        }

        return RdfStreamFrame.MetadataEntry.newInstance()
                .setKey(COUNTER_KEY)
                .setValue(ByteString.copyFrom(bytes));
    }

    /**
     * Try to get the counter value from the metadata.
     * If the counter is not present or cannot be read, -1 is returned.
     *
     * @param metadata The metadata map to read from
     * @return The counter value, or -1 if it cannot be read
     */
    public static long tryGetCounterFromMetadata(Collection<RdfStreamFrame.MetadataEntry> metadata) {
        var maybeCounterBytes = metadata
                .stream()
                .filter(entry -> COUNTER_KEY.equals(entry.getKey()))
                .map(RdfStreamFrame.MetadataEntry::getValue)
                .findFirst();

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
