/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.rajaram.nanopub;

import ch.tkuhn.nanopub.MalformedNanopubException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.openrdf.model.URI;

/**
 * <p>
 * Checks URI's of the nanopublication.
 * </p>
 * 
 * @author Rajaram
 * @since 03-10-2013
 * @version 1.0
 */
public class CheckURI {
        
    /**
     * <p>
     * Check for shortcuts in URI's. 
     * <br>
     * Example<br>
     * ---------------------------------------------------------<br>
     * Valid URI        :   http://www.biosemantics.org#example<br>
     * Invalid URI      :   http://biosemantics.org#example<br>
     * ---------------------------------------------------------<br>
     * </p>
     * @param utf8  Nanopublication in string object format.
     * @throws MalformedNanopubException    Throws exception if the URI is 
     *                                      invalid.
     */
    public static void checkShortcuts (String utf8) throws 
            MalformedNanopubException {
        String [] lines = utf8.split("\\n");
        for (String line:lines) {
            if (line.contains("http://") && !line.contains("www.")) {
                throw new MalformedNanopubException("URI shortcut on line ==> "
                        +line);
            }
        }
                
    }
    
    /**
     * <p>
     * Check duplicates in URI's.
     * <br>
     * Example<br>
     * ---------------------------------------------------------<br>
     * Valid URIs :<br>
     * ==========<br>
     * Assertion URI    = http://www.biosemantics.org#example1<br>
     * Provenance URI   = http://www.biosemantics.org#example2<br>
     * <br>
     * Invalid URIs :<br>
     * ============<br>
     * Assertion URI    = http://www.biosemantics.org#example<br>
     * Provenance URI   = http://www.biosemantics.org#example<br>
     * ---------------------------------------------------------<br>
     * </p>
     * @param URIs  List of graph URIs.
     * @throws MalformedNanopubException    Throws exception if the same URI is 
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
    
    /**
     * <p>
     * Check for errors in prefix's. 
     * 
     * Example
     * ---------------------------------------------------------
     * Valid prefix        :   @prefix <http://www.biosemantics.org#>
     * Invalid prefix      :   @prefix <http://www.biosemantics.org>
     * ---------------------------------------------------------
     * </p>
     * @param utf8  Nanopublication in string object format.
     * @throws MalformedNanopubException    Throws exception if the prefix is 
     *                                      invalid.
     */
    public static void checkPrefix (String utf8) throws 
            MalformedNanopubException {
        utf8 = utf8.toLowerCase();
        String [] lines = utf8.split("\\n");
        for (String line:lines) {
            if (line.contains("prefix") && line.contains("http://")) {
                // To take characters between '<' and '>'.
                Pattern logEntry = Pattern.compile("\\<(.*?)\\>");
                Matcher matchPattern = logEntry.matcher(line);

                while(matchPattern.find()) {
                    String link = matchPattern.group(1);
                    //Checking the last character in the link. 
                    if ((link.charAt(link.length()-1) == '#') ||
                            (link.charAt(link.length()-1) == '/')){
                        
                    }
                    else {
                        throw new MalformedNanopubException("Invalid prefix: "
                                + "The prefix link should end with either "
                                + "'#' or '/' ==> "+line);
                    }
                }                
            }
        }
                
    }
    
}
