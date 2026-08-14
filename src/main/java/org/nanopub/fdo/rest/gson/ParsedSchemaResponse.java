package org.nanopub.fdo.rest.gson;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a parsed schema response.
 */
public class ParsedSchemaResponse {
    /**
     * The schema required properties.
     */
    public String[] required = new String[0];

    /**
     * The schema properties.
     */
    public Map<String, Object> properties = new HashMap<>();
}
