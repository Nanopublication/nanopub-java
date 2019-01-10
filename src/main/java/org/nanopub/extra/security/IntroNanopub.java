package org.nanopub.extra.security;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.nanopub.Nanopub;
import org.nanopub.extra.server.GetNanopub;
import org.openrdf.OpenRDFException;
import org.openrdf.model.Literal;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.vocabulary.FOAF;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.helpers.RDFHandlerBase;
import org.openrdf.rio.turtle.TurtleParser;

import net.trustyuri.TrustyUriUtils;

public class IntroNanopub {

	public static IntroNanopub get(String userId) throws IOException, OpenRDFException {
		RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(10000)
				.setConnectionRequestTimeout(100).setSocketTimeout(10000).build();
		HttpClient c = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).build();
		return get(userId, c);
	}

	public static IntroNanopub get(final String userId, HttpClient httpClient) throws IOException, OpenRDFException {
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
			return new IntroNanopub(ie.getIntroNanopub(), new URIImpl(userId));
		} finally {
			if (in != null) in.close();
		}
	}

	private Nanopub nanopub;
	private URI user;
	private Map<URI,KeyDeclaration> keyDeclarations = new HashMap<>();

	private IntroNanopub(Nanopub nanopub, URI user) {
		this.nanopub = nanopub;
		this.user = user;
		for (Statement st : nanopub.getAssertion()) {
			if (st.getPredicate().equals(KeyDeclaration.DECLARED_BY) && st.getObject() instanceof URI) {
				URI subj = (URI) st.getSubject();
				KeyDeclaration d;
				if (keyDeclarations.containsKey(subj)) {
					d = keyDeclarations.get(subj);
				} else {
					d = new KeyDeclaration(subj);
					keyDeclarations.put(subj, d);
				}
				d.addDeclarer((URI) st.getObject());
			}
		}
		for (Statement st : nanopub.getAssertion()) {
			URI subj = (URI) st.getSubject();
			if (!keyDeclarations.containsKey(subj)) continue;
			KeyDeclaration d = keyDeclarations.get(subj);
			URI pred = st.getPredicate();
			Value obj = st.getObject();
			if (pred.equals(CryptoElement.HAS_ALGORITHM) && obj instanceof Literal) {
				try {
					d.setAlgorithm((Literal) obj);
				} catch (MalformedCryptoElementException ex) {
					ex.printStackTrace();
				}
			} else if (pred.equals(CryptoElement.HAS_PUBLIC_KEY) && obj instanceof Literal) {
				try {
					d.setPublicKeyLiteral((Literal) obj);
				} catch (MalformedCryptoElementException ex) {
					ex.printStackTrace();
				}
			}
		}
	}

	public Nanopub getNanopub() {
		return nanopub;
	}

	public URI getUser() {
		return user;
	}

	public List<KeyDeclaration> getKeyDeclarations() {
		return new ArrayList<>(keyDeclarations.values());
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

}
