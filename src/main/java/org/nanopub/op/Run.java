package org.nanopub.op;

import org.eclipse.rdf4j.common.exception.RDF4JException;
import org.eclipse.rdf4j.rio.RDFParserRegistry;
import org.eclipse.rdf4j.rio.RDFWriterRegistry;
import org.eclipse.rdf4j.rio.turtle.TurtleParserFactory;
import org.eclipse.rdf4j.rio.turtle.TurtleWriterFactory;
import org.nanopub.NanopubImpl;

import java.io.IOException;
import java.util.*;

/**
 * Main class for running various nanopublication operations from the command line.
 * This class serves as a dispatcher to the specific operation classes.
 */
public class Run {

    private Run() {
    }  // no instances allowed

    /**
     * Main method to run the nanopub operations.
     *
     * @param args the command line arguments
     * @throws java.io.IOException                               if an I/O error occurs
     * @throws org.eclipse.rdf4j.common.exception.RDF4JException if an RDF4J error occurs
     */
    public static void main(String[] args) throws IOException, RDF4JException {
        NanopubImpl.ensureLoaded();

        // Not sure why this isn't done automatically...:
        RDFParserRegistry.getInstance().add(new TurtleParserFactory());
        RDFWriterRegistry.getInstance().add(new TurtleWriterFactory());

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
        addRunnableClass(Filter.class, "filter");
        addRunnableClass(Extract.class, "extract");
        addRunnableClass(Gml.class, "gml");
        addRunnableClass(Fingerprint.class, "fingerprint");
        addRunnableClass(Topic.class, "topic");
        addRunnableClass(Reuse.class, "reuse");
        addRunnableClass(Count.class, "count");
        addRunnableClass(Decontextualize.class, "decontext");
        addRunnableClass(Union.class, "union");
        addRunnableClass(IndexReuse.class, "ireuse");
        addRunnableClass(ExportJson.class, "exportjson");
        addRunnableClass(Namespaces.class, "namespaces");
        addRunnableClass(Aggregate.class, "aggregate");
        addRunnableClass(Import.class, "import");
        addRunnableClass(Create.class, "create");
        addRunnableClass(Build.class, "build");
        addRunnableClass(Tar.class, "tar");
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
            System.err.println("Run with 'help' argument to show all available commands.");
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
                ex.printStackTrace(System.err);
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
            System.err.println("Run 'npop help' to show all available commands.");
            System.exit(1);
        }
    }

}
