package org.nanopub.templates;

import org.eclipse.rdf4j.common.net.ParsedIRI;
import org.eclipse.rdf4j.model.IRI;
import org.nanopub.UriSchemes;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * Helpers for the IRIs that fill placeholders.
 */
final class IriUtils {

    private IriUtils() {
    }

    /**
     * The marker for an artifact code in an IRI, as a template writes it.
     */
    static final String ARTIFACT_CODE_MARKER = "~~ARTIFACTCODE~~";

    /**
     * The marker for an artifact code in an IRI of a nanopublication that is not signed yet,
     * which is replaced by the artifact code when the nanopublication is signed.
     */
    static final String ARTIFACT_CODE_TARGET_MARKER = "~~~ARTIFACTCODE~~~";

    /**
     * Whether the text is an IRI in one of the schemes nanopublications allow, rather than a
     * name that still needs a prefix or a namespace in front of it.
     */
    static boolean isUriValue(String text) {
        if (text == null || text.isBlank()) return false;
        if (!UriSchemes.isAllowedUriScheme(text)) return false;
        for (int i = 0; i < text.length(); i++) {
            if (Character.isWhitespace(text.charAt(i))) return false;
        }
        String rest = text.substring(text.indexOf(':') + 1);
        if (rest.startsWith("//")) rest = rest.substring(2);
        return !rest.isEmpty();
    }

    /**
     * Whether the text is an absolute IRI.
     */
    static boolean isWellFormedUri(String text) {
        if (text == null || text.isBlank()) return false;
        try {
            if (new ParsedIRI(text).isAbsolute()) return true;
        } catch (URISyntaxException ex) {
            // fall through to the more permissive parser below
        }
        try {
            return new URI(text).isAbsolute();
        } catch (URISyntaxException ex) {
            return false;
        }
    }

    /**
     * Whether the text is a plain name: one that gets the namespace of the nanopublication
     * put in front of it.
     */
    static boolean isPlainName(String text) {
        return text != null && text.matches("[^:# ]+");
    }

    /**
     * Whether the text can follow a prefix: it is not empty and has no colon or hash in it.
     */
    static boolean isPostfix(String text) {
        return text != null && !text.isEmpty() && !text.contains(":") && !text.contains("#");
    }

    /**
     * The part of an IRI after its last hash, or after its last slash if it has no hash.
     */
    static String getPostfix(String iri) {
        if (iri.contains("#")) return iri.substring(iri.lastIndexOf('#') + 1);
        return iri.substring(iri.lastIndexOf('/') + 1);
    }

    static String urlEncode(String text) {
        return URLEncoder.encode(text, StandardCharsets.UTF_8);
    }

    /**
     * Decodes URL-encoded text.
     *
     * @throws IllegalArgumentException if the text has a percent sign that starts no escape
     */
    static String urlDecode(String text) {
        return URLDecoder.decode(text, StandardCharsets.UTF_8);
    }

    static boolean hasArtifactCodeMarker(String iri) {
        return iri.contains(ARTIFACT_CODE_MARKER);
    }

    /**
     * Writes the artifact-code marker of a template IRI the way an unsigned nanopublication
     * carries it.
     */
    static String toTargetMarker(String iri) {
        return iri.replace(ARTIFACT_CODE_TARGET_MARKER, ARTIFACT_CODE_MARKER).replace(ARTIFACT_CODE_MARKER, ARTIFACT_CODE_TARGET_MARKER);
    }

    /**
     * Whether an IRI is the given template IRI, with any artifact code where the template
     * IRI has its marker. A fresh artifact code is put there when a nanopublication is
     * signed, so the marker stands for whichever one that was.
     */
    static boolean matchesWithArtifactCode(String templateIri, IRI iri) {
        String normalized = templateIri.replace(ARTIFACT_CODE_TARGET_MARKER, ARTIFACT_CODE_MARKER);
        String[] parts = normalized.split(Pattern.quote(ARTIFACT_CODE_MARKER), -1);
        StringBuilder regex = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) regex.append("(?:[A-Za-z0-9_-]+|").append(Pattern.quote(ARTIFACT_CODE_TARGET_MARKER)).append(")");
            regex.append(Pattern.quote(parts[i]));
        }
        return iri.stringValue().matches(regex.toString());
    }

}
