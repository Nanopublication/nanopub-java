package org.nanopub.fdo.rest;

import org.nanopub.fdo.rest.gson.ParsedJsonResponse;
import org.nanopub.fdo.rest.gson.Value;

/**
 * Utility class to print the parsed JSON response in a readable format.
 */
public class ResponsePrinter {

    /**
     * Prints the parsed JSON response to the console.
     *
     * @param response The parsed JSON response to print.
     */
    public static void print(ParsedJsonResponse response) {
        System.out.println("id: " + response.handle);
        System.out.println("TYPE   DATA.FORMAT   DATA");
        for (Value value : response.values) {
            System.out.println(value.type + "   " + value.data.format + "   " + value.data.value);
        }
    }

}
