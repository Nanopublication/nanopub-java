package org.nanopub;

import net.trustyuri.TrustyUriUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.*;
import org.eclipse.rdf4j.rio.*;
import org.eclipse.rdf4j.rio.helpers.BasicParserSettings;
import org.nanopub.trusty.TempUriReplacer;
import org.nanopub.trusty.TrustyNanopubUtils;
import org.nanopub.vocabulary.NP;
import org.nanopub.vocabulary.NPX;
import org.nanopub.vocabulary.PAV;
import org.nanopub.vocabulary.RDFG;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Utility class for working with Nanopubs.
 *
 * @author Tobias Kuhn
 */
public class NanopubUtils {

    private static final List<Pair<String, String>> defaultNamespaces = new ArrayList<>();
    private static final Random random = new Random();
    private static final ValueFactory vf = SimpleValueFactory.getInstance();
    private static CloseableHttpClient httpClient;

    /**
     * The initial checksum for a Nanopub, which is a base64-encoded 32-byte zero array.
     */
    public static final String INIT_CHECKSUM = TrustyUriUtils.getBase64(new byte[32]);

    private NanopubUtils() {
    }  // no instances allowed

    static {
        defaultNamespaces.add(Pair.of(RDF.PREFIX, RDF.NAMESPACE));
        defaultNamespaces.add(Pair.of(RDFS.PREFIX, RDFS.NAMESPACE));
        defaultNamespaces.add(Pair.of(RDFG.PREFIX, RDFG.NAMESPACE));
        defaultNamespaces.add(Pair.of(XSD.PREFIX, XSD.NAMESPACE));
        defaultNamespaces.add(Pair.of(OWL.PREFIX, OWL.NAMESPACE));
        defaultNamespaces.add(Pair.of("dct", DCTERMS.NAMESPACE));
        defaultNamespaces.add(Pair.of("dce", DC.NAMESPACE));
        defaultNamespaces.add(Pair.of(PAV.PREFIX, PAV.NAMESPACE));
        defaultNamespaces.add(Pair.of(PROV.PREFIX, PROV.NAMESPACE));
        defaultNamespaces.add(Pair.of(NP.PREFIX, NP.NAMESPACE));
    }

    /**
     * Returns the default namespaces used in Nanopubs.
     *
     * @return a list of pairs containing namespace prefixes and URIs
     */
    public static List<Pair<String, String>> getDefaultNamespaces() {
        return defaultNamespaces;
    }

    /**
     * Returns a sorted list of all statements in the given Nanopub.
     * The order is: head, assertion, provenance, pubinfo.
     *
     * @param nanopub the Nanopub to get the statements from
     * @return a sorted list of statements
     */
    public static List<Statement> getStatements(Nanopub nanopub) {
        List<Statement> s = new ArrayList<>();
        s.addAll(getSortedList(nanopub.getHead()));
        s.addAll(getSortedList(nanopub.getAssertion()));
        s.addAll(getSortedList(nanopub.getProvenance()));
        s.addAll(getSortedList(nanopub.getPubinfo()));
        return s;
    }

    private static List<Statement> getSortedList(Set<Statement> s) {
        List<Statement> l = new ArrayList<>(s);
        l.sort((st1, st2) -> {
            // TODO better sorting
            // it works fine for now, since AbstractStatement has a valid toString()
            // implementation, which does not consist of any runtime object references
            return st1.toString().compareTo(st2.toString());
        });
        return l;
    }

    /**
     * Writes the given Nanopub to the specified output stream in the given RDF format.
     *
     * @param nanopub the Nanopub to write
     * @param out     the output stream to write to
     * @param format  the RDF format to use
     * @throws org.eclipse.rdf4j.rio.RDFHandlerException if an error occurs while writing
     */
    public static void writeToStream(Nanopub nanopub, OutputStream out, RDFFormat format) throws RDFHandlerException {
        writeNanopub(nanopub, format, new OutputStreamWriter(out, StandardCharsets.UTF_8));
    }

