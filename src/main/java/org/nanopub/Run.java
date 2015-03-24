package org.nanopub;

import java.io.IOException;
import java.util.Arrays;

import org.nanopub.extra.index.MakeIndex;
import org.nanopub.extra.security.MakeKeys;
import org.nanopub.extra.security.SignNanopub;
import org.nanopub.extra.server.GetNanopub;
import org.nanopub.extra.server.GetServerInfo;
import org.nanopub.extra.server.NanopubStatus;
import org.nanopub.extra.server.PublishNanopub;
import org.nanopub.trusty.FixTrustyNanopub;
import org.nanopub.trusty.MakeTrustyNanopub;
import org.openrdf.OpenRDFException;

public class Run {

	private Run() {}  // no instances allowed

	public static void main(String[] args) throws IOException, OpenRDFException {
		run(args);
	}

	public static void run(String[] command) throws IOException, OpenRDFException {
		if (command.length == 0) {
			System.err.println("ERROR: missing command");
			System.exit(1);
		}
		String cmd = command[0];
		String[] cmdArgs = Arrays.copyOfRange(command, 1, command.length);
		if (cmd.equals("CheckNanopub") || cmd.equals("check")) {
			CheckNanopub.main(cmdArgs);
		} else if (cmd.equals("GetNanopub") || cmd.equals("get")) {
			GetNanopub.main(cmdArgs);
		} else if (cmd.equals("PublishNanopub") || cmd.equals("publish")) {
			PublishNanopub.main(cmdArgs);
		} else if (cmd.equals("SignNanopub") || cmd.equals("sign")) {
			SignNanopub.main(cmdArgs);
		} else if (cmd.equals("MakeTrustyNanopub") || cmd.equals("mktrusty")) {
			MakeTrustyNanopub.main(cmdArgs);
		} else if (cmd.equals("FixTrustyNanopub") || cmd.equals("fix")) {
			FixTrustyNanopub.main(cmdArgs);
		} else if (cmd.equals("NanopubStatus") || cmd.equals("status")) {
			NanopubStatus.main(cmdArgs);
		} else if (cmd.equals("GetServerInfo") || cmd.equals("server")) {
			GetServerInfo.main(cmdArgs);
		} else if (cmd.equals("MakeIndex") || cmd.equals("mkindex")) {
			MakeIndex.main(cmdArgs);
		} else if (cmd.equals("MakeKeys") || cmd.equals("mkkeys")) {
			MakeKeys.main(cmdArgs);
		} else {
			System.err.println("ERROR: Unrecognized command " + cmd);
			System.exit(1);
		}
	}

}
