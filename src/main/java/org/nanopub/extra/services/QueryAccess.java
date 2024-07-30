package org.nanopub.extra.services;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;

import org.apache.http.HttpResponse;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

/**
 * Second-generation query API access
 */
public abstract class QueryAccess {

	protected abstract void processHeader(String[] line);

	protected abstract void processLine(String[] line);

	public void call(String queryId, Map<String,String> params) throws IOException, CsvValidationException {
		CSVReader csvReader = null;
		try {
			HttpResponse resp = QueryCall.run(queryId, params);
			csvReader = new CSVReader(new BufferedReader(new InputStreamReader(resp.getEntity().getContent())));
			String[] line = null;
			int n = 0;
			while ((line = csvReader.readNext()) != null) {
				n++;
				if (n == 1) {
					processHeader(line);
				} else {
					processLine(line);
				}
			}
		} finally {
			if (csvReader != null) csvReader.close();
		}
	}

	public static ApiResponse get(String queryId, Map<String,String> params) throws IOException, CsvValidationException {
		final ApiResponse response = new ApiResponse();
		QueryAccess a = new QueryAccess() {

			@Override
			protected void processHeader(String[] line) {
				response.setHeader(line);
			}

			@Override
			protected void processLine(String[] line) {
				response.add(line);
			}

		};
		a.call(queryId, params);
		return response;
	}

}