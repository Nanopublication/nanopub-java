package org.nanopub.jelly;

import eu.ostrzyciel.jelly.convert.rdf4j.Rdf4jConverterFactory$;
import eu.ostrzyciel.jelly.convert.rdf4j.rio.package$;
import eu.ostrzyciel.jelly.core.JellyOptions$;
import eu.ostrzyciel.jelly.core.ProtoDecoder;
import eu.ostrzyciel.jelly.core.proto.v1.*;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.nanopub.MalformedNanopubException;
import org.nanopub.Nanopub;
import org.nanopub.NanopubImpl;
import org.nanopub.NanopubUtils;
import scala.Option;
import scala.Some;
import scala.jdk.CollectionConverters;
import scala.runtime.BoxedUnit;

import java.io.InputStream;
import java.util.Vector;
import java.util.stream.Stream;

/**
 * Utility functions for working with Jelly RDF data.
 */
public class JellyUtils {

    /**
     * Jelly RDF format for use with RDF4J Rio.
     */
    public final static RDFFormat JELLY_FORMAT = package$.MODULE$.JELLY();

    public final static Option<RdfStreamOptions> defaultSupportedOptions =
        Some.apply(JellyOptions$.MODULE$.defaultSupportedOptions());

    /**
     * Options for Jelly RDF streams that are written to the database.
     */
    public static RdfStreamOptions jellyOptionsForDB = JellyOptions$.MODULE$.smallStrict()
        .withPhysicalType(PhysicalStreamType.QUADS$.MODULE$)
        .withLogicalType(LogicalStreamType.DATASETS$.MODULE$);

    /**
     * Options for Jelly RDF streams that are transmitted between services.
     */
    public static RdfStreamOptions jellyOptionsForTransmission = JellyOptions$.MODULE$.bigStrict()
        .withPhysicalType(PhysicalStreamType.QUADS$.MODULE$)
        .withLogicalType(LogicalStreamType.DATASETS$.MODULE$);

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
        RdfStreamFrame frame = RdfStreamFrame$.MODULE$.parseFrom(jellyBytes);
        return readFromFrame(frame);
    }

    /**
     * Read one Nanopub from an input byte stream in the Jelly format. This can be used on HTTP responses.
     *
     * @param is Jelly RDF data (delimited, one frame (!!!))
     * @return Nanopub
     * @throws MalformedNanopubException if this is not a valid Nanopub
     */
    public static Nanopub readFromInputStream(InputStream is) throws MalformedNanopubException {
        RdfStreamFrame frame = RdfStreamFrame$.MODULE$.parseDelimitedFrom(is).get();
        return readFromFrame(frame);
    }

    static Nanopub readFromFrame(RdfStreamFrame frame) throws MalformedNanopubException {
        final Vector<Statement> statements = new Vector<>();
        final Vector<Pair<String, String>> namespaces = new Vector<>();
        final ProtoDecoder<Statement> decoder = getDecoder(namespaces);

        parseStatements(frame, decoder, statements);
        return new NanopubImpl(statements, namespaces);
    }

    static Stream<MaybeNanopub> readFromFrameStream(Stream<RdfStreamFrame> frameStream) {
        final Vector<Statement> statements = new Vector<>();
        final Vector<Pair<String, String>> namespaces = new Vector<>();
        final ProtoDecoder<Statement> decoder = getDecoder(namespaces);

        return frameStream.map(frame -> {
            try {
                statements.clear();
                namespaces.clear();
                parseStatements(frame, decoder, statements);
                return new MaybeNanopub(
                        new NanopubImpl(statements, namespaces),
                        // Extract the counter metadata from the frame
                        JellyMetadataUtil.tryGetCounterFromMetadata(frame.metadata())
                );
            } catch (MalformedNanopubException e) {
                return new MaybeNanopub(e);
            }
        });
    }

    private static ProtoDecoder<Statement> getDecoder(Vector<Pair<String, String>> namespaces) {
        return Rdf4jConverterFactory$.MODULE$.quadsDecoder(
                defaultSupportedOptions,
                ((String prefix, Value node) -> {
                    namespaces.add(Pair.of(prefix, node.stringValue()));
                    return BoxedUnit.UNIT;
                })
        );
    }

    private static void parseStatements(
            RdfStreamFrame frame, ProtoDecoder<Statement> decoder, Vector<Statement> statements
    ) {
        CollectionConverters.SeqHasAsJava(frame.rows()).asJava().forEach(row -> {
            Statement maybeSt = (Statement) decoder.ingestRowFlat(row);
            if (maybeSt != null) {
                statements.add(maybeSt);
            }
        });
    }
}
