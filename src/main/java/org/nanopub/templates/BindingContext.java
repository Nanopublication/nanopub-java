package org.nanopub.templates;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.nanopub.Nanopub;
import org.nanopub.SimpleCreatorPattern;
import org.nanopub.vocabulary.NTEMPLATE;

import java.io.Serializable;
import java.util.*;

/**
 * The values bound to the positions of a template's statements, for one nanopublication:
 * either an existing one that is checked against the template, or one that is being made
 * from it.
 * <p>
 * A binding is the RDF value as it stands in the nanopublication. Its key is the value the
 * template has in that position, which is a placeholder or a fixed IRI. Where a statement is
 * repeated, the key carries a {@code __N} suffix for the repetition, as in
 * {@link Template#getTemplateIri(IRI)}; which positions get such a suffix is for the caller
 * to decide.
 * <p>
 * The context also knows what a {@link PlaceholderMatcher} needs to know about the
 * nanopublication: its URI and namespace, under which local resources are minted, its
 * assertion URI, and its creators.
 */
public class BindingContext implements Serializable {

    private static final ValueFactory vf = SimpleValueFactory.getInstance();

    /**
     * Stands in a prefix ({@code nt:hasPrefix}) for the IRI of the space a nanopublication is
     * made for.
     */
    public static final String SPACE_TOKEN = "~~SPACE~~";

    /**
     * Stands in a prefix ({@code nt:hasPrefix}) for the namespace of the resource a
     * nanopublication is made for.
     */
    public static final String NAMESPACE_TOKEN = "~~NAMESPACE~~";

    private final Template template;
    private final IRI nanopubUri;
    private final IRI assertionUri;
    private final String namespace;
    private final Set<IRI> creators;
    private final Map<IRI, Value> bindings = new LinkedHashMap<>();
    private final Map<String, String> prefixBases = new HashMap<>();
    private final Set<IRI> pinned = new HashSet<>();

    private BindingContext(Template template, IRI nanopubUri, IRI assertionUri, Set<IRI> creators) {
        this.template = Objects.requireNonNull(template, "template");
        this.nanopubUri = Objects.requireNonNull(nanopubUri, "nanopubUri");
        this.assertionUri = Objects.requireNonNull(assertionUri, "assertionUri");
        this.namespace = getNamespace(nanopubUri.stringValue(), assertionUri.stringValue());
        this.creators = Collections.unmodifiableSet(new LinkedHashSet<>(creators));
    }

    /**
     * Creates a context for checking an existing nanopublication against a template.
     *
     * @param template the template
     * @param nanopub  the nanopublication to check
     * @return the context, without any bindings
     */
    public static BindingContext forNanopub(Template template, Nanopub nanopub) {
        Objects.requireNonNull(nanopub, "nanopub");
        return new BindingContext(template, nanopub.getUri(), nanopub.getAssertionUri(), SimpleCreatorPattern.getCreators(nanopub));
    }

    /**
     * Creates a context for making a nanopublication from a template.
     *
     * @param template   the template
     * @param nanopubUri the URI of the nanopublication to make, which is a temporary one
     *                   ending in a slash as long as the nanopublication is not signed, such
     *                   as {@code http://purl.org/nanopub/temp/np001/}
     * @param creator    the creator of the nanopublication, which {@code nt:CREATOR} stands
     *                   for, or null if not known
     * @return the context, with the creator bound if one is given
     */
    public static BindingContext forNewNanopub(Template template, String nanopubUri, IRI creator) {
        Objects.requireNonNull(nanopubUri, "nanopubUri");
        IRI uri = vf.createIRI(nanopubUri);
        String ns = (nanopubUri.endsWith("/") || nanopubUri.endsWith("#")) ? nanopubUri : nanopubUri + "/";
        Set<IRI> creators = creator == null ? Set.of() : Set.of(creator);
        BindingContext context = new BindingContext(template, uri, vf.createIRI(ns + "assertion"), creators);
        if (creator != null) context.bindings.put(NTEMPLATE.CREATOR_PLACEHOLDER, creator);
        return context;
    }

    // The namespace is what stands before the name of the assertion graph, where that graph
    // has its usual name; this covers nanopublications that use a hash as separator.
    private static String getNamespace(String nanopubUri, String assertionUri) {
        if (assertionUri.startsWith(nanopubUri) && assertionUri.endsWith("assertion")) {
            String ns = assertionUri.substring(0, assertionUri.length() - "assertion".length());
            if (ns.length() - nanopubUri.length() <= 1) return ns;
        }
        if (nanopubUri.endsWith("/") || nanopubUri.endsWith("#")) return nanopubUri;
        return nanopubUri + "/";
    }

