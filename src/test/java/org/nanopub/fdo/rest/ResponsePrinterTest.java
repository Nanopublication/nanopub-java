package org.nanopub.fdo.rest;

import org.junit.jupiter.api.Test;
import org.nanopub.fdo.rest.gson.Data;
import org.nanopub.fdo.rest.gson.ParsedJsonResponse;
import org.nanopub.fdo.rest.gson.Value;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResponsePrinterTest {

    @Test
    void printWithValidResponsePrintsExpectedOutput() {
        ParsedJsonResponse response = new ParsedJsonResponse();
        response.handle = "12345";
        Data data1 = new Data();
        data1.format = "JSON";
        data1.value = "{\"key\":\"value\"}";
        Value value1 = new Value();
        value1.type = String.class.getSimpleName();
        value1.data = data1;
        response.values = new Value[]{
                value1,
        };

        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outContent));

        try {
            ResponsePrinter.print(response);
        } finally {
            System.setOut(originalOut);
        }

        String expected = """
                id: 12345
                TYPE   DATA.FORMAT   DATA
                String   JSON   {"key":"value"}
                """;
        assertEquals(expected, outContent.toString());
    }

}