package org.nanopub.jelly;

import eu.ostrzyciel.jelly.convert.rdf4j.Rdf4jConverterFactory$;
import eu.ostrzyciel.jelly.core.ProtoEncoder;
import eu.ostrzyciel.jelly.core.proto.v1.RdfStreamFrame;
import eu.ostrzyciel.jelly.core.proto.v1.RdfStreamFrame$;
import eu.ostrzyciel.jelly.core.proto.v1.RdfStreamOptions;
import eu.ostrzyciel.jelly.core.proto.v1.RdfStreamRow;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.rio.helpers.AbstractRDFHandler;
import scala.Some$;
import scala.collection.mutable.ListBuffer;

/**
 * RDF4J Rio RDFHandler that converts nanopubs into Jelly RdfStreamFrames.
 */
public class JellyWriterRDFHandler extends AbstractRDFHandler {
    private final ProtoEncoder<Value, Statement, Statement, ?> encoder;
    private final ListBuffer<RdfStreamRow> rowBuffer = new ListBuffer<>();

    JellyWriterRDFHandler(RdfStreamOptions options) {
        // Enabling namespace declarations -- so we are using Jelly 1.1.0 here.
        this.encoder = Rdf4jConverterFactory$.MODULE$.encoder(ProtoEncoder.Params.apply(
            options, true, Some$.MODULE$.apply(rowBuffer)
        ));
    }

    @Override
    public void handleStatement(Statement st) {
        encoder.addQuadStatement(st);
    }

    @Override
    public void handleNamespace(String prefix, String uri) {
        encoder.declareNamespace(prefix, uri);
    }

    /**
     * Call this at the end of a nanopub.
     * @return RdfStreamFrame
     */
    public RdfStreamFrame getFrame() {
        return getFrame(-1);
    }

    /**
     * Call this at the end of a nanopub.
     * This flushes the buffer and returns the RdfStreamFrame corresponding to one nanopub.
     * @param counter The counter value to store in the frame metadata. If < 0, no metadata is added.
     * @return RdfStreamFrame
     */
    public RdfStreamFrame getFrame(long counter) {
        var rows = rowBuffer.toList();
        rowBuffer.clear();
        var metadata = JellyMetadataUtil.EMPTY_METADATA;
        if (counter >= 0) {
            metadata = JellyMetadataUtil.getCounterMetadata(counter);
        }
        return RdfStreamFrame$.MODULE$.apply(rows, metadata);
    }
}
