package org.nanopub;

import java.util.Locale;
import java.util.Set;

/**
 * Defines which URI schemes are allowed in a nanopublication.
 * <p>
 * Nanopublications have historically been restricted to http(s) URIs by tooling convention, but the
 * nanopublication guidelines do not mandate this. Content-addressed and decentralized identifiers
 * ({@code ipfs:}, {@code ipns:}, {@code did:}, {@code at:}) are valid IRIs and are useful to refer
 * to from within a nanopublication. Allowing them is not an endorsement: it only means that
 * nanopublications using them are not flagged as problematic.
 * <p>
 * This class is the single place where that policy is stated, so that downstream tools do not each
 * have to re-invent an ad-hoc {@code matches("https?://.+")} test.
 *
 * @author Tobias Kuhn
 */
public class UriSchemes {

    /**
     * The URI schemes allowed in a nanopublication, in any position.
     */
    public static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https", "ipfs", "ipns", "did", "at");

    private UriSchemes() {
    }  // no instances allowed

    /**
     * Checks whether the given URI uses one of the allowed schemes.
     *
     * @param uri the URI to check
     * @return true if the URI is absolute and its scheme is allowed
     */
    public static boolean isAllowedUriScheme(String uri) {
        String scheme = getScheme(uri);
        return scheme != null && ALLOWED_SCHEMES.contains(scheme);
    }

    /**
     * Extracts the scheme of an absolute URI, normalized to lower case. URI schemes are
     * case-insensitive, and consist of a letter followed by letters, digits, {@code +}, {@code -}
     * and {@code .} (RFC 3986).
     *
     * @param uri the URI to take the scheme from
     * @return the lower-case scheme, or null if the given string is null or is not an absolute URI
     */
    public static String getScheme(String uri) {
        if (uri == null) return null;
        int colon = uri.indexOf(':');
        if (colon < 1) return null;
        for (int i = 0; i < colon; i++) {
            char c = uri.charAt(i);
            boolean valid = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') ||
                    (i > 0 && ((c >= '0' && c <= '9') || c == '+' || c == '-' || c == '.'));
            if (!valid) return null;
        }
        return uri.substring(0, colon).toLowerCase(Locale.ROOT);
    }

}
