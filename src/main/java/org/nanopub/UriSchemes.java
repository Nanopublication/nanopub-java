package org.nanopub;

import java.util.Locale;
import java.util.Set;

/**
 * Defines which URI schemes are allowed at which position in a nanopublication.
 * <p>
 * Nanopublications have historically been restricted to http(s) URIs by tooling convention, but the
 * nanopublication guidelines do not mandate this. Content-addressed and decentralized identifiers
 * ({@code ipfs:}, {@code ipns:}, {@code did:}, {@code at:}) are valid IRIs and are useful as
 * subjects and objects of assertions. They are meaningless as predicates, however, and the
 * nanopublication's own URI has to remain http(s) because that is what the trusty URI scheme
 * assumes.
 * <p>
 * This class is the single place where that policy is stated, so that downstream tools do not each
 * have to re-invent an ad-hoc {@code matches("https?://.+")} test.
 *
 * @author Tobias Kuhn
 */
public class UriSchemes {

    /**
     * The position a URI occupies in a nanopublication, which determines the schemes allowed for it.
     */
    public enum Position {

        /**
         * The subject of a triple, in any of the four graphs.
         */
        SUBJECT,

        /**
         * The predicate of a triple, in any of the four graphs.
         */
        PREDICATE,

        /**
         * The object of a triple, in any of the four graphs. Only applies if the object is a URI.
         */
        OBJECT,

        /**
         * The nanopublication's own URI, or one of its four graph URIs.
         */
        NANOPUB_URI

    }

    /**
     * The schemes allowed for predicates and for the nanopublication's own URI.
     */
    public static final Set<String> HTTP_SCHEMES = Set.of("http", "https");

    /**
     * The schemes allowed for subjects and objects.
     */
    public static final Set<String> RESOURCE_SCHEMES = Set.of("http", "https", "ipfs", "ipns", "did", "at");

    private UriSchemes() {
    }  // no instances allowed

    /**
     * Returns the schemes that are allowed at the given position.
     *
     * @param position the position the URI occupies
     * @return an unmodifiable set of lower-case scheme names
     */
    public static Set<String> getAllowedSchemes(Position position) {
        return switch (position) {
            case SUBJECT, OBJECT -> RESOURCE_SCHEMES;
            case PREDICATE, NANOPUB_URI -> HTTP_SCHEMES;
        };
    }

    /**
     * Checks whether the given URI uses a scheme that is allowed at the given position.
     *
     * @param uri      the URI to check
     * @param position the position the URI occupies
     * @return true if the URI is absolute and its scheme is allowed at that position
     */
    public static boolean isAllowedUriScheme(String uri, Position position) {
        String scheme = getScheme(uri);
        return scheme != null && getAllowedSchemes(position).contains(scheme);
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
