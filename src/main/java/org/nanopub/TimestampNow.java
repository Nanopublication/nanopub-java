package org.nanopub;

import com.beust.jcommander.ParameterException;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

import java.util.Date;

public class TimestampNow extends CliRunner {

	public static void main(String[] args) {
		TimestampNow obj = Run.initJc(new TimestampNow(), args);
		try {
			obj.run();
		} catch (ParameterException ex) {
			System.exit(1);
		} catch (Exception ex) {
			ex.printStackTrace();
			System.exit(1);
		}
	}

	private void run() {
		System.out.println(getTimestamp().stringValue());
	}

	public static Literal getTimestamp() {
		return SimpleValueFactory.getInstance().createLiteral(new Date());
	}

}
