package org.nanopub.extra.services;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Represents a response from an API service, containing a header and a list of entries.
 */
public class ApiResponse implements Serializable {

    private String[] header;
    private List<ApiResponseEntry> data = new ArrayList<>();

    /**
     * Default constructor for ApiResponse.
     * Initializes an empty response with no header and no data entries.
     */
    public ApiResponse() {
    }

    /**
     * Constructs an ApiResponse with specified entries to initialize the response.
     *
     * @param entries a collection of ApiResponseEntry objects to initialize the response with.
     */
    public ApiResponse(Collection<ApiResponseEntry> entries) {
        data.addAll(entries);
    }

    /**
     * Sets the header for the API response.
     *
     * @param header an array of strings representing the header of the response.
     */
    public void setHeader(String[] header) {
        this.header = header;
    }

    /**
     * Adds a single ApiResponseEntry to the data list of the API response.
     *
     * @param entry the ApiResponseEntry to be added to the response.
     */
    public void add(ApiResponseEntry entry) {
        data.add(entry);
    }

    /**
     * Adds a new entry to the API response using an array of strings.
     *
     * @param line an array of strings representing a single entry in the response.
     */
    public void add(String[] line) {
        ApiResponseEntry entry = new ApiResponseEntry();
        for (int i = 0; i < line.length; i++) {
            entry.add(header[i], line[i]);
        }
        data.add(entry);
    }

    /**
     * Returns the header of the API response.
     *
     * @return an array of strings representing the header of the response.
     */
    public String[] getHeader() {
        return header;
    }

    /**
     * Returns the list of data entries in the API response.
     *
     * @return a list of ApiResponseEntry objects representing the data in the response.
     */
    public List<ApiResponseEntry> getData() {
        return data;
    }

    /**
     * Returns the number of entries in the API response.
     *
     * @return the size of the data list, representing the number of entries in the response.
     */
    public int size() {
        return data.size();
    }

}
