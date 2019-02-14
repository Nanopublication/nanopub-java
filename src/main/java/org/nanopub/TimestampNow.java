package org.nanopub;

import java.util.Date;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;


public class TimestampNow {

	public static void main(String[] args) {
		NanopubImpl.ensureLoaded();
		TimestampNow obj = new TimestampNow();
		JCommander jc = new JCommander(obj);
		try {
			jc.parse(args);
		} catch (ParameterException ex) {
			jc.usage();
			System.exit(1);
		}
		try {
			obj.run();
		} catch (Exception ex) {
			ex.printStackTrace();
			System.exit(1);
		}
	}

	private TimestampNow() {
	}

	private void run() {
		System.out.println(getTimestamp().stringValue());
	}

	public static Literal getTimestamp() {
		return SimpleValueFactory.getInstance().createLiteral(new Date());
	}

}
