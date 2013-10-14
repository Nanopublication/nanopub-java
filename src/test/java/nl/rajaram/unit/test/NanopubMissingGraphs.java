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
import static org.junit.Assert.*;
import org.openrdf.OpenRDFException;
import org.openrdf.rio.RDFFormat;

/**
 *
 * @author Rajaram
 * @since 14-10-2013
 * @version 1.0
 */
public class NanopubMissingGraphs {
    
    public NanopubMissingGraphs() {
    }
    
    /**
     * <p>
     * The nanopublication graph is missing, so this test is expected 
     * to throw MalformedNanopubException exception. 
     * </p>
     * @throws MalformedNanopubException 
     */
    @Test(expected = MalformedNanopubException.class)
    public void noNanopublicationGraph() throws MalformedNanopubException,
    IOException {
        // See package src/test/resources for the file.
        URL fileURL = this.getClass().
                getResource("/noNanopublicationGraph.trig");
        String content = FileOperation.readFile(fileURL.getPath(), 
                StandardCharsets.UTF_8);
        try {
            NanopubImpl test = new NanopubImpl(content, RDFFormat.TRIG);
        }
        catch (OpenRDFException | IOException e) {
            fail("This is a invalid nanopublication");
        }
    }
    
    /**
     * <p>
     * The assertion graph is missing, so this test is expected 
     * to throw MalformedNanopubException exception. 
     * </p>
     * @throws MalformedNanopubException 
     */
    @Test(expected = MalformedNanopubException.class)
    public void noAssertionGraph() throws MalformedNanopubException,
    IOException {
        // See package src/test/resources for the file.
        URL fileURL = this.getClass().
                getResource("/noAssertionGraph.trig");
        String content = FileOperation.readFile(fileURL.getPath(), 
                StandardCharsets.UTF_8);
        try {
            NanopubImpl test = new NanopubImpl(content, RDFFormat.TRIG);
        }
        catch (OpenRDFException | IOException e) {
            fail("This is a invalid nanopublication");
        }
    }
    
    /**
     * <p>
     * The provenance graph is missing, so this test is expected 
     * to throw MalformedNanopubException exception. 
     * </p>
     * @throws MalformedNanopubException 
     */
    @Test(expected = MalformedNanopubException.class)
    public void noProvenanceGraph() throws MalformedNanopubException,
    IOException {
        // See package src/test/resources for the file.
        URL fileURL = this.getClass().
                getResource("/noProvenanceGraph.trig");
        String content = FileOperation.readFile(fileURL.getPath(), 
                StandardCharsets.UTF_8);
        try {
            NanopubImpl test = new NanopubImpl(content, RDFFormat.TRIG);
        }
        catch (OpenRDFException | IOException e) {
            fail("This is a invalid nanopublication");
        }
    }
    
    /**
     * <p>
     * The PublicationInfo graph is missing, so this test is expected 
     * to throw MalformedNanopubException exception. 
     * </p>
     * @throws MalformedNanopubException 
     */
    @Test(expected = MalformedNanopubException.class)
    public void noPublicationInfoGraph() throws MalformedNanopubException,
    IOException {
        // See package src/test/resources for the file.
        URL fileURL = this.getClass().
                getResource("/noPublicationInfoGraph.trig");
        String content = FileOperation.readFile(fileURL.getPath(), 
                StandardCharsets.UTF_8);
        try {
            NanopubImpl test = new NanopubImpl(content, RDFFormat.TRIG);
        }
        catch (OpenRDFException | IOException e) {
            fail("This is a invalid nanopublication");
        }
    }
}