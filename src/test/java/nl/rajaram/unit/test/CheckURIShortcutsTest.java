package nl.rajaram.unit.test;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

import ch.tkuhn.nanopub.MalformedNanopubException;
import nl.rajaram.nanopub.CheckURI;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Rajaram
 * @since 03-10-2013
 * @version 1.0
 */
public class CheckURIShortcutsTest {
   
    public CheckURIShortcutsTest() {
    }
    /**
     * <p>
     * This URI has missing "www.", so this test is expected to throw
     * MalformedNanopubException exception. 
     * </p>
     * @throws MalformedNanopubException 
     */
    @Test(expected = MalformedNanopubException.class)
    public void uriShortcutsInvalid() throws MalformedNanopubException {
        
        String uri = "http://biosemantics.org#example";       
        CheckURI.checkShortcuts(uri);
    }
    /**
     * <p>
     * This URI is valid, so the test is not expected to throw
     * MalformedNanopubException exception.
     * </p>
     * @throws MalformedNanopubException 
     */
    @Test
    public void uriShortcutsValidHtpp() throws MalformedNanopubException {
        
        String uri = "http://www.biosemantics.org#example";       
        try {
            CheckURI.checkShortcuts(uri);
        }
        catch (MalformedNanopubException e) {
            fail("This is a valid uri : "+uri);
        }
    }
    
    /**
     * <p>
     * This URI is valid, so the test is not expected to throw
     * MalformedNanopubException exception.
     * </p>
     * @throws MalformedNanopubException 
     */
    @Test
    public void uriShortcutsValidFtp() throws MalformedNanopubException {
        
        String uri = "ftp://ftp.is.co.za/rfc/rfc1808.txt";       
        try {
            CheckURI.checkShortcuts(uri);
        }
        catch (MalformedNanopubException e) {
            fail("This is a valid uri : "+uri);
        }
    }
    
    /**
     * <p>
     * This URI is valid, so the test is not expected to throw
     * MalformedNanopubException exception.
     * </p>
     * @throws MalformedNanopubException 
     */
    @Test
    public void uriShortcutsValidLdap() throws MalformedNanopubException {
        
        String uri = "ldap://[2001:db8::7]/c=GB?objectClass?one";       
        try {
            CheckURI.checkShortcuts(uri);
        }
        catch (MalformedNanopubException e) {
            fail("This is a valid uri : "+uri);
        }
    }
    
    /**
     * <p>
     * This URI is valid URN, so the test is not expected to throw
     * MalformedNanopubException exception.
     * </p>
     * @throws MalformedNanopubException 
     */
    @Test
    public void uriShortcutsValidUrn() throws MalformedNanopubException {
        
        String uri = "urn:oasis:names:specification:docbook:dtd:xml:4.1.2";       
        try {
            CheckURI.checkShortcuts(uri);
        }
        catch (MalformedNanopubException e) {
            fail("This is a valid uri : "+uri);
        }
    }
    
    /**
     * <p>
     * This URI is valid, so the test is not expected to throw
     * MalformedNanopubException exception.
     * </p>
     * @throws MalformedNanopubException 
     */
    @Test
    public void uriShortcutsValidMailTo() throws MalformedNanopubException {
        
        String uri = "mailto:John.Doe@example.com";       
        try {
            CheckURI.checkShortcuts(uri);
        }
        catch (MalformedNanopubException e) {
            fail("This is a valid uri : "+uri);
        }
    }
    
    /**
     * <p>
     * This URI is valid, so the test is not expected to throw
     * MalformedNanopubException exception.
     * </p>
     * @throws MalformedNanopubException 
     */
    @Test
    public void uriShortcutsValidNews() throws MalformedNanopubException {
        
        String uri = "news:comp.infosystems.www.servers.unix";       
        try {
            CheckURI.checkShortcuts(uri);
        }
        catch (MalformedNanopubException e) {
            fail("This is a valid uri : "+uri);
        }
    }
}
