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
import org.eclipse.rdf4j.RDF4JException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.helpers.AbstractRDFHandler;
import org.eclipse.rdf4j.rio.turtle.TurtleParser;
import org.nanopub.Nanopub;
import org.nanopub.extra.server.GetNanopub;

import net.trustyuri.TrustyUriUtils;

public class IntroNanopub {

	public static IntroNanopub get(String userId) throws IOException, RDF4JException {
		RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(1000)
				.setConnectionRequestTimeout(100).setSocketTimeout(1000).build();
		HttpClient c = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).build();
		return get(userId, c);
	}

	public static IntroNanopub get(final String userId, HttpClient httpClient) throws IOException, RDF4JException {
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
			return new IntroNanopub(ie.getIntroNanopub(), SimpleValueFactory.getInstance().createIRI(userId));
		} finally {
			if (in != null) in.close();
		}
	}

	private Nanopub nanopub;
	private IRI user;
	private Map<IRI,KeyDeclaration> keyDeclarations = new HashMap<>();

	private IntroNanopub(Nanopub nanopub, IRI user) {
		this.nanopub = nanopub;
		this.user = user;
		for (Statement st : nanopub.getAssertion()) {
			if (st.getPredicate().equals(KeyDeclaration.DECLARED_BY) && st.getObject() instanceof IRI) {
				IRI subj = (IRI) st.getSubject();
				KeyDeclaration d;
				if (keyDeclarations.containsKey(subj)) {
					d = keyDeclarations.get(subj);
				} else {
					d = new KeyDeclaration(subj);
					keyDeclarations.put(subj, d);
				}
				d.addDeclarer((IRI) st.getObject());
			}
		}
		for (Statement st : nanopub.getAssertion()) {
			IRI subj = (IRI) st.getSubject();
			if (!keyDeclarations.containsKey(subj)) continue;
			KeyDeclaration d = keyDeclarations.get(subj);
			IRI pred = st.getPredicate();
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

	public IRI getUser() {
		return user;
	}

	public List<KeyDeclaration> getKeyDeclarations() {
		return new ArrayList<>(keyDeclarations.values());
	}


	private static class IntroExtractor extends AbstractRDFHandler {

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
