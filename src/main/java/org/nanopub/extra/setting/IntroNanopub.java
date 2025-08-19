package org.nanopub.extra.setting;

import net.trustyuri.TrustyUriUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.eclipse.rdf4j.common.exception.RDF4JException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.helpers.AbstractRDFHandler;
import org.eclipse.rdf4j.rio.turtle.TurtleParser;
import org.nanopub.Nanopub;
import org.nanopub.NanopubUtils;
import org.nanopub.extra.security.KeyDeclaration;
import org.nanopub.extra.security.MalformedCryptoElementException;
import org.nanopub.extra.server.GetNanopub;
import org.nanopub.vocabulary.NPX;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class represents an Intro Nanopub, which is a nanopublication that contains the introduction of a user.
 */
public class IntroNanopub implements Serializable {

    /**
     * Get the IntroNanopub for a userId.
     *
     * @param userId the userId to get the IntroNanopub for
     * @return the IntroNanopub for the userId, or null if not found
     * @throws java.io.IOException                               if there is an error fetching the IntroNanopub
     * @throws org.eclipse.rdf4j.common.exception.RDF4JException if there is an error parsing the RDF data
     */
    public static IntroNanopub get(String userId) throws IOException, RDF4JException {
        return get(userId, (HttpClient) null);
    }

    /**
     * Get the IntroNanopub for a userId using a specific HttpClient.
     *
     * @param userId     the userId to get the IntroNanopub for
     * @param httpClient the HttpClient to use for fetching the IntroNanopub
     * @return the IntroNanopub for the userId, or null if not found
     * @throws java.io.IOException                               if there is an error fetching the IntroNanopub
     * @throws org.eclipse.rdf4j.common.exception.RDF4JException if there is an error parsing the RDF data
     */
    public static IntroNanopub get(String userId, HttpClient httpClient) throws IOException, RDF4JException {
        IntroExtractor ie = extract(userId, httpClient);
        if (ie != null) {
            return new IntroNanopub(ie.getIntroNanopub(), SimpleValueFactory.getInstance().createIRI(userId));
        }
        return null;
    }

    /**
     * Get the IntroNanopub for a userId using an IntroExtractor.
     *
     * @param userId the userId to get the IntroNanopub for
     * @param ie     the IntroExtractor that has already extracted the IntroNanopub
     * @return the IntroNanopub for the userId, or null if not found
     */
    public static IntroNanopub get(String userId, IntroExtractor ie) {
        return new IntroNanopub(ie.getIntroNanopub(), SimpleValueFactory.getInstance().createIRI(userId));
    }

