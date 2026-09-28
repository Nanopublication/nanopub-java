package org.nanopub.extra.services;

import net.trustyuri.TrustyUriUtils;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractSimpleQueryModelVisitor;
import org.eclipse.rdf4j.query.parser.ParsedGraphQuery;
import org.eclipse.rdf4j.query.parser.ParsedQuery;
import org.eclipse.rdf4j.query.parser.sparql.SPARQLParser;
import org.nanopub.Nanopub;
import org.nanopub.extra.server.GetNanopub;
import org.nanopub.vocabulary.KPXL_GRLC;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A parameterizable SPARQL query template extracted from a nanopublication.
 *
 * <p>Templates are identified by a {@code queryId} of the form
 * {@code RA<43-char-artifact-code>/<query-suffix>} (or with {@code #} in place of
 * the inner {@code /}). They carry a SPARQL string, a target endpoint, optional
 * label/description/license, and a list of placeholder variables that can be
 * substituted via {@link #expandQuery(Map)}.
 *
 * <p>Placeholder conventions (SPARQL variable names starting with {@code _}):
 * <ul>
 *   <li>{@code ?_x} &mdash; mandatory placeholder, substituted as a literal</li>
 *   <li>{@code ?__x} &mdash; optional (leading double underscore)</li>
 *   <li>{@code ?_x_iri} &mdash; substituted as an IRI</li>
 *   <li>{@code ?_x_multi}, {@code ?_x_multi_iri} or {@code ?_x_multi_val} &mdash; substituted
 *       inside a {@code VALUES ?_x_multi { }} block, allowing multiple values</li>
 * </ul>
 */
public class QueryTemplate implements Serializable {

    private static final String ID_REGEX = "RA[A-Za-z0-9\\-_]{43}[/#][^/#]+";
    private static final String URI_REGEX = "https?://.*[^A-Za-z0-9\\-_]" + ID_REGEX;

    private final String queryId;
    private final String artifactCode;
    private final String querySuffix;
    private final Nanopub nanopub;
    private final IRI queryUri;
    private final String sparql;
    private final IRI endpoint;
    private final String label;
    private final String description;
    private final IRI license;
    private final List<String> placeholdersList;
    private final boolean constructQuery;

    /**
     * Loads a query template by its identifier or URI, fetching the underlying
     * nanopublication via {@link GetNanopub#get(String)}.
     *
     * @param idOrUri a query ID ({@code RA.../suffix}), a full query URI, or a
     *                nanopub trusty URI containing a single grlc-query
     * @throws IllegalArgumentException if the identifier is invalid or the
     *                                  nanopub does not contain a query
     */
    public QueryTemplate(String idOrUri) {
        this(null, idOrUri);
    }

    /**
     * Builds a query template from a pre-fetched nanopublication that contains
     * exactly one grlc-query (identified via {@code rdf:type kpxl_grlc:grlc-query}).
     *
     * @param nanopub the nanopublication
     * @throws IllegalArgumentException if no query or more than one query is found
     */
    public QueryTemplate(Nanopub nanopub) {
        this(nanopub, null);
    }

    /**
     * Builds a query template from a pre-fetched nanopublication and an explicit
     * identifier. Use this form when a nanopub contains multiple queries or when
     * the caller wants to avoid the network fetch in {@link #QueryTemplate(String)}.
     *
     * @param nanopub  the nanopublication (must not be null if {@code idOrUri} is null)
     * @param idOrUri  a query ID, query URI, or nanopub URI; may be null if the
     *                 nanopub contains exactly one grlc-query
     * @throws IllegalArgumentException on any parsing or lookup failure
     */
    public QueryTemplate(Nanopub nanopub, String idOrUri) {
        String parsedQueryId = null;
        String parsedArtifactCode = null;

        if (idOrUri != null) {
            if (idOrUri.matches(URI_REGEX)) {
                parsedQueryId = idOrUri.replaceFirst("^.*?(" + ID_REGEX + ")$", "$1").replace("#", "/");
                parsedArtifactCode = parsedQueryId.replaceFirst("/.*$", "");
            } else if (idOrUri.matches(ID_REGEX)) {
                parsedQueryId = idOrUri.replace("#", "/");
                parsedArtifactCode = parsedQueryId.replaceFirst("/.*$", "");
            } else if (TrustyUriUtils.isPotentialTrustyUri(idOrUri)) {
                parsedArtifactCode = TrustyUriUtils.getArtifactCode(idOrUri);
            } else {
                throw new IllegalArgumentException("Not a valid query ID or URI: " + idOrUri);
            }
        } else if (nanopub == null) {
            throw new IllegalArgumentException("Either nanopub or idOrUri must be provided");
        }

        if (nanopub == null) {
            nanopub = GetNanopub.get(parsedArtifactCode);
            if (nanopub == null) {
                throw new IllegalArgumentException("Could not fetch nanopublication: " + parsedArtifactCode);
            }
        }
        if (parsedArtifactCode == null) {
            parsedArtifactCode = TrustyUriUtils.getArtifactCode(nanopub.getUri().stringValue());
        }
        this.nanopub = nanopub;
        this.artifactCode = parsedArtifactCode;

        IRI foundQueryUri = null;
        if (parsedQueryId == null) {
            for (Statement st : nanopub.getAssertion()) {
                if (st.getPredicate().equals(RDF.TYPE) && st.getObject().equals(KPXL_GRLC.GRLC_QUERY)) {
                    if (foundQueryUri != null) {
                        throw new IllegalArgumentException("Nanopublication defines more than one query: " + nanopub.getUri());
                    }
                    foundQueryUri = (IRI) st.getSubject();
                }
            }
            if (foundQueryUri == null) {
                throw new IllegalArgumentException("No query found in nanopublication: " + nanopub.getUri());
            }
            String uriStr = foundQueryUri.stringValue();
            if (!uriStr.matches(URI_REGEX) && !uriStr.matches(ID_REGEX)) {
                throw new IllegalArgumentException("Query URI does not match expected pattern: " + uriStr);
            }
            parsedQueryId = uriStr.replaceFirst("^.*?(" + ID_REGEX + ")$", "$1").replace("#", "/");
        } else {
            for (Statement st : nanopub.getAssertion()) {
                if (st.getSubject().stringValue().replace("#", "/").endsWith(parsedQueryId)) {
                    foundQueryUri = (IRI) st.getSubject();
                    break;
                }
            }
            if (foundQueryUri == null) {
                throw new IllegalArgumentException("No query with id " + parsedQueryId + " in nanopublication: " + nanopub.getUri());
            }
        }
        this.queryUri = foundQueryUri;
        this.queryId = parsedQueryId;
        this.querySuffix = parsedQueryId.replaceFirst("^.*/", "");

        String foundSparql = null;
        IRI foundEndpoint = null;
        String foundLabel = null;
        String foundDescription = null;
        IRI foundLicense = null;
        for (Statement st : nanopub.getAssertion()) {
            if (!st.getSubject().equals(foundQueryUri)) continue;
            IRI p = st.getPredicate();
            if (p.equals(KPXL_GRLC.SPARQL) && st.getObject() instanceof Literal lit) {
                foundSparql = lit.stringValue();
            } else if (p.equals(KPXL_GRLC.ENDPOINT) && st.getObject() instanceof IRI iri) {
                foundEndpoint = iri;
            } else if (p.equals(RDFS.LABEL)) {
                foundLabel = st.getObject().stringValue();
            } else if (p.equals(DCTERMS.DESCRIPTION)) {
                foundDescription = st.getObject().stringValue();
            } else if (p.equals(DCTERMS.LICENSE) && st.getObject() instanceof IRI lic) {
                foundLicense = lic;
            }
        }
        if (foundSparql == null) {
            throw new IllegalArgumentException("Query has no SPARQL string: " + parsedQueryId);
        }
        this.sparql = foundSparql;
        this.endpoint = foundEndpoint;
        this.label = foundLabel;
        this.description = foundDescription;
        this.license = foundLicense;

        ParsedQuery parsed;
        try {
            parsed = new SPARQLParser().parseQuery(foundSparql, null);
        } catch (MalformedQueryException ex) {
            throw new IllegalArgumentException("Invalid SPARQL in query " + parsedQueryId + ": " + ex.getMessage(), ex);
        }
        this.constructQuery = parsed instanceof ParsedGraphQuery;

        final Set<String> placeholders = new HashSet<>();
        parsed.getTupleExpr().visitChildren(new AbstractSimpleQueryModelVisitor<RuntimeException>() {
            @Override
            public void meet(Var node) {
                super.meet(node);
                if (!node.isConstant() && !node.isAnonymous() && node.getName().startsWith("_")) {
                    placeholders.add(node.getName());
                }
            }
        });
        List<String> sorted = new ArrayList<>(placeholders);
        Collections.sort(sorted);
        this.placeholdersList = Collections.unmodifiableList(sorted);
    }

    /** @return the query identifier, of the form {@code RA.../querySuffix} */
    public String getQueryId() {
        return queryId;
    }

    /** @return the artifact code of the containing nanopublication */
    public String getArtifactCode() {
        return artifactCode;
    }

    /** @return the query suffix (the part after the artifact code) */
    public String getQuerySuffix() {
        return querySuffix;
    }

    /** @return the nanopublication this template was parsed from */
    public Nanopub getNanopub() {
        return nanopub;
    }

    /** @return the full IRI of the query within the nanopublication */
    public IRI getQueryUri() {
        return queryUri;
    }

    /** @return the SPARQL template string (with {@code ?_xxx} placeholders) */
    public String getSparql() {
        return sparql;
    }

    /** @return the target SPARQL endpoint, or null if not specified */
    public IRI getEndpoint() {
        return endpoint;
    }

    /** @return the {@code rdfs:label} of the query, or null if not specified */
    public String getLabel() {
        return label;
    }

    /** @return the {@code dcterms:description} of the query, or null if not specified */
    public String getDescription() {
        return description;
    }

    /** @return the {@code dcterms:license} IRI of the query, or null if not specified */
    public IRI getLicense() {
        return license;
    }

    /** @return the sorted, immutable list of placeholder names (without the leading {@code ?}) */
    public List<String> getPlaceholdersList() {
        return placeholdersList;
    }

    /** @return true if the query is a SPARQL {@code CONSTRUCT} (RDF graph result) */
    public boolean isConstructQuery() {
        return constructQuery;
    }

    /**
     * Expands the SPARQL template by substituting placeholder values.
     * Equivalent to {@code expandQuery(params, true)}.
     *
     * @param params a map from simplified parameter name (see {@link #getParamName(String)})
     *               to a list of values; for single-valued placeholders only the first
     *               value is used
     * @return the expanded SPARQL query
     * @throws IllegalArgumentException if any non-optional placeholder is missing a value
     */
    public String expandQuery(Map<String, ? extends List<String>> params) {
        return expandQuery(params, true);
    }

    /**
     * Expands the SPARQL template by substituting placeholder values.
     *
     * <p>In {@code strict} mode, a missing value for a non-optional placeholder causes an
     * {@link IllegalArgumentException}. In non-strict mode, missing placeholders are left
     * in place &mdash; useful for producing a partially expanded query that a UI can
     * complete (e.g. a Yasgui link).
     *
     * @param params a map from simplified parameter name (see {@link #getParamName(String)})
     *               to a list of values
     * @param strict whether missing mandatory placeholders should throw
     * @return the expanded SPARQL query
     */
    public String expandQuery(Map<String, ? extends List<String>> params, boolean strict) {
        String expanded = sparql;
        for (String ph : placeholdersList) {
            String paramName = getParamName(ph);
            List<String> values = params.get(paramName);
            boolean missing = values == null || values.isEmpty();
            if (missing) {
                if (strict && !isOptionalPlaceholder(ph)) {
                    throw new IllegalArgumentException("Missing value for non-optional placeholder: " + ph);
                }
                if (isMultiPlaceholder(ph)) {
                    expanded = expanded.replaceAll("values\\s*\\?" + ph + "\\s*\\{\\s*\\}(\\s*\\.)?", "");
                }
                continue;
            }
            if (isMultiPlaceholder(ph)) {
                StringBuilder valueList = new StringBuilder();
                for (String v : values) {
                    valueList.append(isIriPlaceholder(ph) ? serializeIri(v) : serializeLiteral(v)).append(" ");
                }
                expanded = expanded.replaceAll(
                        "values\\s*\\?" + ph + "\\s*\\{\\s*\\}",
                        "values ?" + ph + " { " + escapeSlashes(valueList.toString()) + "}"
                );
            } else {
                String serialized = isIriPlaceholder(ph) ? serializeIri(values.get(0)) : serializeLiteral(values.get(0));
                expanded = expanded.replaceAll(
                        "\\?" + ph + "(?![A-Za-z0-9_])",
                        escapeSlashes(serialized)
                );
            }
        }
        return expanded;
    }

    /**
     * @param placeholder a placeholder name (without the leading {@code ?})
     * @return true if optional (starts with {@code __})
     */
    public static boolean isOptionalPlaceholder(String placeholder) {
        return placeholder.startsWith("__");
    }

    /**
     * @param placeholder a placeholder name
     * @return true if multi-valued (ends with {@code _multi}, {@code _multi_iri} or {@code _multi_val})
     */
    public static boolean isMultiPlaceholder(String placeholder) {
        return placeholder.endsWith("_multi") || placeholder.endsWith("_multi_iri") || placeholder.endsWith("_multi_val");
    }

    /**
     * @param placeholder a placeholder name
     * @return true if the value should be serialized as an IRI (ends with {@code _iri})
     */
    public static boolean isIriPlaceholder(String placeholder) {
        return placeholder.endsWith("_iri");
    }

    /**
     * Strips the placeholder conventions (leading underscores and the
     * {@code _multi_val}/{@code _iri}/{@code _multi} suffixes) to yield the simplified
     * parameter name used as a lookup key in {@link #expandQuery(Map)}.
     */
    public static String getParamName(String placeholder) {
        return placeholder.replaceFirst("^_+", "").replaceFirst("_multi_val$", "").replaceFirst("_iri$", "").replaceFirst("_multi$", "");
    }

    /** Serializes a string as a SPARQL IRI ({@code <iri>}). */
    public static String serializeIri(String iri) {
        return "<" + iri + ">";
    }

    /** Serializes a string as a SPARQL literal ({@code "literal"}), escaping as needed. */
    public static String serializeLiteral(String literal) {
        return "\"" + escapeLiteral(literal) + "\"";
    }

    /** Escapes backslashes, newlines and double quotes for inclusion in a SPARQL literal. */
    public static String escapeLiteral(String s) {
        return s.replace("\\", "\\\\").replace("\n", "\\n").replace("\"", "\\\"");
    }

    private static String escapeSlashes(String s) {
        return s.replace("\\", "\\\\");
    }

}
