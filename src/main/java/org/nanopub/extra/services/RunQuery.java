package org.nanopub.extra.services;

import com.beust.jcommander.ParameterException;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.opencsv.CSVWriterBuilder;
import com.opencsv.ICSVWriter;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.nanopub.CliRunner;

import java.io.PrintWriter;
import java.util.List;

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

    /**
     * Runs the query once and prints what comes back: a table as CSV, as it arrives, and RDF from
     * a CONSTRUCT query as Turtle.
     */
    private void run() throws FailedApiCallException, APINotReachableException, NotEnoughAPIInstancesException {
        Multimap<String, String> paramMap = prepareParamsMap(params);
        QueryRef queryRef = new QueryRef(queryId, paramMap);
        PrintWriter out = new PrintWriter(System.out);
        ICSVWriter csv = new CSVWriterBuilder(out).withSeparator(',').build();
        new QueryAccess() {

            @Override
            protected void processHeader(String[] line) {
                csv.writeNext(line);
            }

            @Override
            protected void processLine(String[] line) {
                csv.writeNext(line);
            }

            @Override
            protected void processRdfContent(Model model) {
                Rio.write(model, System.out, RDFFormat.TURTLE);
            }

        }.call(queryRef);
        csv.flushQuietly();
    }

    Multimap<String, String> prepareParamsMap(List<String> params) {
        Multimap<String, String> paramMap = ArrayListMultimap.create();
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
