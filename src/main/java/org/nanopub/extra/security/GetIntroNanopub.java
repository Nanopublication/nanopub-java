package org.nanopub.extra.security;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.nanopub.Nanopub;
import org.nanopub.NanopubImpl;
import org.nanopub.NanopubUtils;
import org.nanopub.extra.server.GetNanopub;
import org.openrdf.OpenRDFException;
import org.openrdf.model.Statement;
import org.openrdf.model.vocabulary.FOAF;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.helpers.RDFHandlerBase;
import org.openrdf.rio.turtle.TurtleParser;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

import net.trustyuri.TrustyUriUtils;

public class GetIntroNanopub {

	@com.beust.jcommander.Parameter(description = "user-id", required = true)
	private List<String> userIds;

	public static void main(String[] args) {
		NanopubImpl.ensureLoaded();
		GetIntroNanopub obj = new GetIntroNanopub();
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

	public static Nanopub get(String userId) throws IOException, OpenRDFException {
		RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(10000)
				.setConnectionRequestTimeout(100).setSocketTimeout(10000).build();
		HttpClient c = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).build();
		return get(userId, c);
	}

	public static Nanopub get(final String userId, HttpClient httpClient) throws IOException, OpenRDFException {
		HttpGet get = new HttpGet(userId);
		get.setHeader("Accept", "text/turtle");
		InputStream in = null;
		try {
			HttpResponse resp = httpClient.execute(get);
			if (!wasSuccessful(resp)) {
				EntityUtils.consumeQuietly(resp.getEntity());
				throw new IOException(resp.getStatusLine().toString());
			}
			in = resp.getEntity().getContent();
			IntroExtractor ie = new IntroExtractor(userId);
			TurtleParser parser = new TurtleParser();
			parser.setRDFHandler(ie);
			parser.parse(in, userId);
			return ie.getIntroNanopub();
		} finally {
			if (in != null) in.close();
		}
	}

	private static class IntroExtractor extends RDFHandlerBase {

		private String userId;
		private Nanopub introNanopub;

		public IntroExtractor(String userId) {
			this.userId = userId;
		}

		public void handleStatement(Statement st) throws RDFHandlerException {
			if (introNanopub != null) return;
			if (!st.getSubject().stringValue().equals(userId)) return;
			if (!st.getPredicate().stringValue().equals(FOAF.PAGE.stringValue())) return;
			String o = st.getObject().stringValue();
			if (TrustyUriUtils.isPotentialTrustyUri(o)) {
				introNanopub = GetNanopub.get(o);
			}
		};

		public Nanopub getIntroNanopub() {
			return introNanopub;
		}
	}
	
	private static boolean wasSuccessful(HttpResponse resp) {
		int c = resp.getStatusLine().getStatusCode();
		return c >= 200 && c < 300;
	}

	private void run() throws IOException, OpenRDFException {
		for (String userId : userIds) {
			NanopubUtils.writeToStream(get(userId), System.out, RDFFormat.TRIG);
		}
	}

}
