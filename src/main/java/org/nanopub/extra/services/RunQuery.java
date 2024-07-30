package org.nanopub.extra.services;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.opencsv.exceptions.CsvValidationException;

public class RunQuery {

	@com.beust.jcommander.Parameter(description = "query-id", required = true)
	private String queryId;

	@com.beust.jcommander.Parameter(names = "-p", description = "Parameters, URL-encoded and separated with '&'")
	private List<String> params;

	public static void main(String[] args) {
		RunQuery obj = new RunQuery();
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
