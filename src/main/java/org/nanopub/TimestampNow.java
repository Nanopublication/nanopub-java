package org.nanopub;

import com.beust.jcommander.ParameterException;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

import java.util.Date;

/**
 * A simple command-line utility to print the current timestamp.
 */
public class TimestampNow extends CliRunner {

    /**
     * Main method to run the TimestampNow utility.
     *
     * @param args Command-line arguments.
     */
    public static void main(String[] args) {
        TimestampNow obj = CliRunner.initJc(new TimestampNow(), args);
        try {
            obj.run();
        } catch (ParameterException ex) {
            System.exit(1);
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }

    private void run() {
        System.out.println(getTimestamp().stringValue());
    }

    /**
     * Returns the current timestamp as an RDF Literal.
     *
     * @return A Literal representing the current timestamp.
     */
    public static Literal getTimestamp() {
        return SimpleValueFactory.getInstance().createLiteral(new Date());
    }

}