    /**
     * Writes the given Nanopub to a string in the specified RDF format.
     *
     * @param nanopub the Nanopub to write
     * @param format  the RDF format to use
     * @return a string representation of the Nanopub in the specified format
     * @throws org.eclipse.rdf4j.rio.RDFHandlerException if an error occurs while writing
     * @throws java.io.IOException                       if an I/O error occurs
     */
    public static String writeToString(Nanopub nanopub, RDFFormat format) throws RDFHandlerException, IOException {
        try (StringWriter sw = new StringWriter()) {
            writeNanopub(nanopub, format, sw);
            return sw.toString();
        }
    }

    private static void writeNanopub(Nanopub nanopub, RDFFormat format, Writer writer) throws RDFHandlerException {
        try {
            if (format.equals(TrustyNanopubUtils.STNP_FORMAT)) {
                writer.write(TrustyNanopubUtils.getTrustyDigestString(nanopub));
                writer.flush();
            } else {
                RDFWriter rdfWriter = Rio.createWriter(format, writer);
                propagateToHandler(nanopub, rdfWriter);
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Propagates the Nanopub to the given RDFHandler, starting with the RDF header and namespaces.
     *
     * @param nanopub the Nanopub to propagate
     * @param handler the RDFHandler to propagate to
     * @throws org.eclipse.rdf4j.rio.RDFHandlerException if an error occurs while handling RDF
     */
    public static void propagateToHandler(Nanopub nanopub, RDFHandler handler) throws RDFHandlerException {
        handler.startRDF();
        if (nanopub instanceof NanopubWithNs np && !np.getNsPrefixes().isEmpty()) {
            for (String p : np.getNsPrefixes()) {
                handler.handleNamespace(p, np.getNamespace(p));
            }
        } else {
            handler.handleNamespace("this", nanopub.getUri().toString());
            for (Pair<String, String> p : defaultNamespaces) {
                handler.handleNamespace(p.getLeft(), p.getRight());
            }
        }
        for (Statement st : getStatements(nanopub)) {
            handler.handleStatement(st);
        }
        handler.endRDF();
    }

    /**
     * Creates an RDFParser for the specified RDF format.
     *
     * @param format the RDF format to create a parser for
     * @return an RDFParser configured for the specified format
     */
    public static RDFParser getParser(RDFFormat format) {
        RDFParser p = Rio.createParser(format);
        p.getParserConfig().set(BasicParserSettings.NAMESPACES, new HashSet<>());
        return p;
    }

    /**
     * Returns a set of prefixes used in the Nanopub's namespaces.
     *
     * @param np the Nanopub to analyze
     * @return a set of namespace prefixes
     */
    public static Set<String> getUsedPrefixes(NanopubWithNs np) {
        Set<String> usedPrefixes = new HashSet<>();
        CustomTrigWriter writer = new CustomTrigWriter(usedPrefixes);
        try {
            NanopubUtils.propagateToHandler(np, writer);
        } catch (RDFHandlerException ex) {
            ex.printStackTrace();
            return usedPrefixes;
        }
        return usedPrefixes;
    }

    /**
     * Returns a label for the Nanopub, which is derived from its assertion and pubinfo.
     * The label is constructed from RDFS labels and DCTERMS titles.
     *
     * @param np the Nanopub to get the label for
     * @return a string label for the Nanopub, or null if no label can be found
     */
    public static String getLabel(Nanopub np) {
        final String separator = " ";
        String npLabel = "", npTitle = "", aLabel = "", aTitle = "", introLabel = "";
        final IRI npId = np.getUri();
        final IRI aId = np.getAssertionUri();
        final Map<IRI, Boolean> introMap = new HashMap<>();
        for (Statement st : np.getPubinfo()) {
            final Resource subj = st.getSubject();
            final IRI pred = st.getPredicate();
            final Value obj = st.getObject();
            if (subj.equals(npId) && pred.equals(RDFS.LABEL) && obj instanceof Literal) {
                npLabel += separator + obj.stringValue();
            }
            if (subj.equals(npId) && (pred.equals(DCTERMS.TITLE) || pred.equals(DC.TITLE)) && obj instanceof Literal) {
                npTitle += separator + obj.stringValue();
            }
            if (subj.equals(npId) && (pred.equals(NPX.INTRODUCES) || pred.equals(NPX.DESCRIBES) || pred.equals(NPX.EMBEDS)) && obj instanceof IRI) {
                introMap.put((IRI) obj, true);
            }
        }
        for (Statement st : np.getProvenance()) {
            final Resource subj = st.getSubject();
            final IRI pred = st.getPredicate();
            final Value obj = st.getObject();
            if (subj.equals(aId) && pred.equals(RDFS.LABEL) && obj instanceof Literal) {
                aLabel += separator + obj.stringValue();
            }
            if (subj.equals(aId) && pred.equals(DCTERMS.TITLE) && obj instanceof Literal) {
                aTitle += separator + obj.stringValue();
            }
        }
        for (Statement st : np.getAssertion()) {
            final Resource subj = st.getSubject();
            final IRI pred = st.getPredicate();
            final Value obj = st.getObject();
            if (subj.equals(aId) && pred.equals(RDFS.LABEL) && obj instanceof Literal) {
                aLabel += separator + obj.stringValue();
            }
            if (subj.equals(aId) && (pred.equals(DCTERMS.TITLE) || pred.equals(DC.TITLE)) && obj instanceof Literal) {
                aTitle += separator + obj.stringValue();
            }
            if (introMap.containsKey(subj) && pred.equals(RDFS.LABEL) && obj instanceof Literal) {
                introLabel += separator + obj.stringValue();
            }
        }
        if (!npLabel.isEmpty()) return npLabel.substring(1);
        if (!npTitle.isEmpty()) return npTitle.substring(1);
        if (!aLabel.isEmpty()) return aLabel.substring(1);
        if (!aTitle.isEmpty()) return aTitle.substring(1);
        if (!introLabel.isEmpty()) return introLabel.substring(1);
        return null;
    }

    /**
     * Returns a description for the Nanopub, which is derived from its assertion, pubinfo, and introduction.
     * The description is constructed from DCTERMS descriptions, RDFS comments, and SKOS definitions.
     *
     * @param np the Nanopub to get the description for
     * @return a string description for the Nanopub, or null if no description can be found
     */
    public static String getDescription(Nanopub np) {
        String npDesc = "", npComment = "", npDef = "", aDesc = "", aComment = "", aDef = "", iDesc = "", iComment = "", iDef = "";
        final IRI npId = np.getUri();
        final IRI aId = np.getAssertionUri();
        final Map<IRI, Boolean> introMap = new HashMap<>();
        for (Statement st : np.getPubinfo()) {
            final Resource subj = st.getSubject();
            final IRI pred = st.getPredicate();
            final Value obj = st.getObject();
            if (!subj.equals(npId)) continue;
            if (obj instanceof Literal) {
                if (pred.equals(DCTERMS.DESCRIPTION) || pred.equals(DC.DESCRIPTION)) {
                    npDesc += "\n" + obj.stringValue();
                } else if (pred.equals(RDFS.COMMENT)) {
                    npComment += "\n" + obj.stringValue();
                } else if (pred.equals(SKOS.DEFINITION)) {
                    npDef += "\n" + obj.stringValue();
                }
            } else {
                if (pred.equals(NPX.INTRODUCES) || pred.equals(NPX.DESCRIBES) || pred.equals(NPX.EMBEDS)) {
                    introMap.put((IRI) obj, true);
                }
            }
        }
        for (Statement st : np.getProvenance()) {
            final Resource subj = st.getSubject();
            final IRI pred = st.getPredicate();
            final Value obj = st.getObject();
            if (!(obj instanceof Literal)) continue;
            if (!subj.equals(aId)) continue;
            if (pred.equals(DCTERMS.DESCRIPTION) || pred.equals(DC.DESCRIPTION)) {
                aDesc += "\n" + obj.stringValue();
            } else if (pred.equals(RDFS.COMMENT)) {
                aComment += "\n" + obj.stringValue();
            } else if (pred.equals(SKOS.DEFINITION)) {
                aDef += "\n" + obj.stringValue();
            }
        }
        for (Statement st : np.getAssertion()) {
            final Resource subj = st.getSubject();
            final IRI pred = st.getPredicate();
            final Value obj = st.getObject();
            if (!(obj instanceof Literal)) continue;
            if (subj.equals(aId)) {
                if (pred.equals(DCTERMS.DESCRIPTION) || pred.equals(DC.DESCRIPTION)) {
                    aDesc += "\n" + obj.stringValue();
                } else if (pred.equals(RDFS.COMMENT)) {
                    aComment += "\n" + obj.stringValue();
                } else if (pred.equals(SKOS.DEFINITION)) {
                    aDef += "\n" + obj.stringValue();
                }
            }
            if (introMap.containsKey(subj)) {
                if (pred.equals(DCTERMS.DESCRIPTION) || pred.equals(DC.DESCRIPTION)) {
                    iDesc += " " + obj.stringValue();
                } else if (pred.equals(RDFS.COMMENT)) {
                    iComment += " " + obj.stringValue();
                } else if (pred.equals(SKOS.DEFINITION)) {
                    iDef += " " + obj.stringValue();
                }
            }
        }
        String description = "";
        if (!npDesc.isEmpty()) description += npDesc;
        if (!npComment.isEmpty()) description += npComment;
        if (!npDef.isEmpty()) description += npDef;
        if (!aDesc.isEmpty()) description += aDesc;
        if (!aComment.isEmpty()) description += aComment;
        if (!aDef.isEmpty()) description += aDef;
        if (!iDesc.isEmpty()) description += iDesc;
        if (!iComment.isEmpty()) description += iComment;
        if (!iDef.isEmpty()) description += iDef;
        if (description.isEmpty()) return null;
        return description.substring(1);
    }

    /**
     * Returns a set of types for the given Nanopub.
     * Types are derived from RDF type statements in the assertion and pubinfo,
     * as well as introduction nanopubs.
     *
     * @param np the Nanopub to get the types for
     * @return a set of IRI types associated with the Nanopub
     */
    public static Set<IRI> getTypes(Nanopub np) {
        final Set<IRI> types = new HashSet<>();
        final IRI npId = np.getUri();
        final IRI aId = np.getAssertionUri();
        final Map<IRI, Boolean> introMap = new HashMap<>();
        for (Statement st : np.getPubinfo()) {
            final Resource subj = st.getSubject();
            final IRI pred = st.getPredicate();
            final Value obj = st.getObject();
            if (subj.equals(npId) && pred.equals(RDF.TYPE) && obj instanceof IRI) {
                types.add((IRI) obj);
            }
            if (subj.equals(npId) && pred.equals(NPX.HAS_NANOPUB_TYPE) && obj instanceof IRI) {
                types.add((IRI) obj);
            }
            if (subj.equals(npId) && (pred.equals(NPX.INTRODUCES) || pred.equals(NPX.DESCRIBES) || pred.equals(NPX.EMBEDS)) && obj instanceof IRI) {
                introMap.put((IRI) obj, true);
            }
        }
        for (Statement st : np.getProvenance()) {
            final Resource subj = st.getSubject();
            final IRI pred = st.getPredicate();
            final Value obj = st.getObject();
            if (subj.equals(aId) && pred.equals(RDF.TYPE) && obj instanceof IRI) {
                types.add((IRI) obj);
            }
        }
        IRI onlySubjectInAssertion = null;
        List<IRI> allTypes = new ArrayList<>();
        boolean hasOnlySubjectInAssertion = true;
        IRI onlyPredicateInAssertion = null;
        boolean hasOnlyPredicateInAssertion = true;
        for (Statement st : np.getAssertion()) {
            final IRI subj = (IRI) st.getSubject();
            final IRI pred = st.getPredicate();
            final Value obj = st.getObject();
            if (pred.equals(RDF.TYPE) && obj instanceof IRI) {
                allTypes.add((IRI) obj);
                if (subj.equals(aId)) types.add((IRI) obj);
                if (introMap.containsKey(subj)) types.add((IRI) obj);
            }
            if (pred.equals(NPX.DECLARED_BY)) {
                // This predicate is used in introduction nanopubs for users. To simplify backwards compatibility,
                // this predicate is treated as a special case that triggers a type assignment.
                types.add(pred);
            }
            if (onlySubjectInAssertion == null) {
                onlySubjectInAssertion = subj;
            } else if (!onlySubjectInAssertion.equals(subj)) {
                hasOnlySubjectInAssertion = false;
            }
            if (onlyPredicateInAssertion == null) {
                onlyPredicateInAssertion = pred;
            } else if (!onlyPredicateInAssertion.equals(pred)) {
                hasOnlyPredicateInAssertion = false;
            }
        }
        if (hasOnlySubjectInAssertion) types.addAll(allTypes);
        if (hasOnlyPredicateInAssertion) types.add(onlyPredicateInAssertion);
        return types;
    }

    /**
     * For the first 32 bytes of the checksum, XOR them with the nanupub ID (starting at 3rd character)
     *
     * @param nanopubId the IRI of the Nanopub
     * @param checksum  the base64-encoded checksum to update
     * @return base64 representation of (checksum XOR nanopubId)
     */
    public static String updateXorChecksum(IRI nanopubId, String checksum) {
        byte[] checksumBytes = TrustyUriUtils.getBase64Bytes(checksum);
        if (checksumBytes.length < 32) {
            throw new IllegalArgumentException("Checksum must be at least 32 bytes long.");
        }
        byte[] addBytes = TrustyUriUtils.getBase64Bytes(TrustyUriUtils.getArtifactCode(nanopubId.stringValue()).substring(2));
        for (int i = 0; i < 32; i++) {
            checksumBytes[i] = (byte) (checksumBytes[i] ^ addBytes[i]);
        }
        return TrustyUriUtils.getBase64(checksumBytes);
    }

    /**
     * Returns a singleton instance of CloseableHttpClient with a custom configuration.
     *
     * @return a CloseableHttpClient instance
     */
    public static CloseableHttpClient getHttpClient() {
        if (httpClient == null) {
            RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(10000).setConnectionRequestTimeout(500).setSocketTimeout(10000).setCookieSpec(CookieSpecs.STANDARD).build();
            PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager();
            connManager.setDefaultMaxPerRoute(200);
            connManager.setMaxTotal(400);
            httpClient = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).setConnectionManager(connManager).build();
        }
        return httpClient;
    }

    /**
     * Creates a temporary Nanopub IRI with a random integer.
     *
     * @return a new IRI for a temporary Nanopub
     */
    public static IRI createTempNanopubIri() {
        return vf.createIRI(TempUriReplacer.tempUri + Math.abs(random.nextInt()) + "/");
    }

    /**
     * Retrieves a set of introduced IRI IDs from the nanopublication.
     *
     * @param np the nanopublication from which to extract introduced IRI IDs
     * @return a set of introduced IRI IDs
     */
    public static Set<String> getIntroducedIriIds(Nanopub np) {
        Set<String> introducedIriIds = new HashSet<>();
        for (Statement st : np.getPubinfo()) {
            if (!st.getSubject().equals(np.getUri())) {
                continue;
            }
            IRI p = st.getPredicate();
            if (!p.equals(NPX.INTRODUCES) && !p.equals(NPX.DESCRIBES) && !p.equals(NPX.EMBEDS)) {
                continue;
            }
            if (st.getObject() instanceof IRI obj) {
                introducedIriIds.add(obj.stringValue());
            }
        }
        return introducedIriIds;
    }

    /**
     * Retrieves a set of embedded IRI IDs from the nanopublication.
     *
     * @param np the nanopublication from which to extract embedded IRI IDs
     * @return a set of embedded IRI IDs
     */
    public static Set<String> getEmbeddedIriIds(Nanopub np) {
        Set<String> embeddedIriIds = new HashSet<>();
        for (Statement st : np.getPubinfo()) {
            if (!st.getSubject().equals(np.getUri())) {
                continue;
            }
            if (!st.getPredicate().equals(NPX.EMBEDS)) {
                continue;
            }
            if (st.getObject() instanceof IRI obj) {
                embeddedIriIds.add(obj.stringValue());
            }
        }
        return embeddedIriIds;
    }

}
