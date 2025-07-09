package org.nanopub;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

/**
 * Abstract class for command-line interface (CLI) runners.
 */
public abstract class CliRunner {

    /**
     * The JCommander instance that handles command-line arguments.
     */
    private JCommander jc;

    /**
     * Initializes the JCommander instance with the given object and command-line arguments.
     *
     * @param obj  the object to be initialized
     * @param args the command-line arguments
     * @param <T>  the type of the object
     * @return the initialized object
     */
    public static <T extends CliRunner> T initJc(T obj, String[] args) {
        JCommander jc = new JCommander(obj);
        ((CliRunner) obj).setJc(jc);
        try {
            jc.parse(args);
        } catch (ParameterException ex) {
            jc.usage();
            throw ex;
        }
        return obj;
    }

    /**
     * Returns the initialized JCommander instance.
     *
     * @return the JCommander instance
     */
    public JCommander getJc() {
        return jc;
    }

    /**
     * Sets the JCommander instance.
     *
     * @param jc the JCommander instance to set
     */
    private void setJc(JCommander jc) {
        this.jc = jc;
    }

}
