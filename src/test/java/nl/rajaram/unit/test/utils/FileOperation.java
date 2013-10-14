/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.rajaram.unit.test.utils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * <p>
 * Class to do simple file operation. (Example. read from a file, 
 * write to a file)
 * </p>
 * @author Rajaram
 * @since 10-10-2013
 * @version 1.0
 */
public class FileOperation {    
    
    /**
     * <p>
     * To read the content of the given file.
     * </p> 
     * @param filePath Path of the file.
     * @param encoding Charset (Example: UTF_8)
     * @return  File content in string format.
     * @throws IOException If the file path is incorrect.
     */
    public static String readFile(String filePath, Charset encoding) 
            throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(filePath));
        return encoding.decode(ByteBuffer.wrap(encoded)).toString();
    }
    
}
