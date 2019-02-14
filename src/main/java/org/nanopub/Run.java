package org.nanopub;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.rdf4j.RDF4JException;
import org.nanopub.extra.index.MakeIndex;
import org.nanopub.extra.security.MakeKeys;
import org.nanopub.extra.security.SignNanopub;
import org.nanopub.extra.server.GetNanopub;
import org.nanopub.extra.server.GetServerInfo;
import org.nanopub.extra.server.NanopubStatus;
import org.nanopub.extra.server.PublishNanopub;
import org.nanopub.trusty.FixTrustyNanopub;
import org.nanopub.trusty.MakeTrustyNanopub;

public class Run {

	private Run() {}  // no instances allowed

	public static void main(String[] args) throws IOException, RDF4JException {
		NanopubImpl.ensureLoaded();
		run(args);
	}

	private static List<Class<?>> runnableClasses = new ArrayList<>();
	private static Map<String,Class<?>> runnableClassesByName = new HashMap<>();
	private static Map<String,Class<?>> runnableClassesByShortcut = new HashMap<>();
	private static Map<Class<?>,String> runnableClassNames = new HashMap<>();
	private static Map<Class<?>,String> runnableClassShortcuts = new HashMap<>();

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
	}

	public static void run(String[] command) throws IOException, RDF4JException {
		if (command.length == 0) {
			System.err.println("ERROR: missing command");
			System.err.println("Run 'np help' to show all available commands.");
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
			System.err.println("Run 'np help' to show all available commands.");
			System.exit(1);
		}
	}

}
