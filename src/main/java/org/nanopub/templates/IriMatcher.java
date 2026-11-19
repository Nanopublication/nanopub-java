package org.nanopub.templates;

import net.trustyuri.TrustyUriUtils;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.nanopub.NanopubUtils;
import org.nanopub.UriSchemes;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A placeholder that takes an IRI: a URI, external URI, trusty URI, auto-escape URI, guided
 * choice or agent placeholder, or a root nanopublication placeholder.
 * <p>
 * A value is looked at in the form it is given in when the template is filled in: as the
 * part after the prefix of the placeholder where it starts with that prefix, as a name where
 * it is minted in the namespace of the nanopublication, and otherwise as the IRI in full.
 * The regular expression of the placeholder applies to that form. What follows a prefix
 * cannot contain a colon or a hash; an IRI where it would is taken in full.
 * <p>
 * A guided choice only suggests values, so the values it would offer do not constrain it.
 * <p>
 * A resource that is minted within the nanopublication and introduced by it keeps its IRI in
 * later versions. Such an IRI in the namespace of another nanopublication is taken by its
 * name there.
 */
final class IriMatcher extends PlaceholderMatcher {

    private final IRI key;

    IriMatcher(Template template, IRI iri) {
        super(template, iri);
        this.key = iri;
    }

    @Override
    public boolean isPlaceholder() {
        return true;
    }

    private boolean isAutoEscape() {
        return template.isAutoEscapePlaceholder(key);
    }

    @Override
    void check(Value value, BindingContext context, List<Violation> violations) {
        if (!(value instanceof IRI)) {
            violations.add(blocking(value, Violation.Kind.WRONG_KIND, "The value " + describe(value) + " of '" + getFieldLabel() + "' is not an IRI"));
            return;
        }
        String full = value.stringValue();
        String prefix = getMintingPrefix(key, context);
        String localName = context.getLocalName(value);
        // The value as given when filling in: after the prefix, as a local name, or in full.
        String given = full;
        boolean minted = false;
        if (!prefix.isEmpty() && full.startsWith(prefix) && IriUtils.isPostfix(full.substring(prefix.length()))) {
            given = full.substring(prefix.length());
            minted = prefix.equals(context.getNamespace());
        } else if (localName != null && IriUtils.isPlainName(localName)) {
            given = localName;
            minted = true;
        } else if (isCarriedOver(full, context)) {
            given = IriUtils.getPostfix(full);
        }
        boolean inFull = given.equals(full);

        if (!IriUtils.isWellFormedUri(full)) {
            violations.add(blocking(value, Violation.Kind.MALFORMED_IRI, "The IRI <" + full + "> of '" + getFieldLabel() + "' is not well-formed"));
        }
        if (inFull && !IriUtils.isUriValue(full)) {
            violations.add(blocking(value, Violation.Kind.SCHEME_NOT_ALLOWED, "The IRI <" + full + "> of '" + getFieldLabel()
                    + "' has a scheme that is not allowed here; use one of: "
                    + UriSchemes.ALLOWED_SCHEMES.stream().sorted().collect(Collectors.joining(", "))));
        }
        checkRegex(key, given, value, violations);
        if (minted && template.isExternalUriPlaceholder(key)) {
            violations.add(blocking(value, Violation.Kind.NOT_EXTERNAL, "The IRI <" + full + "> of '" + getFieldLabel()
                    + "' is minted within the nanopublication, but an external IRI is needed"));
        }
        if (template.isTrustyUriPlaceholder(key) && !TrustyUriUtils.isPotentialTrustyUri(full)) {
            violations.add(blocking(value, Violation.Kind.NOT_TRUSTY, "The IRI <" + full + "> of '" + getFieldLabel() + "' is not a trusty URI"));
        }
        if (localName != null && NanopubUtils.isReservedLocalName(localName)) {
            violations.add(reported(value, Violation.Kind.RESERVED_NAME, "'" + localName + "' is what the nanopublication calls one of its"
                    + " own parts, so the value of '" + getFieldLabel() + "' is that part rather than a resource of its own"));
        }
    }

    // An introduced resource keeps its IRI when the nanopublication is superseded, so a
    // resource that is minted within the nanopublication may sit in the namespace of an
    // earlier version. It is then the name within that version that was given.
    private boolean isCarriedOver(String full, BindingContext context) {
        if (!template.isIntroducedResource(key) || !template.isLocalResource(key) || context.hasDynamicPrefix(key)) return false;
        String name = IriUtils.getPostfix(full);
        if (!IriUtils.isPlainName(name) || full.length() <= name.length() + 1) return false;
        String nanopubUri = full.substring(0, full.length() - name.length() - 1);
        String artifactCode = TrustyUriUtils.getArtifactCode(nanopubUri);
        return artifactCode != null && nanopubUri.endsWith(artifactCode);
    }

    @Override
    public Value resolve(BindingContext context) {
        Value bound = context.get(key);
        if (bound != null) return bound;
        // A nanopublication is its own root definition unless it says otherwise.
        if (template.isRootNanopubPlaceholder(key)) return context.getNanopubUri();
        return null;
    }

    @Override
    public Value toValue(RawValue raw, BindingContext context) {
        String text = raw.text();
        if (text.isEmpty()) return null;
        if (!isAutoEscape()) return toIri(key, text, context);
        String prefix = getMintingPrefix(key, context);
        // An IRI that already has the prefix is the escaped value itself. This is how a value
        // gets through unchanged whose escaping would not come out the same again, as with
        // "%20" where escaping writes "+".
        if (!prefix.isEmpty() && text.startsWith(prefix) && IriUtils.isUriValue(text)) return createIri(text);
        checkPrefixResolved(key, context);
        String v = prefix + IriUtils.urlEncode(text);
        if (IriUtils.isPlainName(v)) v = context.getNamespace() + v;
        return createIri(v);
    }

    @Override
    public RawValue toRaw(Value value, BindingContext context) {
        if (!(value instanceof IRI)) return new RawValue(value.stringValue());
        String full = value.stringValue();
        List<String> candidates = new ArrayList<>();
        String prefix = getMintingPrefix(key, context);
        if (!prefix.isEmpty() && full.startsWith(prefix)) candidates.add(unescape(full.substring(prefix.length())));
        String localName = context.getLocalName(value);
        if (localName != null) candidates.add(unescape(localName));
        return shortestRaw(value, context, candidates);
    }

    private String unescape(String text) {
        if (!isAutoEscape()) return text;
        try {
            return IriUtils.urlDecode(text);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

}
