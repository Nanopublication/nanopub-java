package org.nanopub;

import org.junit.Test;

import java.net.UnknownHostException;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * I get paid by test coverage ;-)
 * <p>
 * Calling PopularPrefixes.getNamespace() just once, adds around 2500 lines of coverage.
 * One may argue the better approach would be, to read the list of popular prefixes
 * form a file instead of generate source code. But for now we are happy enough with this
 * solution.
 */
public class PopularPrefixesTest {

    @Test
    public void checkDnsLookUp() throws UnknownHostException {
        String schema = PopularPrefixes.getNamespace("schema");
        assertNotNull(schema);
    }

}