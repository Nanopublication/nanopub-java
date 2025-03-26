package org.nanopub;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

/**
 * Base class for all cli commands.
 */
public abstract class CliRunner {

    /** the initialized JCommander */
    private JCommander jc;

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

    public JCommander getJc() {
        return jc;
    }

    private void setJc(JCommander jc) {
        this.jc = jc;
    }

}
