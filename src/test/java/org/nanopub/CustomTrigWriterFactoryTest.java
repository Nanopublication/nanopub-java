package org.nanopub;

import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class CustomTrigWriterFactoryTest {

    private final CustomTrigWriterFactory factory = new CustomTrigWriterFactory();

    @Test
    void writesTriG() {
        assertEquals(RDFFormat.TRIG, factory.getRDFFormat());
    }

    @Test
    void getWriterForAnOutputStream() {
        RDFWriter writer = factory.getWriter(new ByteArrayOutputStream());

        assertInstanceOf(CustomTrigWriter.class, writer);
    }

    @Test
    void getWriterForAWriter() {
        RDFWriter writer = factory.getWriter(new StringWriter());

        assertInstanceOf(CustomTrigWriter.class, writer);
    }

}
