package org.nanopub.extra.security;

// See: https://docs.oracle.com/javase/8/docs/technotes/guides/security/StandardNames.html#Signature

/**
 * Enumeration of supported signature algorithms.
 */
public enum SignatureAlgorithm {
    /**
     * Rivest–Shamir–Adleman (RSA) algorithm.
     */
    RSA,
    /**
     * Digital Signature Algorithm (DSA).
     */
    DSA
}
