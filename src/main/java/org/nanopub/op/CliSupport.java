package org.nanopub.op;

import com.beust.jcommander.ParameterException;

/**
 * Shared plumbing for the command-line tools in this package.
 * <p>
 * The tools keep their exit-code handling here rather than calling
 * {@link System#exit} from their own {@code main}, so that the outcome of a run
 * can be checked by a caller (including a test) without ending the JVM.
 */
final class CliSupport {

    private CliSupport() {
    }

    /**
     * The body of a command-line tool: everything between parsing the arguments
     * and finishing.
     */
    @FunctionalInterface
    interface CliBody {

        /**
         * Runs the tool.
         *
         * @throws Exception if the run fails for any reason
         */
        void run() throws Exception;
    }

    /**
     * Runs the given body and maps its outcome to a process exit code.
     * <p>
     * A {@link ParameterException} means the arguments were wrong, and
     * JCommander has already printed the usage, so nothing more is reported
     * here. Any other failure gets its stack trace printed before the failing
     * status is returned.
     *
     * @param body the body of the tool
     * @return 0 if the body completed, 1 if it failed
     */
    static int execute(CliBody body) {
        try {
            body.run();
            return 0;
        } catch (ParameterException ex) {
            return 1;
        } catch (Exception ex) {
            ex.printStackTrace();
            return 1;
        }
    }

    /**
     * Ends the process with the given status, unless it is zero, in which case
     * the caller simply returns and the JVM shuts down on its own.
     *
     * @param status the process exit code
     */
    static void exitWith(int status) {
        if (status != 0) {
            System.exit(status);
        }
    }

}
