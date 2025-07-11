package org.nanopub.extra.services;

import java.io.Serializable;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Represents a single entry in an API response.
 */
public class ApiResponseEntry implements Serializable {

    private Map<String, String> data = new HashMap<>();

    /**
     * Default constructor.
     */
    public ApiResponseEntry() {
    }

    /**
     * Constructor to initialize with a key-value pair.
     *
     * @param key   the key for the entry
     * @param value the value for the entry
     */
    public void add(String key, String value) {
        data.put(key, value);
    }

    /**
     * Retrieves the value associated with the specified key.
     *
     * @param key the key to look up
     * @return the value associated with the key, or null if not found
     */
    public String get(String key) {
        return data.get(key);
    }

    /**
     * Retrieves the value associated with the specified key as a boolean.
     *
     * @param key the key to look up
     * @return true if the value is "1" or "true", false otherwise
     */
    public boolean getAsBoolean(String key) {
        String v = data.get(key);
        return v.equals("1") || v.equals("true");
    }

    /**
     * Retrieves the keys present in this entry.
     *
     * @return a set of keys in this entry
     */
    public Set<String> getKeys() {
        return data.keySet();
    }

    /**
     * Comparator for ApiResponseEntry objects.
     */
    public static class DataComparator implements Comparator<ApiResponseEntry> {

        /**
         * Compares two ApiResponseEntry objects based on their "date" field.
         *
         * @param e1 the first object to be compared.
         * @param e2 the second object to be compared.
         * @return a negative integer, zero, or a positive integer as the first argument is less than,
         */
        public int compare(ApiResponseEntry e1, ApiResponseEntry e2) {
            String d1 = e1.get("date");
            String d2 = e2.get("date");
            if (d1 == null && d2 == null) return 0;
            if (d1 == null) return 1;
            if (d2 == null) return -1;
            return d2.compareTo(d1);
        }

    }

}
