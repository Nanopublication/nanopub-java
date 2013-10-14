/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.rajaram.unit.test;

import ch.tkuhn.nanopub.MalformedNanopubException;
import nl.rajaram.nanopub.CheckURI;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Rajaram
 * @since 08-10-2013
 * @version 1.0
 */
public class CheckPrefixsTest {
    
    /**
     * <p>
     * This prefix is not ending with  '#' or  '/', so this test is expected 
     * to throw MalformedNanopubException exception. 
     * </p>
     * @throws MalformedNanopubException 
     */
    @Test(expected = MalformedNanopubException.class)
    public void prefixInvalid() throws MalformedNanopubException {
        
        String prefix = "@prefix np: <http://www.nanopub.org/nschema>";       
        CheckURI.checkPrefix(prefix);
    }
    
    /**
     * <p>
     * This prefix is ending with  '#', so this test is not expected 
     * to throw MalformedNanopubException exception. 
     * </p>
     */
    @Test
    public void prefixValid1() throws MalformedNanopubException {
        
        String prefix = "@prefix np: <http://www.nanopub.org/nschema#>";
        try{
        CheckURI.checkPrefix(prefix);
        }
        catch (MalformedNanopubException e) {
            fail("This is a valid prefix : "+prefix);
        }
    }
    
    /**
     * <p>
     * This prefix is ending with  '/', so this test is not expected 
     * to throw MalformedNanopubException exception. 
     * </p>
     */
    @Test
    public void prefixValid2() throws MalformedNanopubException {
        
        String prefix = "@prefix np: <http://www.nanopub.org/nschema/>";
        try {
        CheckURI.checkPrefix(prefix);
        }
        catch (MalformedNanopubException e) {
            fail("This is a valid prefix : "+prefix);
        }
    }
}