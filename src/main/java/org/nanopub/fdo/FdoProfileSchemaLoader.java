package org.nanopub.fdo;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.nanopub.fdo.rest.HandleResolver;
import org.nanopub.fdo.rest.gson.ParsedJsonResponse;
import org.nanopub.fdo.rest.gson.Value;
import org.nanopub.vocabulary.HDL;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Loads a property-name → handle-IRI mapping for an FDO profile by resolving the profile handle
 * and fetching its entry from a Data Type Registry (DTR).
 *
 * <p>Two DTR JSON shapes are supported:</p>
 * <ul>
 *   <li>Array of <code>{"name": ..., "identifier": ...}</code> under <code>properties</code>
 *       (e.g. DiSSCo / PID Consortium DTR).</li>
 *   <li>Array of <code>{"Name": "&lt;handle&gt;", ...}</code> under <code>Schema.Properties</code>
 *       (e.g. GWDG testbed).</li>
 * </ul>
 *
 * <p>Resolution is best-effort: any failure along the way yields an empty map so the caller can
 * fall back to default predicate construction.</p>
 */
public final class FdoProfileSchemaLoader {

    private static final ValueFactory vf = SimpleValueFactory.getInstance();

    private static final Pattern JSON_LOC_HREF = Pattern.compile(
            "<location\\s+[^>]*href=\"([^\"]+)\"[^>]*view=\"json\"", Pattern.CASE_INSENSITIVE);

    private FdoProfileSchemaLoader() {
    }

    /**
     * Build a name → handle-IRI mapping for the given FDO profile.
     *
     * @param profileIri the profile IRI from the FdoRecord (may be a handle, handle URL, or DOI URL)
     * @return a possibly-empty map of property name → handle IRI; never null
     */
    public static Map<String, IRI> loadPropertyMap(IRI profileIri) {
        if (profileIri == null) return Map.of();
        String handleId = FdoUtils.extractHandleId(profileIri.stringValue());
        if (handleId == null) return Map.of();
        try {
            ParsedJsonResponse profile = new HandleResolver().call(handleId);
            String jsonLoc = findJsonLocation(profile);
            if (jsonLoc == null) return Map.of();
            String body = httpGet(jsonLoc);
            return parsePropertyMap(body);
        } catch (Exception ignore) {
            return Map.of();
        }
    }

    static String findJsonLocation(ParsedJsonResponse profile) {
        if (profile == null || profile.values == null) return null;
        for (Value v : profile.values) {
            if ("10320/loc".equals(v.type) && v.data != null && v.data.value != null) {
                Matcher m = JSON_LOC_HREF.matcher(String.valueOf(v.data.value));
                if (m.find()) return m.group(1);
            }
        }
        return null;
    }

    static String httpGet(String url) throws Exception {
        HttpRequest req = HttpRequest.newBuilder().GET().uri(new URI(url)).build();
        HttpResponse<String> resp = HttpClient.newHttpClient().send(req, HttpResponse.BodyHandlers.ofString());
        return resp.body();
    }

    static Map<String, IRI> parsePropertyMap(String json) {
        Map<String, IRI> out = new HashMap<>();
        JsonElement parsed = JsonParser.parseString(json);
        if (!parsed.isJsonObject()) return out;
        JsonObject root = parsed.getAsJsonObject();

        if (root.has("properties")) {
            JsonElement props = root.get("properties");
            // DTR array shape: [{"name": ..., "identifier": ...}]
            if (props.isJsonArray()) {
                for (JsonElement el : props.getAsJsonArray()) {
                    if (!el.isJsonObject()) continue;
                    JsonObject p = el.getAsJsonObject();
                    if (p.has("name") && p.has("identifier")) {
                        String name = p.get("name").getAsString();
                        String id = p.get("identifier").getAsString();
                        if (FdoUtils.looksLikeHandle(id)) {
                            IRI handleIri = vf.createIRI(HDL.NAMESPACE + id);
                            out.putIfAbsent(name, handleIri);
                            addTitleKey(out, p, handleIri);
                        }
                    }
                }
            }
            // JSON Schema object shape: {"<handle>": {title: "..."}}
            if (props.isJsonObject()) {
                for (Map.Entry<String, JsonElement> e : props.getAsJsonObject().entrySet()) {
                    String key = e.getKey();
                    if (!FdoUtils.looksLikeHandle(key)) continue;
                    IRI handleIri = vf.createIRI(HDL.NAMESPACE + key);
                    out.putIfAbsent(key, handleIri);
                    if (e.getValue().isJsonObject()) {
                        addTitleKey(out, e.getValue().getAsJsonObject(), handleIri);
                    }
                }
            }
        }

        if (root.has("Schema") && root.get("Schema").isJsonObject()) {
            JsonObject schema = root.getAsJsonObject("Schema");
            if (schema.has("Properties") && schema.get("Properties").isJsonArray()) {
                for (JsonElement el : schema.getAsJsonArray("Properties")) {
                    if (!el.isJsonObject()) continue;
                    JsonObject p = el.getAsJsonObject();
                    if (p.has("Name")) {
                        String name = p.get("Name").getAsString();
                        if (FdoUtils.looksLikeHandle(name)) {
                            IRI handleIri = vf.createIRI(HDL.NAMESPACE + name);
                            out.putIfAbsent(name, handleIri);
                            addTitleKey(out, p, handleIri);
                        }
                    }
                }
            }
        }

        return out;
    }

    private static void addTitleKey(Map<String, IRI> out, JsonObject p, IRI handleIri) {
        for (String field : new String[]{"Title", "title"}) {
            if (p.has(field) && p.get(field).isJsonPrimitive()) {
                String title = p.get(field).getAsString();
                if (!title.isEmpty()) out.putIfAbsent(title, handleIri);
            }
        }
    }

}
