package org.nanopub.op;

import com.beust.jcommander.ParameterException;
import org.junit.jupiter.api.Test;
import org.nanopub.CliRunner;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Test the initialization of ExportJson.
 * The run itself is tested elsewhere.
 */
class ExportJsonTest {

    @Test
    void initNoArgs() throws Exception {
        assertThrows(ParameterException.class, () -> {
            CliRunner.initJc(new ExportJson(), new String[0]);
        });
    }

    @Test
    void initInvalidArgs() throws Exception {
        // LATER check parameters at this stage
//        assertThrows(ParameterException.class, () -> {
//            ExportJson.init(new String[] {"in-files", "--in-format", "any-invalid-format"});
//        });

    }

    @Test
    void initValidArgs() throws Exception {
        ExportJson obj = CliRunner.initJc(new ExportJson(), new String[] {"inputFile"});
        assertNotNull(obj);

        obj = CliRunner.initJc(new ExportJson(), new String[] {"inputFile", "-o", "outputFile"});
        assertNotNull(obj);

        obj = CliRunner.initJc(new ExportJson(), new String[] {"inputFile", "--in-format", "trig"});
        assertNotNull(obj);
    }

}