    /**
     * @return the template the values are bound for
     */
    public Template getTemplate() {
        return template;
    }

    /**
     * @return the URI of the nanopublication, which {@code nt:NANOPUB} stands for
     */
    public IRI getNanopubUri() {
        return nanopubUri;
    }

    /**
     * @return the URI of the assertion graph of the nanopublication, which
     * {@code nt:ASSERTION} stands for
     */
    public IRI getAssertionUri() {
        return assertionUri;
    }

    /**
     * @return the namespace of the nanopublication, under which its local resources are
     * minted: its URI followed by a slash or a hash
     */
    public String getNamespace() {
        return namespace;
    }

    /**
     * @return the creators of the nanopublication, any of which {@code nt:CREATOR} can stand
     * for; empty if they are not known
     */
    public Set<IRI> getCreators() {
        return creators;
    }

    /**
     * Returns the name an IRI has within the nanopublication.
     *
     * @param value the value to look at
     * @return what follows the namespace of the nanopublication, or null if the value is not
     * an IRI in that namespace
     */
    public String getLocalName(Value value) {
        if (!(value instanceof IRI)) return null;
        String s = value.stringValue();
        if (!s.startsWith(namespace) || s.length() == namespace.length()) return null;
        return s.substring(namespace.length());
    }

    /**
     * Mints an IRI within the nanopublication.
     *
     * @param localName the name within the nanopublication
     * @return the IRI of that name in the namespace of the nanopublication
     */
    public IRI mint(String localName) {
        return vf.createIRI(namespace + localName);
    }

    /**
     * Returns the value bound to a position.
     *
     * @param templateValue the value of the template in that position, with a repetition
     *                      suffix if it has one
     * @return the bound value, or null if there is none
     */
    public Value get(IRI templateValue) {
        return bindings.get(templateValue);
    }

    /**
     * @param templateValue the value of the template in a position
     * @return true if a value is bound to it
     */
    public boolean isBound(IRI templateValue) {
        return bindings.containsKey(templateValue);
    }

    /**
     * Binds a value to a position, if it can stand there. A position that holds a fixed IRI
     * is recorded like a placeholder, so that the resources a nanopublication introduces and
     * embeds can be told from the bindings alone.
     *
     * @param templateValue the value of the template in that position, with a repetition
     *                      suffix if it has one
     * @param value         the value to bind
     * @return what the check of the value found; the value is bound if
     * {@link MatchResult#isBindable()} holds
     */
    public MatchResult bind(Value templateValue, Value value) {
        MatchResult result = PlaceholderMatcher.of(template, templateValue).match(value, this);
        if (result.isBindable() && templateValue instanceof IRI key) bindings.put(key, value);
        return result;
    }

    /**
     * Binds a value to a position without checking it. This is for a value that is not given
     * anew but carried over, such as the IRI of a resource that an earlier version of the
     * nanopublication introduced: it stands as it is, whatever the template would say about
     * a value given now. Such a value is left out of {@link #getViolations()} too.
     *
     * @param templateValue the value of the template in that position, with a repetition
     *                      suffix if it has one
     * @param value         the value to bind
     */
    void pin(IRI templateValue, Value value) {
        bindings.put(templateValue, value);
        pinned.add(templateValue);
    }

    /**
     * Removes the binding of a position.
     *
     * @param templateValue the value of the template in that position
     */
    public void unbind(IRI templateValue) {
        bindings.remove(templateValue);
        pinned.remove(templateValue);
    }

    /**
     * @return the bindings, by the value of the template in each bound position; unmodifiable
     */
    public Map<IRI, Value> getBindings() {
        return Collections.unmodifiableMap(bindings);
    }

    /**
     * Takes a copy of the bindings, to return to with {@link #restore(Map)} after trying out
     * an assignment that turns out not to work.
     *
     * @return a copy of the current bindings
     */
    public Map<IRI, Value> snapshot() {
        return new LinkedHashMap<>(bindings);
    }

    /**
     * Sets the bindings back to an earlier state.
     *
     * @param snapshot the bindings as {@link #snapshot()} returned them
     */
    public void restore(Map<IRI, Value> snapshot) {
        bindings.clear();
        bindings.putAll(snapshot);
        pinned.retainAll(snapshot.keySet());
    }

    /**
     * Returns the RDF value a position of a template statement stands for under the current
     * bindings: the bound value of a placeholder, a fixed value as the template gives it, an
     * IRI in the namespace of this nanopublication for a local resource, and the URIs of the
     * nanopublication and its assertion for {@code nt:NANOPUB} and {@code nt:ASSERTION}.
     *
     * @param templateValue the value of the template in that position, with a repetition
     *                      suffix if it has one
     * @return the value, or null if the position is a placeholder nothing is bound to
     */
    public Value resolve(Value templateValue) {
        return PlaceholderMatcher.of(template, templateValue).resolve(this);
    }

