/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.rajaram.unit.test;

import ch.tkuhn.nanopub.MalformedNanopubException;
import ch.tkuhn.nanopub.NanopubImpl;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import org.junit.Test;
import static org.junit.Assert.*;
import org.openrdf.OpenRDFException;

/**
 *
 * @author Rajaram
 * @since 14-10-2013
 * @version 1.0
 */
public class NanopubFromFile {
    
    public NanopubFromFile() {
    }
    
    /**
     * <p>
     * This is a valid nanopublication in trig format, so the test is not 
     * expected to throw exception.
     * </p>
     * @throws IOException 
     */
    @Test
    public void validNanopublication1() throws IOException{        
        // See package src/test/resources for the file.
        URL fileURL = this.getClass().
                getResource("/validNanopublication1.trig");
        File file = new File(fileURL.getPath());
        try {
            NanopubImpl test = new NanopubImpl(file);
        }
        catch (MalformedNanopubException | OpenRDFException | IOException e) {
            fail("This is valid nanopublication");
        }
    }
    
    /**
     * <p>
     * This is a valid nanopublication in n-quads format, so the test is not 
     * expected to throw exception.
     * </p>
     * @throws IOException 
     */
    @Test
    public void validNanopublication2() throws IOException{        
        // See package src/test/resources for the file.
        URL fileURL = this.getClass().getResource("/validNanopublication2.nq");
        File file = new File(fileURL.getPath());
        try {
            NanopubImpl test = new NanopubImpl(file);
        }
        catch (MalformedNanopubException | OpenRDFException | IOException e) {
            fail("This is valid nanopublication");
        } 
    }
    
    /**
     * <p>
     * This is a valid nanopublication in trix format, so the test is not 
     * expected to throw exception.
     * </p>
     * @throws IOException 
     */
    @Test
    public void validNanopublication3() throws IOException{        
        // See package src/test/resources for the file.
        URL fileURL = this.getClass().
                getResource("/validNanopublication3.trix");
        File file = new File(fileURL.getPath());
        try {
            NanopubImpl test = new NanopubImpl(file);
        }
        catch (MalformedNanopubException | OpenRDFException | IOException e) {
            fail("This is valid nanopublication");
        } 
    }
    
    /**
     * <p>
     * This is a valid nanopublication in rdf/xml format, so the test is not 
     * expected to throw exception.
     * </p>
     * @throws IOException 
     */
    @Test
    public void validNanopublication4() throws IOException{        
        // See package src/test/resources for the file.
        URL fileURL = this.getClass().getResource("/validNanopublication4.xml");
        File file = new File(fileURL.getPath());
        try {
            NanopubImpl test = new NanopubImpl(file);
        }
        catch (MalformedNanopubException | OpenRDFException | IOException e) {
            fail("This is valid nanopublication");
        } 
    }
}