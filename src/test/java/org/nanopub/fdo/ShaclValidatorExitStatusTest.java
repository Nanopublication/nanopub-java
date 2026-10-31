package org.nanopub.fdo;

import org.junit.jupiter.api.Test;
import org.nanopub.CliRunner;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests the exit status the {@link ShaclValidator} command reports.
 */
class ShaclValidatorExitStatusTest {

    private static final String SHAPE = "./src/test/resources/fdo/shape.trig";

    @Test
    void exitStatus_withConformingNanopub_isZero() throws Exception {
        ShaclValidator validator = runValidator("./src/test/resources/fdo/validPerson.trig");

        assertEquals(0, validator.exitStatus());
    }

    @Test
    void exitStatus_withNonConformingNanopub_isOne() throws Exception {
        ShaclValidator validator = runValidator("./src/test/resources/fdo/invalidPerson.trig");

        assertEquals(1, validator.exitStatus());
    }

    /**
     * Runs the validator on the given nanopub against the test shape.
     *
     * @param nanopubFile the path of the nanopub to validate
     * @return the validator after its run
     * @throws Exception if the validator cannot be set up or run
     */
    private static ShaclValidator runValidator(String nanopubFile) throws Exception {
        ShaclValidator validator = CliRunner.initJc(new ShaclValidator(), new String[]{"-n", nanopubFile, "-s", SHAPE});
        validator.run();
        return validator;
    }

}
