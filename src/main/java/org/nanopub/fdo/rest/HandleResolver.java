package org.nanopub.fdo.rest;

import com.google.gson.Gson;
import org.nanopub.fdo.rest.gson.ParsedJsonResponse;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * A simple class to resolve handles.
 */
public class HandleResolver {

    /**
     * The base URI for the Handle.
     */
    public final static String BASE_URI = "https://hdl.handle.net/api/handles/";

    private HttpClient client = HttpClient.newHttpClient();

    /**
     * Calls the handle resolver for a given handle ID.
     *
     * @param id the handle ID to resolve
     * @return ParsedJsonResponse containing the resolved handle information
     * @throws URISyntaxException   if the URI is malformed
     * @throws IOException          if an I/O error occurs
     * @throws InterruptedException if the operation is interrupted
     */
    public ParsedJsonResponse call(String id) throws URISyntaxException, IOException, InterruptedException {

        HttpRequest req = HttpRequest.newBuilder().GET().uri(new URI(BASE_URI + id)).build();
        HttpResponse<String> httpResponse = client.send(req, HttpResponse.BodyHandlers.ofString());

        ParsedJsonResponse r = new Gson().fromJson(httpResponse.body(), ParsedJsonResponse.class);
        return r;
    }

}
