/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.rajaram.unit.test;

import ch.tkuhn.nanopub.MalformedNanopubException;
import java.util.ArrayList;
import java.util.List;
import nl.rajaram.nanopub.CheckURI;
import nl.rajaram.nanopub.URIs;
import org.junit.Test;
import static org.junit.Assert.*;
import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;

/**
 *
 * @author rkaliyaperumal
 */
public class CheckURIDuplicatesTest {
    
    public CheckURIDuplicatesTest() {
    }
    /**
     * <p>
     * Base and nanopublication URIs are same, so this test is excepted to 
     * throws MalformedNanopubException exception.
     * </p>
     * @throws MalformedNanopubException 
     */
    @Test(expected = MalformedNanopubException.class)
    public void uriDuplicates () throws MalformedNanopubException {
        List <URIs> uris = new ArrayList<URIs>();
        URI uri = new URIImpl("http://biosemantics.org/example/00001");
        String uriName = "Base uri";        
        uris.add(new URIs(uri, uriName));
        
        uri = new URIImpl("http://biosemantics.org/example/00001");
        uriName = "Nanopublication uri";        
        uris.add(new URIs(uri, uriName));
        
        uri = new URIImpl("http://biosemantics.org/example/00001#assertion");
        uriName = "Assertion uri";        
        uris.add(new URIs(uri, uriName));
        
        CheckURI.checkDuplicates(uris);
        
    }
    
    /**
     * <p>
     * No duplicates in the URIs list, so this test is not excepted to throw
     * MalformedNanopubException exception.
     * </p>
     */
    @Test
    public void uriNoDuplicates () {
        List <URIs> uris = new ArrayList<URIs>();
        URI uri = new URIImpl("http://biosemantics.org/example/");
        String uriName = "Base uri";        
        uris.add(new URIs(uri, uriName));
        
        uri = new URIImpl("http://biosemantics.org/example/00001");
        uriName = "Nanopublication uri";        
        uris.add(new URIs(uri, uriName));
        
        uri = new URIImpl("http://biosemantics.org/example/00001#assertion");
        uriName = "Assertion uri";        
        uris.add(new URIs(uri, uriName));
        
        try {
            CheckURI.checkDuplicates(uris);
        }
        catch (MalformedNanopubException e) {
            fail("No duplicates in the URIs list");
        }
        
        
    }
}