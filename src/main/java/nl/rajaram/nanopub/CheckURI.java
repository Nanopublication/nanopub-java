/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.rajaram.nanopub;

import ch.tkuhn.nanopub.MalformedNanopubException;

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
     * Invalid URI      :   http://.biosemantics.org#example
     * ---------------------------------------------------------
     * @param utf8 Nanopublication in string object format.
     * @throws MalformedNanopubException Throws exception if the URI is invalid.
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
    
}
