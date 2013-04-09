package ch.tkuhn.nanopub;

import java.util.ArrayList;
import java.util.List;

import org.openrdf.model.Statement;

public class NanopubUtils {

	private NanopubUtils() {}  // no instances allowed

	public static List<Statement> getStatements(Nanopub nanopub) {
		List<Statement> s = new ArrayList<>();
		s.addAll(nanopub.getHead());
		s.addAll(nanopub.getAssertion());
		s.addAll(nanopub.getProvenance());
		s.addAll(nanopub.getPubinfo());
		return s;
	}

}
