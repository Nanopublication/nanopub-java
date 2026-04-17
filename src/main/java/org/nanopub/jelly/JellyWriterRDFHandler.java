package org.nanopub.jelly;

import eu.neverblink.jelly.convert.rdf4j.Rdf4jConverterFactory;
import eu.neverblink.jelly.core.ProtoDecoderConverter;
import eu.neverblink.jelly.core.ProtoEncoder;
import eu.neverblink.jelly.core.memory.RowBuffer;
import eu.neverblink.jelly.core.proto.v1.RdfStreamFrame;
import eu.neverblink.jelly.core.proto.v1.RdfStreamOptions;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.rio.helpers.AbstractRDFHandler;

/**
 * RDF4J Rio RDFHandler that converts nanopubs into Jelly RdfStreamFrames.
 */
public class JellyWriterRDFHandler extends AbstractRDFHandler {
    private final ProtoDecoderConverter<Value, ?> decoderConverter;
    private final ProtoEncoder<Value> encoder;
    private final RowBuffer rowBuffer = RowBuffer.newLazyImmutable();

    JellyWriterRDFHandler(RdfStreamOptions options) {
        this.decoderConverter = Rdf4jConverterFactory.getInstance().decoderConverter();

        // Enabling namespace declarations -- so we are using Jelly 1.1.0 here.
        this.encoder = Rdf4jConverterFactory.getInstance().encoder(ProtoEncoder.Params.of(options, true, rowBuffer));
    }

    /**
     * {@inheritDoc}
     * <p>
     * Handle a statement by encoding it into the Jelly format.
     */
    @Override
    public void handleStatement(Statement st) {
        encoder.handleQuad(st.getSubject(), st.getPredicate(), st.getObject(), st.getContext());
    }

    /**
     * {@inheritDoc}
     * <p>
     * Handle a namespace declaration by encoding it into the Jelly format.
     */
    @Override
    public void handleNamespace(String prefix, String uri) {
        encoder.handleNamespace(prefix, decoderConverter.makeIriNode(uri));
    }

    /**
     * Call this at the end of a nanopub.
     *
     * @return RdfStreamFrame
     */
    public RdfStreamFrame getFrame() {
        return getFrame(-1);
    }

    /**
     * Call this at the end of a nanopub.
     * This flushes the buffer and returns the RdfStreamFrame corresponding to one nanopub.
     *
     * @param counter The counter value to store in the frame metadata. If {@code counter < 0}, no metadata is added.
     * @return RdfStreamFrame with the nanopub data.
     */
    public RdfStreamFrame getFrame(long counter) {
        var rows = rowBuffer.getRows();
        rowBuffer.clear();

        final var frame = RdfStreamFrame.newInstance();
        for (final var row : rows) {
            frame.addRows(row);
        }

        if (counter >= 0) {
            frame.addMetadata(JellyMetadataUtil.getCounterMetadata(counter));
        }

        return frame;
    }

}
