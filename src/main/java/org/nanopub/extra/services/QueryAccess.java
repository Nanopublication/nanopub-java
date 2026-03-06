package org.nanopub.extra.services;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriterBuilder;
import com.opencsv.ICSVWriter;
import com.opencsv.exceptions.CsvValidationException;
import org.apache.http.HttpResponse;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;

/**
 * Second-generation query API access
 */
public abstract class QueryAccess {

    /**
     * Process the header.
     *
     * @param line the header line from the CSV response
     */
    protected abstract void processHeader(String[] line);

    /**
     * Process a line of data from the CSV response.
     *
     * @param line the line of data from the CSV response
     */
    protected abstract void processLine(String[] line);

    /**
     * Process the RDF content from a SPARQL CONSTRUCT query response.
     * Override this method to handle RDF responses. Default implementation is a no-op.
     *
     * @param model the parsed RDF model
     */
    protected void processRdfContent(Model model) {
    }

    /**
     * Call a query with the given queryId and parameters.
     *
     * @param queryRef the query reference
     * @throws FailedApiCallException         if the API call fails
     * @throws APINotReachableException       if the API is not reachable
     * @throws NotEnoughAPIInstancesException if there are not enough API instances available
     */
    public void call(QueryRef queryRef) throws FailedApiCallException, APINotReachableException, NotEnoughAPIInstancesException {
        HttpResponse resp = QueryCall.run(queryRef);
        String contentType = resp.getFirstHeader("Content-Type") != null
                ? resp.getFirstHeader("Content-Type").getValue() : "";
        String mimeType = contentType.contains(";") ? contentType.split(";")[0].trim() : contentType.trim();
        if (mimeType.equals("text/csv") || mimeType.isEmpty()) {
            try (CSVReader csvReader = new CSVReader(new BufferedReader(new InputStreamReader(resp.getEntity().getContent())))) {
                String[] line = null;
                int n = 0;
                while ((line = csvReader.readNext()) != null) {
                    n++;
                    if (n == 1) {
                        processHeader(line);
                    } else {
                        processLine(line);
                    }
                }
            } catch (IOException | CsvValidationException ex) {
                throw new FailedApiCallException(ex);
            }
        } else {
            RDFFormat format = Rio.getParserFormatForMIMEType(mimeType).orElse(RDFFormat.TURTLE);
            try {
                Model model = Rio.parse(resp.getEntity().getContent(), format);
                processRdfContent(model);
            } catch (IOException ex) {
                throw new FailedApiCallException(ex);
            }
        }
    }

    /**
     * Print the response of a query in CSV format to the given writer.
     *
     * @param queryRef the query reference
     * @param writer   the writer to print the CSV response to
     * @throws FailedApiCallException         if the API call fails
     * @throws APINotReachableException       if the API is not reachable
     * @throws NotEnoughAPIInstancesException if there are not enough API instances available
     */
    public static void printCvsResponse(QueryRef queryRef, Writer writer) throws FailedApiCallException, APINotReachableException, NotEnoughAPIInstancesException {
        ICSVWriter icsvWriter = new CSVWriterBuilder(writer).withSeparator(',').build();
        QueryAccess a = new QueryAccess() {

            @Override
            protected void processHeader(String[] line) {
                icsvWriter.writeNext(line);
            }

            @Override
            protected void processLine(String[] line) {
                icsvWriter.writeNext(line);
            }

        };
        a.call(queryRef);
        icsvWriter.flushQuietly();
    }

    /**
     * Get the response of a query as an ApiResponse object.
     *
     * @param queryRef the query reference
     * @return an ApiResponse object containing the response data
     * @throws FailedApiCallException         if the API call fails
     * @throws APINotReachableException       if the API is not reachable
     * @throws NotEnoughAPIInstancesException if there are not enough API instances available
     */
    public static ApiResponse get(QueryRef queryRef) throws FailedApiCallException, APINotReachableException, NotEnoughAPIInstancesException {
        final ApiResponse response = new ApiResponse();
        QueryAccess a = new QueryAccess() {

            @Override
            protected void processHeader(String[] line) {
                response.setHeader(line);
            }

            @Override
            protected void processLine(String[] line) {
                response.add(line);
            }

            @Override
            protected void processRdfContent(Model model) {
                response.setRdfContent(model);
            }

        };
        a.call(queryRef);
        return response;
    }

}