    /**
     * Extract the IntroNanopub for a userId.
     *
     * @param userId     the userId to extract the IntroNanopub for
     * @param httpClient the HttpClient to use for fetching the IntroNanopub
     * @return the IntroExtractor containing the extracted data
     * @throws java.io.IOException                               if there is an error fetching the IntroNanopub
     * @throws org.eclipse.rdf4j.common.exception.RDF4JException if there is an error parsing the RDF data
     */
    public static IntroExtractor extract(String userId, HttpClient httpClient) throws IOException, RDF4JException {
        if (httpClient == null) httpClient = NanopubUtils.getHttpClient();
        HttpGet get = null;
        try {
            get = new HttpGet(userId);
        } catch (IllegalArgumentException ex) {
            throw new IOException("invalid URL: " + userId);
        }
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
            return ie;
        } finally {
            if (in != null) in.close();
        }
    }

    private Nanopub nanopub;
    private IRI user;
    private String name;
    private Map<IRI, KeyDeclaration> keyDeclarations = new HashMap<>();

    /**
     * Constructor for IntroNanopub.
     *
     * @param nanopub the Nanopub that contains the introduction
     */
    public IntroNanopub(Nanopub nanopub) {
        this(nanopub, null);
    }

    /**
     * Constructor for IntroNanopub with user.
     *
     * @param nanopub the Nanopub that contains the introduction
     * @param user    the IRI of the user
     */
    public IntroNanopub(Nanopub nanopub, IRI user) {
        this.nanopub = nanopub;
        this.user = user;
        for (Statement st : nanopub.getAssertion()) {
            if (!(st.getObject() instanceof IRI obj)) continue;
            IRI subj = (IRI) st.getSubject();
            IRI pred = st.getPredicate();
            if (pred.equals(NPX.DECLARED_BY) || pred.equals(NPX.HAS_KEY_LOCATION)) {
                KeyDeclaration d;
                if (keyDeclarations.containsKey(subj)) {
                    d = keyDeclarations.get(subj);
                } else {
                    d = new KeyDeclaration(subj);
                    keyDeclarations.put(subj, d);
                }
                if (pred.equals(NPX.DECLARED_BY)) {
                    if (this.user == null) this.user = obj;
                    if (!this.user.equals(obj)) continue;
                    d.addDeclarer(this.user);
                } else if (pred.equals(NPX.HAS_KEY_LOCATION)) {
                    d.setKeyLocation(obj);
                }
            }
        }
        for (Statement st : nanopub.getAssertion()) {
            IRI subj = (IRI) st.getSubject();
            if (subj.equals(this.user) && st.getPredicate().equals(FOAF.NAME)) {
                this.name = st.getObject().stringValue();
            } else if (keyDeclarations.containsKey(subj)) {
                KeyDeclaration d = keyDeclarations.get(subj);
                IRI pred = st.getPredicate();
                Value obj = st.getObject();
                if (pred.equals(NPX.HAS_ALGORITHM) && obj instanceof Literal) {
                    try {
                        d.setAlgorithm((Literal) obj);
                    } catch (MalformedCryptoElementException ex) {
                        //ex.printStackTrace();
                    }
                } else if (pred.equals(NPX.HAS_PUBLIC_KEY) && obj instanceof Literal) {
                    try {
                        d.setPublicKeyLiteral((Literal) obj);
                    } catch (MalformedCryptoElementException ex) {
                        //ex.printStackTrace();
                    }
                }
            }
        }
        for (IRI kdi : new ArrayList<>(keyDeclarations.keySet())) {
            KeyDeclaration kd = keyDeclarations.get(kdi);
            if (kd.getPublicKeyString() == null || kd.getPublicKeyString().isEmpty() || kd.getDeclarers().isEmpty()) {
                keyDeclarations.remove(kdi);
            }
        }
    }

    /**
     * Get the Nanopub that contains the introduction.
     *
     * @return the Nanopub
     */
    public Nanopub getNanopub() {
        return nanopub;
    }

    /**
     * Get the IRI of the user.
     *
     * @return the IRI of the user
     */
    public IRI getUser() {
        return user;
    }

    /**
     * Get the name of the user.
     *
     * @return the name of the user, or null if not set
     */
    public String getName() {
        return name;
    }

    /**
     * Get the key declarations associated with this IntroNanopub.
     *
     * @return a list of KeyDeclaration objects
     */
    public List<KeyDeclaration> getKeyDeclarations() {
        return new ArrayList<>(keyDeclarations.values());
    }


    /**
     * This class is used to extract the introduction Nanopub.
     */
    public static class IntroExtractor extends AbstractRDFHandler implements Serializable {

        private String userId;
        private Nanopub introNanopub;
        private String name;

        /**
         * Constructor for IntroExtractor.
         *
         * @param userId the userId to extract the introduction for
         */
        public IntroExtractor(String userId) {
            this.userId = userId;
        }

        /**
         * Handle a statement in the RDF data.
         *
         * @param st the statement to handle
         * @throws RDFHandlerException if there is an error handling the statement
         */
        public void handleStatement(Statement st) throws RDFHandlerException {
            if (introNanopub != null) return;
            if (!st.getSubject().stringValue().equals(userId)) return;
            if (st.getPredicate().stringValue().equals(FOAF.PAGE.stringValue())) {
                String o = st.getObject().stringValue();
                if (TrustyUriUtils.isPotentialTrustyUri(o)) {
                    introNanopub = GetNanopub.get(o);
                }
            } else if (st.getPredicate().stringValue().equals(RDFS.LABEL.stringValue())) {
                name = st.getObject().stringValue();
            }
        }

        /**
         * Returns the extracted Nanopub that contains the introduction.
         *
         * @return the Nanopub that contains the introduction
         * @throws RDFHandlerException if there is an error handling the end of the data
         */
        public Nanopub getIntroNanopub() {
            return introNanopub;
        }

        /**
         * Returns the name of the user.
         *
         * @return the name of the user, or null if not set
         */
        public String getName() {
            return name;
        }

    }

    private static boolean wasSuccessful(HttpResponse resp) {
        int c = resp.getStatusLine().getStatusCode();
        return c >= 200 && c < 300;
    }

}
