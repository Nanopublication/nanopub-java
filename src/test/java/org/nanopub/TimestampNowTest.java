package org.nanopub;

import org.eclipse.rdf4j.model.Literal;
import org.junit.jupiter.api.Test;

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

}