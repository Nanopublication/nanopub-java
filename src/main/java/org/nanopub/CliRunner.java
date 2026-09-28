package org.nanopub;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/**
 * Abstract class for command-line interface (CLI) runners.
 */
public abstract class CliRunner {

    private static final Logger logger = LoggerFactory.getLogger(CliRunner.class);

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
            String arguments = "args = " + Arrays.toString(args);
            logOrSysout(logger, arguments);
            jc.usage();
            throw ex;
        }
        return obj;
    }

    /**
     * Depending on the logger-LEVEL of log (if &gt;= DEBUG), we forward the logMsg to SysOut or DEBUG logger.
     * <p>
     * This is intended for CLI progress output only. Library code must not use it, since writing to
     * standard output cannot be suppressed or redirected by an embedding application; log through a
     * {@link Logger} directly instead.
     *
     * @param log    the Logger instance
     * @param logMsg the message to log
     */
    protected static void logOrSysout(Logger log, String logMsg) {
        if (log.isDebugEnabled()) {
            log.debug(logMsg);
        } else {
            System.out.println(logMsg);
        }
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
