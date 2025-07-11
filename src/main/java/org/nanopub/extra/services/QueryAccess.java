package org.nanopub.extra.services;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriterBuilder;
import com.opencsv.ICSVWriter;
import com.opencsv.exceptions.CsvValidationException;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.HttpResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
     * Call a query with the given queryId and parameters.
     *
     * @param queryId the ID of the query to call
     * @param params  the parameters to pass to the query
     * @throws FailedApiCallException if the API call fails
     */
    public void call(String queryId, Map<String, String> params) throws FailedApiCallException {
        HttpResponse resp = QueryCall.run(queryId, params);
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
    }

    /**
     * Print the response of a query in CSV format to the given writer.
     *
     * @param queryId the ID of the query to call
     * @param params  the parameters to pass to the query
     * @param writer  the writer to print the CSV response to
     * @throws FailedApiCallException if the API call fails
     */
    public static void printCvsResponse(String queryId, Map<String, String> params, Writer writer) throws FailedApiCallException {
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
        a.call(queryId, params);
        icsvWriter.flushQuietly();
    }

    /**
     * Get the response of a query as an ApiResponse object.
     *
     * @param queryId the ID of the query to call
     * @param params  the parameters to pass to the query
     * @return an ApiResponse object containing the response data
     * @throws FailedApiCallException if the API call fails
     */
    public static ApiResponse get(String queryId, Map<String, String> params) throws FailedApiCallException {
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

        };
        a.call(queryId, params);
        return response;
    }

    private static Map<String, Pair<Long, String>> latestVersionMap = new HashMap<>();
    // TODO Make a better query for this, where superseded and rejected are excluded from the start:
    private static final String GET_NEWER_VERSIONS = "RA3qSfVzcnAeMOODdpgCg4e-bX6KjZYZ2JQXDsSwluMaI/get-newer-versions-of-np";

    /**
     * Get the latest version ID of a nanopublication.
     * If the latest version is not cached or is older than 1 hour, it will re-fetch it.
     *
     * @param nanopubId the ID of the nanopublication
     * @return the latest version ID of the nanopublication
     */
    // TODO Is this method used anywhere, Nanodash has a copy of this.
    public static String getLatestVersionId(String nanopubId) {
        long currentTime = System.currentTimeMillis();
        if (!latestVersionMap.containsKey(nanopubId) || currentTime - latestVersionMap.get(nanopubId).getLeft() > 1000 * 60 * 60) {
            // Re-fetch if existing value is older than 1 hour
            Map<String, String> params = new HashMap<>();
            params.put("np", nanopubId);
            try {
                ApiResponse r = QueryAccess.get(GET_NEWER_VERSIONS, params);
                List<String> latestList = new ArrayList<>();
                for (ApiResponseEntry e : r.getData()) {
                    if (e.get("retractedBy").isEmpty() && e.get("supersededBy").isEmpty()) {
                        latestList.add(e.get("newerVersion"));
                    }
                }
                if (latestList.size() == 1) {
                    String latest = latestList.get(0);
                    latestVersionMap.put(nanopubId, Pair.of(currentTime, latest));
                    return latest;
                } else {
                    return nanopubId;
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                return nanopubId;
            }
        }
        return latestVersionMap.get(nanopubId).getRight();
    }

}