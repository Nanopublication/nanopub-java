package org.nanopub;

import net.trustyuri.TrustyUriException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.nanopub.extra.security.MalformedCryptoElementException;
import org.nanopub.extra.security.SignatureUtils;
import org.nanopub.extra.security.TransformContext;

import java.security.GeneralSecurityException;

import static org.nanopub.SimpleCreatorPattern.PROV_WASATTRIBUTEDTO;

/**
 * <p>
 * Handles Retracting of Nanopublications
 * </p>
 * The retraction is a separate np, which must be signed with the same key as the original np.
 */
public class NanopubRetractor {

    private static final ValueFactory vf = SimpleValueFactory.getInstance();

    /**
     * The IRI for the retraction statement.
     */
    public static final IRI RETRACTION = vf.createIRI("http://purl.org/nanopub/x/retracts");


    /**
     * Create a retraction np out of the original np and a transformation context
     *
     * @param originalNp The Nanopublication to be retracted
     * @param tc         The transfomation context with the public key
     * @return the retraction np, which can be published afterward
     * @throws org.nanopub.extra.security.MalformedCryptoElementException if the public key in the context is malformed
     * @throws org.nanopub.MalformedNanopubException                      if the original nanopub is malformed
     * @throws net.trustyuri.TrustyUriException                           if the original nanopub's URI is not a valid Trusty URI
     * @throws java.security.GeneralSecurityException                     if there is a security issue with signing the retraction
     */
    public static Nanopub createRetraction(Nanopub originalNp, TransformContext tc) throws MalformedCryptoElementException, MalformedNanopubException, TrustyUriException, GeneralSecurityException {
        SignatureUtils.assertMatchingPubkeys(tc, originalNp);
        NanopubCreator c = new NanopubCreator(true);
        c.addAssertionStatement(tc.getSigner(), RETRACTION, originalNp.getUri());
        c.addProvenanceStatement(PROV_WASATTRIBUTEDTO, tc.getSigner());
        c.addPubinfoStatement(DCTERMS.CREATOR, tc.getSigner());

        Nanopub retractionNp = c.finalizeNanopub(true);
        return SignatureUtils.createSignedNanopub(retractionNp, tc);
    }
}
