package org.nanopub.extra.services;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.codec.Charsets;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import java.io.Serializable;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;

/**
 * A reference to a query with optional parameters.
 * This class is used to encapsulate the name of the query and any parameters
 * that need to be passed to it.
 */
public class QueryRef implements Serializable {

    private final String name;
    private final Multimap<String, String> params;
    private String urlString;

    /**
     * Constructor for QueryRef.
     *
     * @param name   the name of the query
     * @param params a map of parameters for the query
     */
    public QueryRef(String name, Multimap<String, String> params) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Query name cannot be null or empty");
        }
        this.name = name;
        this.params = params;
    }

    /**
     * Constructor for QueryRef with no parameters.
     *
     * @param name the name of the query
     */
    public QueryRef(String name) {
        this(name, ArrayListMultimap.create());
    }

    /**
     * Constructor for QueryRef with a single parameter.
     *
     * @param name       the name of the query
     * @param paramKey   the key of the parameter
     * @param paramValue the value of the parameter
     */
    public QueryRef(String name, String paramKey, String paramValue) {
        this(name);
        if (paramKey == null || paramKey.isBlank()) {
            throw new IllegalArgumentException("Parameter key cannot be null or empty");
        }
        params.put(paramKey, paramValue);
    }

    /**
     * Get the name of the query.
     *
     * @return the name of the query
     */
    public String getName() {
        return name;
    }

    /**
     * Get the parameters of the query.
     *
     * @return a map of parameters
     */
    public Multimap<String, String> getParams() {
        return params;
    }

    /**
     * Get the query reference as a URL string.
     *
     * @return the query reference as a URL string
     */
    public String getAsUrlString() {
        if (urlString == null) {
            String paramString = "";
            if (params != null) {
                paramString = "?";
                List<Entry<String, String>> entryList = new ArrayList<>(params.entries());
                entryList.sort(Comparator.comparing(Entry::getValue));
                entryList.sort(Comparator.comparing(Entry::getKey));
                for (Entry<String, String> e : entryList) {
                    if (paramString.length() > 1) paramString += "&";
                    paramString += (e.getKey() == null ? "$null" : e.getKey()) + "=";
                    paramString += URLEncoder.encode(e.getValue() == null ? "" : e.getValue(), Charsets.UTF_8);
                }
            }
            urlString = name + paramString;
        }
        return urlString;
    }

    @Override
    public String toString() {
        return getAsUrlString();
    }

    /**
     * Parse a QueryRef from a string.
     *
     * @param queryRefUrlString the string to parse
     * @return the parsed QueryRef
     */
    public static QueryRef parseString(String queryRefUrlString) {
        // TODO add check that the string is a valid one before parsing
        if (queryRefUrlString.contains("?")) {
            String queryName = queryRefUrlString.split("\\?")[0];
            Multimap<String, String> queryParams = ArrayListMultimap.create();
            if (!queryRefUrlString.endsWith("?")) {
                for (NameValuePair nvp : URLEncodedUtils.parse(queryRefUrlString.split("\\?")[1], Charsets.UTF_8)) {
                    queryParams.put(nvp.getName(), nvp.getValue());
                }
            }
            return new QueryRef(queryName, queryParams);
        } else {
            return new QueryRef(queryRefUrlString);
        }
    }

}
