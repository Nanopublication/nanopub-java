package org.nanopub.fdo.rest;

import org.nanopub.fdo.rest.gson.ParsedJsonResponse;
import org.nanopub.fdo.rest.gson.Value;

public class ResponsePrinter {

    public static void print(ParsedJsonResponse response) {
        System.out.println("id: " + response.handle);
        System.out.println("TYPE   DATA.FORMAT   DATA");
        for (Value value : response.values) {
            System.out.println(value.type + "   " + value.data.format + "   " + value.data.value);
        }
    }
}
