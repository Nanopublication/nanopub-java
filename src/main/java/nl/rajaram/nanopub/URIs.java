/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.rajaram.nanopub;

import org.openrdf.model.URI;

/**
 * <p>
 * URI's and its other parameters. 
 * </p>
 * @author Rajaram
 * @since 03-10-2013
 * @version 1.0
 */
public class URIs {
    // URI
    private URI uri;
    // Name for the URI.(Example: nanopublication, assertion)
    private String uriName;
    
    public URIs(URI uri, String uriName) {
        this.uri = uri;
        this.uriName = uriName;
    }

    /**
     * @return the uri
     */
    public URI getUri() {
        return uri;
    }

    /**
     * @param uri the uri to set
     */
    public void setUri(URI uri) {
        this.uri = uri;
    }

    /**
     * @return the uriName
     */
    public String getUriName() {
        return uriName;
    }

    /**
     * @param uriName the uriName to set
     */
    public void setUriName(String uriName) {
        this.uriName = uriName;
    }
}
