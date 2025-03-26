package org.nanopub;

import com.beust.jcommander.JCommander;

/**
 * Base class for all cli commands.
 */
public abstract class CliRunner {

    /** the initialized JCommander */
    private JCommander jc;

    public JCommander getJc() {
        return jc;
    }

    public void setJc(JCommander jc) {
        this.jc = jc;
    }

}
