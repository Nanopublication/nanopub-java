/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.rajaram.unit.test;

import ch.tkuhn.nanopub.MalformedNanopubException;
import ch.tkuhn.nanopub.NanopubImpl;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import nl.rajaram.unit.test.utils.FileOperation;
import org.junit.Test;
import org.openrdf.OpenRDFException;
import org.openrdf.rio.RDFFormat;
import static org.junit.Assert.*;

/**
 *
 * @author Rajaram
 * @since 10-10-2013
 * @version 1.0
 */
public class NanopubFromTextbox {
    
    public NanopubFromTextbox() {
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
        String content = FileOperation.readFile(fileURL.getPath(), 
                StandardCharsets.UTF_8);
        try {
            NanopubImpl test = new NanopubImpl(content, RDFFormat.TRIG);
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
        String content = FileOperation.readFile(fileURL.getPath(), 
                StandardCharsets.UTF_8);
        try {
            NanopubImpl test = new NanopubImpl(content, RDFFormat.NQUADS);
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
        String content = FileOperation.readFile(fileURL.getPath(), 
                StandardCharsets.UTF_8);
        try {
            NanopubImpl test = new NanopubImpl(content, RDFFormat.TRIX);
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
        String content = FileOperation.readFile(fileURL.getPath(), 
                StandardCharsets.UTF_8);
        try {
            NanopubImpl test = new NanopubImpl(content, RDFFormat.RDFXML);
        }
        catch (MalformedNanopubException | OpenRDFException | IOException e) {
            fail("This is valid nanopublication");
        } 
    }    
    
    /**
     * <p>
     * This is a valid nanopublication but the RDFFormat is wrong, so the test 
     * is expected to throw OpenRDFException.
     * </p>
     * @throws IOException 
     */
    @Test(expected = OpenRDFException.class)
    public void inValidNanopublication1() throws IOException, 
    OpenRDFException{        
        // See package src/test/resources for the file.
        URL fileURL = this.getClass().
                getResource("/validNanopublication2.nq");
        String content = FileOperation.readFile(fileURL.getPath(), 
                StandardCharsets.UTF_8);
        try {
            NanopubImpl test = new NanopubImpl(content, RDFFormat.TRIG);
        }
        catch (MalformedNanopubException |IOException e) {
            fail("This test is excepted to throw only "
                    + "OpenRDFException");
        }   
    }
    
    
}