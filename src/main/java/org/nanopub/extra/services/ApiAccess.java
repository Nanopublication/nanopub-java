package org.nanopub.extra.services;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.HttpResponse;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

/**
 * First-generation query API access. Deprecated and replaced by second-generation query services.
 */
@Deprecated
public abstract class ApiAccess {

	public static final String MAIN_GRLC_API_GENERIC_URL = "http://purl.org/nanopub/api/";

	protected abstract void processHeader(String[] line);

	protected abstract void processLine(String[] line);

	public void call(String apiUrl, String operation, Map<String,String> params) throws IOException, CsvValidationException {
		HttpResponse resp = ApiCall.run(apiUrl, operation, params);
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

	public static ApiResponse getAll(String operation, Map<String,String> params) throws IOException, CsvValidationException {
		return getAll(MAIN_GRLC_API_GENERIC_URL, operation, params);
	}

	public static ApiResponse getAll(String apiUrl, String operation, Map<String,String> params) throws IOException, CsvValidationException {
		final ApiResponse response = new ApiResponse();
		ApiAccess a = new ApiAccess() {

			@Override
			protected void processHeader(String[] line) {
				response.setHeader(line);
			}

			@Override
			protected void processLine(String[] line) {
				response.add(line);
			}

		};
		a.call(apiUrl, operation, params);
		return response;
	}

	public static ApiResponse getRecent(String operation, Map<String,String> params) {
		return getRecent(MAIN_GRLC_API_GENERIC_URL, operation, params);
	}

	public static ApiResponse getRecent(String apiUrl, String operation, Map<String,String> params) {
		Map<String,ApiResponseEntry> resultEntries = new HashMap<>();
		Map<String,ApiResponseEntry> overflowEntries = new HashMap<>();
		int moveLeftCount = 0;
		Calendar day = Calendar.getInstance();
		day.setTimeZone(timeZone);
		int level = 3;
		while (true) {
			Map<String,String> paramsx = new HashMap<>(params);
			if (level == 0) {
				paramsx.put("day", "http://purl.org/nanopub/admin/date/" + getDayString(day));
			} else if (level == 1) {
				paramsx.put("month", "http://purl.org/nanopub/admin/date/" + getMonthString(day));
			} else if (level == 2) {
				paramsx.put("year", "http://purl.org/nanopub/admin/date/" + getYearString(day));
			}
			ApiResponse tempResult;
			try {
				tempResult = getAll(apiUrl, operation, paramsx);
			} catch (Exception ex) {
				// TODO distinguish between different types of exceptions
				//ex.printStackTrace();
				System.err.println("Request not successful");
				if (level > 0) {
					level--;
					System.err.println("MOVE DOWN");
					continue;
				}
				break;
			}
			System.err.println("LIST SIZE:" + tempResult.size());
			if (tempResult.size() == 1000 && level > 0) {
				level--;
				System.err.println("MOVE DOWN");
				for (ApiResponseEntry r : tempResult.getData()) {
					overflowEntries.put(r.get("np"), r);
				}
				continue;
			}
			for (ApiResponseEntry r : tempResult.getData()) {
				resultEntries.put(r.get("np"), r);
			}
			System.err.println("RESULT SIZE:" + resultEntries.size());
			if (resultEntries.size() < 10) {
				if (level == 0) {
					if (day.get(Calendar.DAY_OF_MONTH) > 1) {
						if (moveLeftCount > 90) break;
						day.add(Calendar.DATE, -1);
						System.err.println("MOVE LEFT");
						moveLeftCount += 1;
						continue;
					}
				} else if (level == 1) {
					if (day.get(Calendar.MONTH) > 1) {
						if (moveLeftCount > 730) break;
						day.add(Calendar.MONTH, -1);
						day.set(Calendar.DAY_OF_MONTH, day.getActualMaximum(Calendar.DAY_OF_MONTH));
						System.err.println("MOVE LEFT");
						moveLeftCount += 30;
						continue;
					}
				} else if (level == 2) {
					if (day.get(Calendar.YEAR) > 2013) {
						day.set(Calendar.DAY_OF_MONTH, 31);
						day.set(Calendar.MONTH, 11);
						day.add(Calendar.YEAR, -1);
						System.err.println("MOVE LEFT");
						moveLeftCount += 365;
						continue;
					}
				}
			}
			break;
		}
		resultEntries.putAll(overflowEntries);
		ApiResponse response = new ApiResponse(resultEntries.values());
		Collections.sort(response.getData(), nanopubResultComparator);
		return response;
	}

	private static Map<String,Pair<Long,String>> latestVersionMap = new HashMap<>();

	public static String getLatestVersionId(String nanopubId) {
		long currentTime = System.currentTimeMillis();
		if (!latestVersionMap.containsKey(nanopubId) || currentTime - latestVersionMap.get(nanopubId).getLeft() > 1000*60*60) {
			// Re-fetch if existing value is older than 1 hour
			Map<String,String> params = new HashMap<>();
			params.put("np", nanopubId);
			try {
				ApiResponse r = ApiAccess.getAll(MAIN_GRLC_API_GENERIC_URL, "get_latest_version", params);
				if (r.getData().size() != 1) return nanopubId;
				String l = r.getData().get(0).get("latest");
				latestVersionMap.put(nanopubId, Pair.of(currentTime, l));
			} catch (Exception ex) {
				ex.printStackTrace();
				return nanopubId;
			}
		}
		return latestVersionMap.get(nanopubId).getRight();
	}

	private static TimeZone timeZone = TimeZone.getTimeZone("UTC");

	private static String getDayString(Calendar c) {
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
		df.setTimeZone(timeZone);
		return df.format(c.getTime());
	}

	private static String getMonthString(Calendar c) {
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM");
		df.setTimeZone(timeZone);
		return df.format(c.getTime());
	}

	private static String getYearString(Calendar c) {
		SimpleDateFormat df = new SimpleDateFormat("yyyy");
		df.setTimeZone(timeZone);
		return df.format(c.getTime());
	}

	private static Comparator<ApiResponseEntry> nanopubResultComparator = new Comparator<ApiResponseEntry>() {
		@Override
		public int compare(ApiResponseEntry e1, ApiResponseEntry e2) {
			return e2.get("date").compareTo(e1.get("date"));
		}
	};

}