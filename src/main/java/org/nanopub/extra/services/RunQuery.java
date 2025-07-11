package org.nanopub.extra.services;

import com.beust.jcommander.ParameterException;
import org.nanopub.CliRunner;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RunQuery is a command-line utility to execute a query against the Nanopub service.
 */
public class RunQuery extends CliRunner {

    @com.beust.jcommander.Parameter(description = "query-id", required = true)
    private String queryId;

    @com.beust.jcommander.Parameter(names = "-p", description = "Parameters in the form: key1=val1 ke2=val2 ...")
    private List<String> params;

    /**
     * Main method to run the query service.
     *
     * @param args Command line arguments where the first argument is the query ID
     */
    public static void main(String[] args) {
        try {
            RunQuery obj = CliRunner.initJc(new RunQuery(), args);
            obj.run();
        } catch (ParameterException ex) {
            System.exit(1);
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }

    private void run() throws FailedApiCallException {
        Map<String, String> paramMap = prepareParamsMap(params);

        QueryAccess.printCvsResponse(queryId, paramMap, new PrintWriter(System.out));
    }

    Map<String, String> prepareParamsMap(List<String> params) {
        Map<String, String> paramMap = new HashMap<>();
        if (params != null) {
            for (String p : params) {
                int i = p.indexOf('=');
                String name = p.substring(0, i);
                String value = p.substring(i + 1);
                paramMap.put(name, value);
            }
        }
        return paramMap;
    }

}
