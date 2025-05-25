package org.nanopub.jelly;

import com.google.protobuf.InvalidProtocolBufferException;
import eu.neverblink.jelly.convert.rdf4j.Rdf4jConverterFactory;
import eu.neverblink.jelly.convert.rdf4j.Rdf4jDatatype;
import eu.neverblink.jelly.core.JellyOptions;
import eu.neverblink.jelly.core.ProtoDecoder;
import eu.neverblink.jelly.core.RdfHandler;
import eu.neverblink.jelly.core.proto.v1.*;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.nanopub.MalformedNanopubException;
import org.nanopub.Nanopub;
import org.nanopub.NanopubImpl;
import org.nanopub.NanopubUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.stream.Stream;

/**
 * Utility functions for working with Jelly RDF data.
 */
public class JellyUtils {

    /**
     * Options for Jelly RDF streams that are written to the database.
     */
    public static final RdfStreamOptions jellyOptionsForDB = JellyOptions.SMALL_STRICT
        .clone()
        .setPhysicalType(PhysicalStreamType.QUADS)
        .setLogicalType(LogicalStreamType.DATASETS);

    /**
     * Options for Jelly RDF streams that are transmitted between services.
     */
    public static final RdfStreamOptions jellyOptionsForTransmission = JellyOptions.BIG_STRICT
        .clone()
        .setPhysicalType(PhysicalStreamType.QUADS)
        .setLogicalType(LogicalStreamType.DATASETS);

    /**
     * Write a Nanopub to bytes in the Jelly format to be stored in the database.
     * @param np Nanopub
     * @return Jelly RDF bytes (non-delimited)
     */
    public static byte[] writeNanopubForDB(Nanopub np) {
        JellyWriterRDFHandler handler = new JellyWriterRDFHandler(jellyOptionsForDB);
        NanopubUtils.propagateToHandler(np, handler);
        RdfStreamFrame frame = handler.getFrame();
        return frame.toByteArray();
    }

    /**
     * Read a Nanopub from bytes in the Jelly format stored in the database.
     * <p>
     * This specialized implementation should be a bit faster than going through RDF4J Rio,
     * because we are dealing with a special (simpler) case here.
     *
     * @param jellyBytes Jelly RDF bytes (non-delimited)
     * @return Nanopub
     * @throws MalformedNanopubException if this is not a valid Nanopub
     */
    public static Nanopub readFromDB(byte[] jellyBytes) throws MalformedNanopubException {
        try {
            RdfStreamFrame frame = RdfStreamFrame.parseFrom(jellyBytes);
            return readFromFrame(frame);
        } catch (InvalidProtocolBufferException e) {
            throw new MalformedNanopubException("Failed to parse Jelly RDF bytes as a Nanopub: " + e.getMessage());
        }
    }

    /**
     * Read one Nanopub from an input byte stream in the Jelly format. This can be used on HTTP responses.
     *
     * @param is Jelly RDF data (delimited, one frame (!!!))
     * @return Nanopub
     * @throws MalformedNanopubException if this is not a valid Nanopub
     */
    public static Nanopub readFromInputStream(InputStream is) throws MalformedNanopubException {
        try {
            RdfStreamFrame frame = RdfStreamFrame.parseDelimitedFrom(is);
            return readFromFrame(frame);
        } catch (IOException e) {
            throw new MalformedNanopubException("Failed to read Jelly RDF from InputStream: " + e.getMessage());
        }
    }

    static Nanopub readFromFrame(RdfStreamFrame frame) throws MalformedNanopubException {
        final ArrayList<Statement> statements = new ArrayList<>();
        final ArrayList<Pair<String, String>> namespaces = new ArrayList<>();

        final var decoder = getDecoder(statements, namespaces);
        frame.getRows().forEach(decoder::ingestRow);

        return new NanopubImpl(statements, namespaces);
    }

    static Stream<MaybeNanopub> readFromFrameStream(Stream<RdfStreamFrame> frameStream) {
        final ArrayList<Statement> statements = new ArrayList<>();
        final ArrayList<Pair<String, String>> namespaces = new ArrayList<>();
        final var decoder = getDecoder(statements, namespaces);

        return frameStream.map(frame -> {
            try {
                statements.clear();
                namespaces.clear();

                frame.getRows().forEach(decoder::ingestRow);
                return new MaybeNanopub(
                        new NanopubImpl(statements, namespaces),
                        // Extract the counter metadata from the frame
                        JellyMetadataUtil.tryGetCounterFromMetadata(frame.getMetadata())
                );
            } catch (MalformedNanopubException e) {
                return new MaybeNanopub(e);
            }
        });
    }

    private static ProtoDecoder<Value, Rdf4jDatatype> getDecoder(
        ArrayList<Statement> statements,
        ArrayList<Pair<String, String>> namespaces
    ) {
        final var quadMaker = Rdf4jConverterFactory.getInstance().decoderConverter();
        final var handler = new RdfHandler.QuadHandler<Value>() {
            @Override
            public void handleNamespace(String prefix, Value namespace) {
                namespaces.add(Pair.of(prefix, namespace.stringValue()));
            }

            @Override
            public void handleQuad(Value subject, Value predicate, Value object, Value graph) {
                statements.add(quadMaker.makeQuad(subject, predicate, object, graph));
            }
        };

        return Rdf4jConverterFactory.getInstance().quadsDecoder(handler, JellyOptions.DEFAULT_SUPPORTED_OPTIONS);
    }
}
