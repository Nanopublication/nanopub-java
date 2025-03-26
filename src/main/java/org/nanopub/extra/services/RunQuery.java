package org.nanopub.extra.services;

import com.beust.jcommander.ParameterException;
import com.opencsv.exceptions.CsvValidationException;
import org.nanopub.CliRunner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RunQuery extends CliRunner {

	@com.beust.jcommander.Parameter(description = "query-id", required = true)
	private String queryId;

	@com.beust.jcommander.Parameter(names = "-p", description = "Parameters, URL-encoded and separated with '&'")
	private List<String> params;

	public static void main(String[] args) {
		try {
			RunQuery obj = CliRunner.initJc(new RunQuery(), args);
			obj.run();
		} catch (ParameterException ex) {
			System.exit(1);
		} catch (Exception ex) {
			ex.printStackTrace();
			System.exit(1);
		}
	}

	private void run() throws IOException, CsvValidationException {
		Map<String,String> paramMap = new HashMap<>();
		if (params != null) {
			for (String p : params) {
				int i = p.indexOf('=');
				String name = p.substring(0, i);
				String value = p.substring(i+1);
				paramMap.put(name, value);
			}
		}
		ApiResponse r = QueryAccess.get(queryId, paramMap);
		List<String> keys = null;
		for (ApiResponseEntry e : r.getData()) {
			if (keys == null) {
				keys = new ArrayList<String>(e.getKeys());
				for (String k : keys) System.out.print(k + "|");
				System.out.println();
			}
			for (String k : keys) System.out.print(e.get(k) + "|");
			System.out.println();
		}
	}

}
