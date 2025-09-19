package org.nanopub.extra.services;

import java.io.Serializable;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.codec.Charsets;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

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

}
