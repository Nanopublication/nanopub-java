package org.nanopub.extra.services;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
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
		HttpResponse resp = QueryCall.run(queryId, params);
		try (CSVReader csvReader = new CSVReader(new BufferedReader(new InputStreamReader(resp.getEntity().getContent())))) {
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

	private static Map<String,Pair<Long,String>> latestVersionMap = new HashMap<>();
	// TODO Make a better query for this, where superseded and rejected are excluded from the start:
	private static final String GET_NEWER_VERSIONS = "RA3qSfVzcnAeMOODdpgCg4e-bX6KjZYZ2JQXDsSwluMaI/get-newer-versions-of-np";

	public static String getLatestVersionId(String nanopubId) {
		long currentTime = System.currentTimeMillis();
		if (!latestVersionMap.containsKey(nanopubId) || currentTime - latestVersionMap.get(nanopubId).getLeft() > 1000*60*60) {
			// Re-fetch if existing value is older than 1 hour
			Map<String,String> params = new HashMap<>();
			params.put("np", nanopubId);
			try {
				ApiResponse r = QueryAccess.get(GET_NEWER_VERSIONS, params);
				List<String> latestList = new ArrayList<>();
				for (ApiResponseEntry e : r.getData()) {
					if (e.get("retractedBy").isEmpty() && e.get("supersededBy").isEmpty()) {
						latestList.add(e.get("newerVersion"));
					}
				}
				if (latestList.size() == 1) {
					String latest = latestList.get(0);
					latestVersionMap.put(nanopubId, Pair.of(currentTime, latest));
					return latest;
				} else {
					return nanopubId;
				}
			} catch (Exception ex) {
				ex.printStackTrace();
				return nanopubId;
			}
		}
		return latestVersionMap.get(nanopubId).getRight();
	}

}