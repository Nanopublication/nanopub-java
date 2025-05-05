package org.nanopub;

import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.PROV;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.junit.jupiter.api.Test;

import java.io.OutputStreamWriter;
import java.nio.charset.Charset;

import static java.lang.System.out;

public class FdoNanopubTest {

    private static final ValueFactory vf = SimpleValueFactory.getInstance();

    @Test
    void example() throws MalformedNanopubException {
        FdoNanopubCreator creator = new FdoNanopubCreator("21.T11967/39b0ec87d17a4856c5f7", "21.T11966/365ff9576c26ca6053db","NumberFdo1" );

        NanopubCreator underlyingNpCreator = creator.getNanopubCreator();
        underlyingNpCreator.addProvenanceStatement(PROV.ATTRIBUTION, vf.createIRI("https://orcid.org/0000-0000-0000-0000"));

        Nanopub np = underlyingNpCreator.finalizeNanopub(true);

        RDFWriter w = Rio.createWriter(RDFFormat.TRIG, new OutputStreamWriter(out, Charset.forName("UTF-8")));
        NanopubUtils.propagateToHandler(np, w);

    }

}