    /**
     * Returns the resources the nanopublication introduces: the values bound to positions
     * the template marks with {@code nt:IntroducedResource}.
     *
     * @return the introduced IRIs
     */
    public Set<IRI> getIntroducedIris() {
        Set<IRI> iris = new LinkedHashSet<>();
        for (Map.Entry<IRI, Value> e : bindings.entrySet()) {
            if (e.getValue() instanceof IRI iri && template.isIntroducedResource(e.getKey())) iris.add(iri);
        }
        return iris;
    }

    /**
     * Returns the resources the nanopublication embeds: the values bound to positions the
     * template marks with {@code nt:EmbeddedResource}.
     *
     * @return the embedded IRIs
     */
    public Set<IRI> getEmbeddedIris() {
        Set<IRI> iris = new LinkedHashSet<>();
        for (Map.Entry<IRI, Value> e : bindings.entrySet()) {
            if (e.getValue() instanceof IRI iri && template.isEmbeddedResource(e.getKey())) iris.add(iri);
        }
        return iris;
    }

    /**
     * Returns the direction pins of the role predicates in the nanopublication: for each IRI
     * bound to a position the template pins as a regular or inverse role property, that pin
     * (see {@link Template#getRoleDirectionPin(IRI)}).
     *
     * @return the pin class by predicate IRI
     */
    public Map<IRI, IRI> getRoleDirectionPins() {
        Map<IRI, IRI> pins = new LinkedHashMap<>();
        for (Map.Entry<IRI, Value> e : bindings.entrySet()) {
            IRI pin = template.getRoleDirectionPin(e.getKey());
            if (pin != null && e.getValue() instanceof IRI iri) pins.put(iri, pin);
        }
        return pins;
    }

    /**
     * Checks all bound values once more, against the bindings as they are now. This finds
     * what could not be told while the values were bound one by one, such as a restricted
     * choice whose options are the values of other placeholders.
     *
     * @return the violations of all bound values, empty if there are none
     */
    public List<Violation> getViolations() {
        List<Violation> violations = new ArrayList<>();
        for (Map.Entry<IRI, Value> e : bindings.entrySet()) {
            if (pinned.contains(e.getKey())) continue;
            violations.addAll(PlaceholderMatcher.of(template, e.getKey()).match(e.getValue(), this).violations());
        }
        return violations;
    }

    /**
     * Says what {@link #SPACE_TOKEN} or {@link #NAMESPACE_TOKEN} stands for in the prefixes
     * of this template. A template uses these to have a resource minted under the space or
     * the resource a nanopublication is made for; only the caller knows which one that is.
     *
     * @param token the token
     * @param base  the IRI the token stands for, or null to leave it open
     */
    public void setPrefixBase(String token, String base) {
        if (base == null || base.isEmpty()) {
            prefixBases.remove(token);
        } else {
            prefixBases.put(token, base);
        }
    }

    /**
     * Returns the prefix of a placeholder ({@code nt:hasPrefix}), with a token in it replaced
     * by what it stands for.
     *
     * @param placeholder the placeholder
     * @return the prefix; null if the template declares none, and also if it declares one
     * with a token that nothing has been given for (see {@link #hasUnresolvedPrefix(IRI)})
     */
    public String getPrefix(IRI placeholder) {
        String rawPrefix = template.getPrefix(placeholder);
        String token = getToken(rawPrefix);
        if (token == null) return rawPrefix;
        String base = prefixBases.get(token);
        if (base == null) return null;
        return rawPrefix.replace(token, base);
    }

    /**
     * @param placeholder the placeholder
     * @return true if the prefix of the placeholder has a token in it, given or not
     */
    public boolean hasDynamicPrefix(IRI placeholder) {
        return getToken(template.getPrefix(placeholder)) != null;
    }

    /**
     * @param placeholder the placeholder
     * @return true if the prefix of the placeholder has a token in it that nothing has been
     * given for
     */
    public boolean hasUnresolvedPrefix(IRI placeholder) {
        String token = getToken(template.getPrefix(placeholder));
        return token != null && !prefixBases.containsKey(token);
    }

    private static String getToken(String rawPrefix) {
        if (rawPrefix == null) return null;
        if (rawPrefix.contains(SPACE_TOKEN)) return SPACE_TOKEN;
        if (rawPrefix.contains(NAMESPACE_TOKEN)) return NAMESPACE_TOKEN;
        return null;
    }

}
