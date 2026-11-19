package org.nanopub.templates;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.nanopub.vocabulary.NTEMPLATE;

import java.io.Serializable;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * What is needed to make a nanopublication from templates: the templates, the values of
 * their placeholders, and who makes it. A {@link TemplateFiller} turns a request into a
 * nanopublication.
 * <p>
 * A request has the same parts as a link to the publishing form of Nanodash, and can be read
 * from and written as the parameters of such a link ({@link #fromParams(Map)},
 * {@link #fromUrl(String)}, {@link #fromJson(String)}):
 * <ul>
 *   <li>{@code template}, the template of the assertion; {@code prtemplate}, the template of
 *       the provenance; {@code pitemplate1}, {@code pitemplate2} and so on, templates of the
 *       publication info;</li>
 *   <li>{@code param_<name>}, the value of a placeholder of the assertion template;
 *       {@code prparam_<name>} and {@code piparam<N>_<name>} for the provenance template and
 *       for the publication info template of that number. See {@link TemplateValues} for the
 *       names;</li>
 *   <li>{@code supersede}, {@code override} or {@code derive}, the nanopublication the new one
 *       is a version of or is derived from (see {@link Mode});</li>
 *   <li>{@code target-namespace}, the namespace the nanopublication gets its URI in.</li>
 * </ul>
 * A form takes the rest from the person who is logged in and from the page it was opened on.
 * A request names it: {@code creator}, the IRI of the creator, with {@code creator-name};
 * and {@code space} and {@code namespace}, the IRIs that the tokens {@code ~~SPACE~~} and
 * {@code ~~NAMESPACE~~} in the prefixes of a template stand for.
 * <p>
 * Unlike the form, a request has to give all values: nothing is read from the
 * nanopublication that is superseded, overridden or derived from, except what
 * {@link Mode} says is kept.
 */
public class FillRequest implements Serializable {

    /**
     * How the nanopublication that is made relates to an existing one.
     */
    public enum Mode {

        /**
         * A new version of the same thing: the nanopublication is linked with
         * {@code npx:supersedes}, and the resources it introduces and its root definition are
         * kept.
         */
        SUPERSEDE("supersede"),

        /**
         * Another definition of the same thing: the nanopublication is linked with
         * {@code prov:wasDerivedFrom}, and the resources it introduces and its root
         * definition are kept.
         */
        OVERRIDE("override"),

        /**
         * A new thing made from an existing one: the nanopublication is linked with
         * {@code prov:wasDerivedFrom}, its resources are minted anew, and it is its own root
         * definition.
         */
        DERIVE("derive");

        private final String param;

        Mode(String param) {
            this.param = param;
        }

        /**
         * @return the name of the parameter that names the existing nanopublication
         */
        public String getParam() {
            return param;
        }

    }

    private static final Pattern PUBINFO_TEMPLATE_PARAM = Pattern.compile("pitemplate([1-9][0-9]*)");
    private static final Pattern PUBINFO_VALUE_PARAM = Pattern.compile("piparam([1-9][0-9]*)_(.+)");

    // Parameters of the form that say nothing about the nanopublication to make.
    private static final Set<String> IGNORED_PARAMS = Set.of("link-message", "postpub-redirect-url", "prtemplate-options",
            "refresh-upon-publish", "part", "context", "locked", "locked-statements");

    // Parameters of the form that would change the nanopublication in a way a request cannot.
    private static final Map<String, String> UNSUPPORTED_PARAMS = Map.ofEntries(
            Map.entry("use", "values are not read from an existing nanopublication; give them as parameters"),
            Map.entry("use-a", "values are not read from an existing nanopublication; give them as parameters"),
            Map.entry("partial", "values are not read from an existing nanopublication; give them as parameters"),
            Map.entry("partial-a", "values are not read from an existing nanopublication; give them as parameters"),
            Map.entry("fill", "values are not read from an existing nanopublication; give them as parameters"),
            Map.entry("fill-all", "values are not read from an existing nanopublication; give them as parameters"),
            Map.entry("improve", "values are not read from an existing nanopublication; give them as parameters"),
            Map.entry("fill-query", "values are not read from a query; give them as parameters"),
            Map.entry("fill-query-mapping", "values are not read from a query; give them as parameters"),
            Map.entry("values-from-query", "values are not read from a query; give them as parameters"),
            Map.entry("values-from-query-mapping", "values are not read from a query; give them as parameters"));

    private final String template;
    private final TemplateValues values = new TemplateValues();
    private String provenanceTemplate;
    private final TemplateValues provenanceValues = new TemplateValues();
    private final Map<String, TemplateValues> pubinfo = new LinkedHashMap<>();
    private IRI creator;
    private String creatorName;
    private Mode mode;
    private String source;
    private String targetNamespace;
    private String space;
    private String namespace;
    private Date creationTime;

    /**
     * Creates a request for the given assertion template.
     *
     * @param template the ID of the assertion template, at the version to use
     */
    public FillRequest(String template) {
        if (template == null || template.isBlank()) throw new IllegalArgumentException("No template given");
        this.template = template;
    }

    /**
     * @return the ID of the assertion template
     */
    public String getTemplate() {
        return template;
    }

    /**
     * @return the values for the placeholders of the assertion template, to add to
     */
    public TemplateValues getValues() {
        return values;
    }

    /**
     * Sets the value of a placeholder of the assertion template.
     *
     * @param name the name of the placeholder, with a {@code __N} suffix for a repetition
     * @param text the text of the value
     * @return this object
     */
    public FillRequest set(String name, String text) {
        values.set(name, text);
        return this;
    }

    /**
     * Sets the template of the provenance. Without one, the template that the assertion
     * template names as its default is used, or else
     * {@link TemplateFiller#DEFAULT_PROVENANCE_TEMPLATE}.
     *
     * @param provenanceTemplate the ID of the provenance template, or null for the default
     * @return this object
     */
    public FillRequest setProvenanceTemplate(String provenanceTemplate) {
        this.provenanceTemplate = provenanceTemplate;
        return this;
    }

    /**
     * @return the ID of the provenance template, or null if the default is to be used
     */
    public String getProvenanceTemplate() {
        return provenanceTemplate;
    }

    /**
     * @return the values for the placeholders of the provenance template, to add to
     */
    public TemplateValues getProvenanceValues() {
        return provenanceValues;
    }

    /**
     * Adds a template of the publication info. The templates a nanopublication always gets
     * need not be added: see {@link TemplateFiller}. Adding one of them, at this or another
     * version, is how values are given for it.
     *
     * @param pubinfoTemplate the ID of the publication info template
     * @return the values for the placeholders of that template, to add to
     */
    public TemplateValues addPubinfoTemplate(String pubinfoTemplate) {
        if (pubinfoTemplate == null || pubinfoTemplate.isBlank()) throw new IllegalArgumentException("No template given");
        return pubinfo.computeIfAbsent(pubinfoTemplate, k -> new TemplateValues());
    }

    /**
     * @return the IDs of the publication info templates that were added, in that order
     */
    public List<String> getPubinfoTemplates() {
        return List.copyOf(pubinfo.keySet());
    }

    /**
     * @param pubinfoTemplate the ID of a publication info template
     * @return the values for the placeholders of that template, or null if it was not added
     */
    public TemplateValues getPubinfoValues(String pubinfoTemplate) {
        return pubinfo.get(pubinfoTemplate);
    }

    /**
     * Sets the creator of the nanopublication, which {@code nt:CREATOR} stands for.
     *
     * @param creator the IRI of the creator
     * @return this object
     */
    public FillRequest setCreator(IRI creator) {
        this.creator = creator;
        return this;
    }

    /**
     * @return the IRI of the creator, or null if not given
     */
    public IRI getCreator() {
        return creator;
    }

    /**
     * Sets the name of the creator, which the publication info then states.
     *
     * @param creatorName the name, or null to leave it out
     * @return this object
     */
    public FillRequest setCreatorName(String creatorName) {
        this.creatorName = creatorName == null || creatorName.isBlank() ? null : creatorName;
        return this;
    }

    /**
     * @return the name of the creator, or null if not given
     */
    public String getCreatorName() {
        return creatorName;
    }

    /**
     * Makes the nanopublication a new version of an existing one, or one derived from it.
     *
     * @param mode   how the two relate
     * @param source the URI of the existing nanopublication
     * @return this object
     */
    public FillRequest setSource(Mode mode, String source) {
        if ((mode == null) != (source == null)) throw new IllegalArgumentException("A mode needs a nanopublication, and the other way round");
        this.mode = mode;
        this.source = source;
        return this;
    }

    /**
     * @return how the nanopublication relates to an existing one, or null if it does not
     */
    public Mode getMode() {
        return mode;
    }

    /**
     * @return the URI of the existing nanopublication, or null if there is none
     */
    public String getSource() {
        return source;
    }

    /**
     * Sets the namespace the nanopublication gets its URI in. Without one, the target
     * namespace of the assertion template is used, or else the namespace of the
     * nanopublication that is superseded, or else {@code https://w3id.org/np/}.
     *
     * @param targetNamespace the namespace, or null for the default
     * @return this object
     */
    public FillRequest setTargetNamespace(String targetNamespace) {
        this.targetNamespace = targetNamespace == null || targetNamespace.isBlank() ? null : targetNamespace;
        return this;
    }

    /**
     * @return the namespace the nanopublication gets its URI in, or null for the default
     */
    public String getTargetNamespace() {
        return targetNamespace;
    }

    /**
     * Sets the space the nanopublication is made for, which {@code ~~SPACE~~} stands for in
     * the prefixes of a template.
     *
     * @param space the IRI of the space, or null
     * @return this object
     */
    public FillRequest setSpace(String space) {
        this.space = space == null || space.isBlank() ? null : space;
        return this;
    }

    /**
     * @return the IRI of the space the nanopublication is made for, or null if not given
     */
    public String getSpace() {
        return space;
    }

    /**
     * Sets the namespace of the resource the nanopublication is made for, which
     * {@code ~~NAMESPACE~~} stands for in the prefixes of a template.
     *
     * @param namespace the namespace, ending in a slash or a hash, or null
     * @return this object
     */
    public FillRequest setNamespace(String namespace) {
        this.namespace = namespace == null || namespace.isBlank() ? null : namespace;
        return this;
    }

    /**
     * @return the namespace of the resource the nanopublication is made for, or null if not
     * given
     */
    public String getNamespace() {
        return namespace;
    }

    /**
     * Sets the time the nanopublication says it was created at. Without one, it is the time
     * at which the template is filled.
     *
     * @param creationTime the time, or null for the time of filling
     * @return this object
     */
    public FillRequest setCreationTime(Date creationTime) {
        this.creationTime = creationTime == null ? null : new Date(creationTime.getTime());
        return this;
    }

    /**
     * @return the time the nanopublication says it was created at, or null for the time of
     * filling
     */
    public Date getCreationTime() {
        return creationTime == null ? null : new Date(creationTime.getTime());
    }

    /**
     * Reads the request back out of a nanopublication that was checked against its templates:
     * the templates it names, the values of their placeholders, and its creator. Filling the
     * request gives the nanopublication again, as far as it is what its templates describe,
     * under a new URI and with a new creation time.
     * <p>
     * This is the way to make a new version of a nanopublication: read its request, change
     * values, and say with {@link #setSource(Mode, String)} what the new nanopublication is
     * to the existing one. The request as read has no such link.
     *
     * @param report the findings of a {@link TemplateComplianceChecker} for the nanopublication
     * @return the request
     * @throws IllegalArgumentException if the nanopublication names no template for its
     *                                  assertion or its provenance, or one that could not be
     *                                  retrieved
     */
    public static FillRequest of(ComplianceReport report) {
        Objects.requireNonNull(report, "report");
        TemplateMatch assertion = report.getAssertionMatch();
        TemplateMatch provenance = report.getProvenanceMatch();
        if (assertion == null) throw new IllegalArgumentException("The nanopublication was not matched against an assertion template");
        if (provenance == null) throw new IllegalArgumentException("The nanopublication was not matched against a provenance template");
        FillRequest request = new FillRequest(assertion.getTemplate().getId());
        TemplateValues.of(assertion).copyTo(request.values);
        request.setProvenanceTemplate(provenance.getTemplate().getId());
        TemplateValues.of(provenance).copyTo(request.provenanceValues);
        List<TemplateMatch> all = new ArrayList<>(List.of(assertion, provenance));
        for (TemplateMatch pubinfoMatch : report.getPubinfoMatches()) {
            TemplateValues.of(pubinfoMatch).copyTo(request.addPubinfoTemplate(pubinfoMatch.getTemplate().getId()));
            all.add(pubinfoMatch);
        }
        // The creator is the one the templates took as such, or else the only one there is.
        for (TemplateMatch match : all) {
            if (request.creator == null && match.getContext().get(NTEMPLATE.CREATOR_PLACEHOLDER) instanceof IRI creator) request.creator = creator;
        }
        Set<IRI> creators = assertion.getContext().getCreators();
        if (request.creator == null && creators.size() == 1) request.creator = creators.iterator().next();
        if (request.creator != null) {
            for (Statement st : report.getNanopub().getPubinfo()) {
                if (st.getSubject().equals(request.creator) && st.getPredicate().equals(FOAF.NAME)) request.setCreatorName(st.getObject().stringValue());
            }
        }
        return request;
    }

    // ---------- as parameters ----------

    /**
     * Returns the request as parameters, named as in a link to the publishing form of
     * Nanodash. The creation time is not among them.
     *
     * @return the parameters
     */
    public Map<String, String> toParams() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("template", template);
        values.toParams().forEach((name, text) -> params.put("param_" + name, text));
        if (provenanceTemplate != null) params.put("prtemplate", provenanceTemplate);
        provenanceValues.toParams().forEach((name, text) -> params.put("prparam_" + name, text));
        int n = 0;
        for (Map.Entry<String, TemplateValues> e : pubinfo.entrySet()) {
            String number = Integer.toString(++n);
            params.put("pitemplate" + number, e.getKey());
            e.getValue().toParams().forEach((name, text) -> params.put("piparam" + number + "_" + name, text));
        }
        if (mode != null) params.put(mode.getParam(), source);
        if (targetNamespace != null) params.put("target-namespace", targetNamespace);
        if (creator != null) params.put("creator", creator.stringValue());
        if (creatorName != null) params.put("creator-name", creatorName);
        if (space != null) params.put("space", space);
        if (namespace != null) params.put("namespace", namespace);
        return params;
    }

    /**
     * Reads a request from parameters, named as in a link to the publishing form of Nanodash.
     * Parameters of the form that say nothing about the nanopublication, such as
     * {@code link-message}, are ignored.
     *
     * @param params the parameters
     * @return the request
     * @throws TemplateFillException if a parameter is not known, or asks for something a
     *                               request cannot do, such as reading the values from an
     *                               existing nanopublication ({@code use})
     */
    public static FillRequest fromParams(Map<String, String> params) throws TemplateFillException {
        List<String> problems = new ArrayList<>();
        String template = params.get("template");
        if (template == null || template.isBlank()) throw new TemplateFillException("No template is given (parameter 'template')");
        FillRequest request = new FillRequest(template);
        Map<String, String> assertionParams = new LinkedHashMap<>();
        Map<String, String> provenanceParams = new LinkedHashMap<>();
        Map<Integer, String> pubinfoTemplates = new TreeMap<>();
        Map<Integer, Map<String, String>> pubinfoParams = new TreeMap<>();
        for (Map.Entry<String, String> e : params.entrySet()) {
            String key = e.getKey();
            String value = e.getValue() == null ? "" : e.getValue();
            Matcher pubinfoTemplate = PUBINFO_TEMPLATE_PARAM.matcher(key);
            Matcher pubinfoValue = PUBINFO_VALUE_PARAM.matcher(key);
            if (key.equals("template")) {
                continue;
            } else if (key.startsWith("param_") && key.length() > 6) {
                assertionParams.put(key.substring(6), value);
            } else if (key.startsWith("prparam_") && key.length() > 8) {
                provenanceParams.put(key.substring(8), value);
            } else if (pubinfoValue.matches()) {
                pubinfoParams.computeIfAbsent(Integer.parseInt(pubinfoValue.group(1)), k -> new LinkedHashMap<>()).put(pubinfoValue.group(2), value);
            } else if (value.isBlank()) {
                // A parameter of the request itself that has no value is one that is not given.
                continue;
            } else if (pubinfoTemplate.matches()) {
                pubinfoTemplates.put(Integer.parseInt(pubinfoTemplate.group(1)), value);
            } else if (key.equals("prtemplate")) {
                request.setProvenanceTemplate(value);
            } else if (getMode(key) != null) {
                if (request.mode != null) {
                    problems.add("Only one of 'supersede', 'override' and 'derive' can be given, not both '" + request.mode.getParam() + "' and '" + key + "'");
                } else {
                    request.setSource(getMode(key), value);
                }
            } else if (key.equals("target-namespace")) {
                request.setTargetNamespace(value);
            } else if (key.equals("creator")) {
                try {
                    request.setCreator(PlaceholderMatcher.createIri(value));
                } catch (IllegalArgumentException ex) {
                    problems.add("The creator is not an IRI: " + value);
                }
            } else if (key.equals("creator-name")) {
                request.setCreatorName(value);
            } else if (key.equals("space")) {
                request.setSpace(value);
            } else if (key.equals("namespace")) {
                request.setNamespace(value);
            } else if (key.equals("template-version")) {
                if (value.equals("latest")) {
                    problems.add("The parameter 'template-version=latest' is not supported: a template is used at exactly the version named");
                }
            } else if (UNSUPPORTED_PARAMS.containsKey(key)) {
                problems.add("The parameter '" + key + "' is not supported: " + UNSUPPORTED_PARAMS.get(key));
            } else if (!IGNORED_PARAMS.contains(key)) {
                problems.add("Unknown parameter '" + key + "'");
            }
        }
        TemplateValues.fromParams(assertionParams).copyTo(request.values);
        TemplateValues.fromParams(provenanceParams).copyTo(request.provenanceValues);
        Map<Integer, TemplateValues> pubinfoValues = new HashMap<>();
        for (Map.Entry<Integer, String> e : pubinfoTemplates.entrySet()) {
            pubinfoValues.put(e.getKey(), request.addPubinfoTemplate(e.getValue()));
        }
        for (Map.Entry<Integer, Map<String, String>> e : pubinfoParams.entrySet()) {
            TemplateValues target = pubinfoValues.get(e.getKey());
            if (target == null) {
                problems.add("There are values for the publication info template " + e.getKey() + " (piparam" + e.getKey()
                        + "_...), but no 'pitemplate" + e.getKey() + "' names that template");
            } else {
                TemplateValues.fromParams(e.getValue()).copyTo(target);
            }
        }
        if (!problems.isEmpty()) throw new TemplateFillException(problems);
        return request;
    }

    private static Mode getMode(String param) {
        for (Mode mode : Mode.values()) {
            // The "-a" form asks a form to read only the assertion from the existing
            // nanopublication, which makes no difference where nothing is read from it.
            if (param.equals(mode.getParam()) || param.equals(mode.getParam() + "-a")) return mode;
        }
        return null;
    }

    /**
     * Reads a request from a link to the publishing form of Nanodash, or from the query
     * string of such a link.
     *
     * @param url the link, or what follows its question mark
     * @return the request
     * @throws TemplateFillException if the parameters do not make a request, see
     *                               {@link #fromParams(Map)}
     */
    public static FillRequest fromUrl(String url) throws TemplateFillException {
        String query = Objects.requireNonNull(url, "url").trim();
        int fragment = query.indexOf('#');
        if (fragment >= 0) query = query.substring(0, fragment);
        int questionMark = query.indexOf('?');
        if (questionMark >= 0) query = query.substring(questionMark + 1);
        Map<String, String> params = new LinkedHashMap<>();
        List<String> problems = new ArrayList<>();
        for (String part : query.split("&")) {
            if (part.isEmpty()) continue;
            int equals = part.indexOf('=');
            String key, value;
            try {
                key = URLDecoder.decode(equals < 0 ? part : part.substring(0, equals), StandardCharsets.UTF_8);
                value = equals < 0 ? "" : URLDecoder.decode(part.substring(equals + 1), StandardCharsets.UTF_8);
            } catch (IllegalArgumentException ex) {
                problems.add("Not a properly encoded parameter: " + part);
                continue;
            }
            String before = params.put(key, value);
            if (before != null && !before.equals(value)) problems.add("The parameter '" + key + "' is given twice, with different values");
        }
        if (!problems.isEmpty()) throw new TemplateFillException(problems);
        return fromParams(params);
    }

    /**
     * Returns the request as the query string of a link to the publishing form of Nanodash.
     * The parameters that only a request has, such as {@code creator}, are among them; the
     * form ignores them.
     *
     * @return the parameters, encoded and joined with ampersands
     */
    public String toQueryString() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : toParams().entrySet()) {
            if (sb.length() > 0) sb.append('&');
            sb.append(URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8)).append('=').append(URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8));
        }
        return sb.toString();
    }

    /**
     * Reads a request from a JSON object that has the parameters as its members, with
     * strings as values. This spares long values, such as a query, the encoding a link needs.
     *
     * @param json the JSON object
     * @return the request
     * @throws TemplateFillException if this is not such an object, or the parameters do not
     *                               make a request, see {@link #fromParams(Map)}
     */
    public static FillRequest fromJson(String json) throws TemplateFillException {
        JsonElement root;
        try {
            root = JsonParser.parseString(Objects.requireNonNull(json, "json"));
        } catch (JsonParseException ex) {
            throw new TemplateFillException("Not valid JSON: " + ex.getMessage());
        }
        if (!root.isJsonObject()) throw new TemplateFillException("The JSON has to be an object with the parameters as its members");
        Map<String, String> params = new LinkedHashMap<>();
        List<String> problems = new ArrayList<>();
        for (Map.Entry<String, JsonElement> e : ((JsonObject) root).entrySet()) {
            JsonElement value = e.getValue();
            if (value.isJsonNull()) continue;
            if (!value.isJsonPrimitive()) {
                problems.add("The value of '" + e.getKey() + "' has to be a string; a further repetition has a name of its own, such as '" + e.getKey() + "__1'");
                continue;
            }
            params.put(e.getKey(), value.getAsString());
        }
        if (!problems.isEmpty()) throw new TemplateFillException(problems);
        return fromParams(params);
    }

    /**
     * Returns the request as a JSON object that has the parameters as its members.
     *
     * @return the JSON object, indented
     */
    public String toJson() {
        return new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(toParams());
    }

    /**
     * Reads a request from the text of a link, a query string or a JSON object, whichever it
     * is.
     *
     * @param text the text
     * @return the request
     * @throws TemplateFillException if the text does not make a request
     */
    public static FillRequest parse(String text) throws TemplateFillException {
        String trimmed = Objects.requireNonNull(text, "text").trim();
        return trimmed.startsWith("{") ? fromJson(trimmed) : fromUrl(trimmed);
    }

}
