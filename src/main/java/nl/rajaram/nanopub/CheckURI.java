/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.rajaram.nanopub;

import ch.tkuhn.nanopub.MalformedNanopubException;
import java.util.List;
import org.openrdf.model.URI;

/**
 * <p>
 * Checks the uri's of the nanopublication.
 * </p>
 * 
 * @author Rajaram
 * @since 03-10-2013
 * @version 1.0
 */
public class CheckURI {
    
    /**
     * Check for the shortcuts in URI's. 
     * 
     * Example
     * ---------------------------------------------------------
     * Valid URI        :   http://www.biosemantics.org#example
     * Invalid URI      :   http://biosemantics.org#example
     * ---------------------------------------------------------
     * @param utf8  Nanopublication in string object format.
     * @throws MalformedNanopubException    Throws exception if the URI is 
     *                                      invalid.
     */
    public static void checkShortcuts (String utf8) throws 
            MalformedNanopubException {
        String [] lines = utf8.split("\\n");
        for (String line:lines) {
            if (line.contains("http://") && !line.contains("www.")) {
                throw new MalformedNanopubException("URI shortcut on line : "
                        +line);
            }
        }
                
    }
    
    /**
     * Check duplicates in URI's. 
     * 
     * Example
     * ---------------------------------------------------------
     * Valid URIs :
     * ==========
     * Assertion URI    = http://www.biosemantics.org#example1
     * Provenance URI   = http://www.biosemantics.org#example2
     * 
     * Invalid URIs :
     * ============
     * Assertion URI    = http://www.biosemantics.org#example
     * Provenance URI   = http://www.biosemantics.org#example
     * ---------------------------------------------------------
     * @param URIs  List of graph URIs.
     * @throws MalformedNanopubException    Throws exception if same URI is 
     *                                      assigned to more than one graph.
     */
    public static void checkDuplicates (List<URIs> uris) 
            throws MalformedNanopubException {
        
        for (URIs uri1:uris) {
            URI uri1Link = uri1.getUri();
            String uri1Name = uri1.getUriName();
            for (URIs uri2:uris) {
                URI uri2Link = uri2.getUri();
                String uri2Name = uri2.getUriName().toLowerCase();
                if (uri1Link.equals(uri2Link) && !(uri1Name.equalsIgnoreCase(
                        uri2Name))) {
                    throw new MalformedNanopubException(uri1Name+" and "
                            +uri2Name+" are same");                            
                }
            }            
        }      
    }
    
}
