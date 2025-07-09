package org.nanopub.extra.security;

import org.nanopub.Nanopub;
import org.nanopub.NanopubPattern;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.GeneralSecurityException;

/**
 * Nanopublication pattern for a digital signature.
 */
public class DigitalSignaturePattern implements NanopubPattern {

    @Override
    public String getName() {
        return "Digitally signed nanopublication";
    }

    @Override
    public boolean appliesTo(Nanopub nanopub) {
        return SignatureUtils.seemsToHaveSignature(nanopub);
    }

    @Override
    public boolean isCorrectlyUsedBy(Nanopub nanopub) {
        try {
            NanopubSignatureElement se = SignatureUtils.getSignatureElement(nanopub);
            if (se != null) {
                return SignatureUtils.hasValidSignature(se);
            } else {
                se = LegacySignatureUtils.getSignatureElement(nanopub);
                if (se != null) {
                    return LegacySignatureUtils.hasValidSignature(se);
                } else {
                    return false;
                }
            }
        } catch (MalformedCryptoElementException | GeneralSecurityException ex) {
            return false;
        }
    }

    @Override
    public String getDescriptionFor(Nanopub nanopub) {
        if (isCorrectlyUsedBy(nanopub)) {
            if (hasLegacySignature(nanopub)) {
                return "Valid digital signature (LEGACY)";
            } else {
                return "Valid digital signature";
            }
        } else {
            return "Digital signature is not valid";
        }
    }

    private boolean hasLegacySignature(Nanopub nanopub) {
        try {
            return LegacySignatureUtils.getSignatureElement(nanopub) != null;
        } catch (MalformedCryptoElementException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public URL getPatternInfoUrl() throws MalformedURLException {
        return new URL("https://github.com/Nanopublication/nanopub-java/blob/master/src/main/java/org/nanopub/extra/security/NanopubSignatureElement.java");
    }

}
