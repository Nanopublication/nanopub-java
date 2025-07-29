package org.nanopub;

import org.eclipse.rdf4j.common.exception.RDF4JException;
import org.nanopub.extra.index.MakeIndex;
import org.nanopub.extra.security.MakeKeys;
import org.nanopub.extra.security.SignNanopub;
import org.nanopub.extra.server.GetNanopub;
import org.nanopub.extra.server.GetServerInfo;
import org.nanopub.extra.server.NanopubStatus;
import org.nanopub.extra.server.PublishNanopub;
import org.nanopub.extra.services.RunQuery;
import org.nanopub.extra.setting.ShowSetting;
import org.nanopub.fdo.ShaclValidator;
import org.nanopub.trusty.FixTrustyNanopub;
import org.nanopub.trusty.MakeTrustyNanopub;

import java.io.IOException;
import java.util.*;

/**
 * Main class for running various nanopub-related commands.
 */
public class Run {

    private Run() {
    }  // no instances allowed

    /**
     * Main method to run the nanopub commands.
     *
     * @param args the command line arguments, where the first argument is the command to run
     * @throws java.io.IOException                               if an I/O error occurs
     * @throws org.eclipse.rdf4j.common.exception.RDF4JException if an RDF4J error occurs
     */
    public static void main(String[] args) throws IOException, RDF4JException {
        System.setProperty("slf4j.internal.verbosity", "WARN");
        NanopubImpl.ensureLoaded();
        run(args);
    }

    private static List<Class<?>> runnableClasses = new ArrayList<>();
    private static Map<String, Class<?>> runnableClassesByName = new HashMap<>();
    private static Map<String, Class<?>> runnableClassesByShortcut = new HashMap<>();
    private static Map<Class<?>, String> runnableClassNames = new HashMap<>();
    private static Map<Class<?>, String> runnableClassShortcuts = new HashMap<>();

    private static void addRunnableClass(Class<?> c, String shortcut) {
        runnableClasses.add(c);
        runnableClassesByName.put(c.getSimpleName(), c);
        runnableClassNames.put(c, c.getSimpleName());
        if (shortcut != null) {
            runnableClassesByShortcut.put(shortcut, c);
            runnableClassShortcuts.put(c, shortcut);
        }
    }

    static {
        addRunnableClass(CheckNanopub.class, "check");
        addRunnableClass(GetNanopub.class, "get");
        addRunnableClass(PublishNanopub.class, "publish");
        addRunnableClass(SignNanopub.class, "sign");
        addRunnableClass(MakeTrustyNanopub.class, "mktrusty");
        addRunnableClass(FixTrustyNanopub.class, "fix");
        addRunnableClass(NanopubStatus.class, "status");
        addRunnableClass(GetServerInfo.class, "server");
        addRunnableClass(MakeIndex.class, "mkindex");
        addRunnableClass(MakeKeys.class, "mkkeys");
        addRunnableClass(Nanopub2Html.class, "html");
        addRunnableClass(TimestampNow.class, "now");
        addRunnableClass(org.nanopub.op.Run.class, "op");
        addRunnableClass(ShowSetting.class, "setting");
        addRunnableClass(RunQuery.class, "query");
        addRunnableClass(TimestampUpdater.class, "udtime");
        addRunnableClass(StripDown.class, "strip");
        addRunnableClass(ShaclValidator.class, "shacl");
        addRunnableClass(RoCrateImporter.class, "rocrate");
        addRunnableClass(NanopubRetractorCli.class, "retract");
    }

    /**
     * Runs the specified command with the given arguments.
     *
     * @param command the command to run, where the first element is the command name
     * @throws java.io.IOException                               if an I/O error occurs
     * @throws org.eclipse.rdf4j.common.exception.RDF4JException if an RDF4J error occurs
     */
    public static void run(String[] command) throws IOException, RDF4JException {
        if (command.length == 0) {
            System.err.println("ERROR: missing command");
            System.err.println("Use 'help' to show all available commands.");
            System.exit(1);
        }
        String cmd = command[0];
        String[] cmdArgs = Arrays.copyOfRange(command, 1, command.length);
        Class<?> runClass = runnableClassesByName.get(cmd);
        if (runClass == null) {
            runClass = runnableClassesByShortcut.get(cmd);
        }
        if (runClass != null) {
            try {
                runClass.getMethod("main", String[].class).invoke(runClass, (Object) cmdArgs);
            } catch (Exception ex) {
                System.err.println("Internal error: " + ex.getMessage());
                ex.printStackTrace();
                System.exit(1);
            }
        } else if (cmd.equals("help")) {
            System.err.println("Available commands:");
            for (Class<?> c : runnableClasses) {
                String s = runnableClassShortcuts.get(c);
                String n = runnableClassNames.get(c);
                if (s == null) {
                    System.err.println("- " + n);
                } else {
                    System.err.println("- " + s + " / " + n);
                }
            }
            System.exit(0);
        } else {
            System.err.println("ERROR. Unrecognized command: " + cmd);
            System.err.println("Use 'help' to show all available commands.");
            System.exit(1);
        }
    }

}
