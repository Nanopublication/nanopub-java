package org.nanopub;

import jakarta.xml.bind.DatatypeConverter;
import org.eclipse.rdf4j.model.Literal;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TimestampNowTest {

    @Test
    void getTimestampReturnsNonNullLiteral() {
        Literal timestamp = TimestampNow.getTimestamp();
        assertNotNull(timestamp);
    }

    @Test
    void getTimestampReturnsValidDateLiteral() {
        Literal timestamp = TimestampNow.getTimestamp();
        assertDoesNotThrow(() -> new Date(timestamp.calendarValue().toGregorianCalendar().getTimeInMillis()));
    }

    @Test
    void mainPrintsTheCurrentTimestamp() {
        PrintStream originalOut = System.out;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        try {
            System.setOut(new PrintStream(captured, true, StandardCharsets.UTF_8));
            TimestampNow.main(new String[0]);
        } finally {
            System.setOut(originalOut);
        }

        String printed = captured.toString(StandardCharsets.UTF_8).trim();
        assertDoesNotThrow(() -> DatatypeConverter.parseDateTime(printed));
    }

}