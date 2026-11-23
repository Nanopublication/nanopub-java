package org.nanopub.extra.checks;

import net.trustyuri.TrustyUriUtils;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.nanopub.Nanopub;
import org.nanopub.extra.checks.NetworkFinding.Kind;
import org.nanopub.extra.checks.NetworkFinding.Severity;
import org.nanopub.extra.security.LegacySignatureUtils;
import org.nanopub.extra.security.MalformedCryptoElementException;
import org.nanopub.extra.security.NanopubSignatureElement;
import org.nanopub.extra.security.SignatureUtils;
import org.nanopub.templates.ComplianceIssue.Graph;
import org.nanopub.templates.MalformedTemplateException;
import org.nanopub.templates.NanopubResolver;
import org.nanopub.templates.Template;
import org.nanopub.templates.TemplateComplianceChecker;
import org.nanopub.vocabulary.KPXL;
import org.nanopub.vocabulary.KPXL_GRLC;
import org.nanopub.vocabulary.NPX;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Checks a nanopublication for mistakes that only show against other nanopublications. Such a
 * nanopublication is valid, signs and publishes, and still does not do what it is meant to:
 * <ol>
 *   <li>A new version has to introduce the resources of the version it supersedes again,
 *       under the same IRIs. Written with the prefix of the new nanopublication, an IRI
 *       changes with the trusty URI, and each version introduces a resource of its own.</li>
 *   <li>A template link has to name a template of its kind: an assertion, a provenance or a
 *       publication info template.</li>
 *   <li>A definition that names a space as governing it ({@code gen:governedBy}) is governed
 *       by that space only if the space maintains what the definition is a version of.</li>
 *   <li>A new version takes the place of the one it supersedes only if it is signed with
 *       the same key, unless a space governs it.</li>
 * </ol>
 * The first, second and fourth are reported as errors, the third as a note. Where the first
 * is about a query or about the assertion of the nanopublication itself, it is a note: these
 * are specific to their version.
 * <p>
 * All of this needs other nanopublications, and the third the query services. What cannot be
 * retrieved or asked is reported as not checked, never as an error.
 * <p>
 * Whether a nanopublication is what its templates describe is another check, see
 * {@link TemplateComplianceChecker}.
 */
public class NetworkChecker {

    private final NanopubResolver resolver;
    private final MaintainerLookup maintainers;
    // The kind of the template a link names; "" for something that is no template.
    private final Map<String, String> templateKinds = new HashMap<>();

    /**
     * Creates a checker that retrieves nanopublications from the nanopublication network and
     * asks its query services.
     */
    public NetworkChecker() {
        this(NanopubResolver.DEFAULT, MaintainerLookup.NETWORK);
    }

    /**
     * Creates a checker.
     *
     * @param resolver    retrieves the nanopublications that are superseded and the templates
     * @param maintainers says which spaces maintain a resource
     */
    public NetworkChecker(NanopubResolver resolver, MaintainerLookup maintainers) {
        this.resolver = Objects.requireNonNull(resolver, "resolver");
        this.maintainers = Objects.requireNonNull(maintainers, "maintainers");
    }

    /**
     * Checks a nanopublication.
     *
     * @param nanopub the nanopublication, signed or not
     * @return the findings, errors first; empty if nothing was found
     */
    public List<NetworkFinding> check(Nanopub nanopub) {
        Objects.requireNonNull(nanopub, "nanopub");
        List<NetworkFinding> findings = new ArrayList<>();
        // The versions this one supersedes; null for one that could not be retrieved.
        Map<IRI, Nanopub> superseded = new HashMap<>();
        for (IRI old : getObjects(nanopub, NPX.SUPERSEDES)) {
            Nanopub oldNanopub = retrieve(old.stringValue());
            superseded.put(old, oldNanopub);
            if (oldNanopub == null) {
                findings.add(note(Kind.UNCHECKED, "The superseded nanopublication <" + old + "> could not be retrieved, so this version is not checked against it"));
            }
        }
        checkIntroducedResources(nanopub, superseded, findings);
        checkTemplateLinks(nanopub, findings);
        Governance governance = checkGovernance(nanopub, superseded, findings);
        checkKey(nanopub, superseded, governance, findings);
        findings.sort((a, b) -> a.severity().compareTo(b.severity()));
        return findings;
    }

    // ---------- 1: a new version keeps the resources it introduces ----------

    private void checkIntroducedResources(Nanopub nanopub, Map<IRI, Nanopub> superseded, List<NetworkFinding> findings) {
        Set<IRI> introduced = getObjects(nanopub, NPX.INTRODUCES);
        String uri = nanopub.getUri().stringValue();
        List<IRI> mintedHere = introduced.stream().filter(iri -> iri.stringValue().startsWith(uri)).toList();
        // What is specific to a version by its nature: a query, and the assertion graph, which
        // is what a template in the old shape has as its node.
        boolean queriesOnly = !mintedHere.isEmpty() && mintedHere.stream().allMatch(iri -> isQuery(nanopub, iri));
        boolean assertionOnly = mintedHere.size() == 1 && mintedHere.get(0).equals(nanopub.getAssertionUri());
        for (Map.Entry<IRI, Nanopub> e : superseded.entrySet()) {
            if (e.getValue() == null) continue;
            Set<IRI> gone = getObjects(e.getValue(), NPX.INTRODUCES);
            gone.removeAll(introduced);
            if (gone.isEmpty()) continue;
            if (queriesOnly) {
                findings.add(note(Kind.REMINTED_QUERY, list(mintedHere) + " is a query and gets a new IRI with each version, which is right for a query."
                        + " Declare it with npx:embeds then, not with npx:introduces"));
            } else if (assertionOnly) {
                findings.add(note(Kind.REMINTED_ASSERTION, "The nanopublication introduces its own assertion <" + nanopub.getAssertionUri()
                        + ">, which is another IRI in each version. What is identified by the assertion of its nanopublication cannot keep its IRI in a new version"));
            } else if (!mintedHere.isEmpty()) {
                findings.add(error(Kind.REMINTED_RESOURCE, "The superseded nanopublication <" + e.getKey() + "> introduces " + list(gone)
                        + ", and this version introduces " + list(mintedHere) + " instead, which is another resource."
                        + " A new version keeps the IRI of what it introduces: write the earlier IRI in full, in the assertion and with npx:introduces"));
            } else {
                findings.add(note(Kind.DROPPED_RESOURCE, "The superseded nanopublication <" + e.getKey() + "> introduces " + list(gone)
                        + ", which this version does not introduce any more"));
            }
        }
    }

    private static boolean isQuery(Nanopub nanopub, IRI iri) {
        for (Statement st : nanopub.getAssertion()) {
            if (st.getSubject().equals(iri) && st.getPredicate().equals(RDF.TYPE) && st.getObject().equals(KPXL_GRLC.GRLC_QUERY)) return true;
        }
        return false;
    }

    // ---------- 2: template links name templates of their kind ----------

    private void checkTemplateLinks(Nanopub nanopub, List<NetworkFinding> findings) {
        boolean hasAssertionTemplate = !TemplateComplianceChecker.getTemplateLinks(nanopub, Graph.ASSERTION).isEmpty();
        for (Graph graph : Graph.values()) {
            String wanted = getName(graph);
            List<IRI> links = TemplateComplianceChecker.getTemplateLinks(nanopub, graph);
            if (links.isEmpty() && hasAssertionTemplate && graph != Graph.ASSERTION) {
                findings.add(note(Kind.MISSING_TEMPLATE_LINK, "The nanopublication names a template for its assertion, but none for its " + wanted));
            }
            for (IRI link : links) {
                String kind = getTemplateKind(link.stringValue());
                if (kind == null) {
                    findings.add(note(Kind.UNCHECKED, "The template <" + link + "> could not be retrieved, so its kind is not checked"));
                } else if (kind.isEmpty()) {
                    findings.add(error(Kind.WRONG_TEMPLATE_KIND, "The link to the " + wanted + " template names <" + link + ">, which is not a template"));
                } else if (!kind.equals(wanted)) {
                    findings.add(error(Kind.WRONG_TEMPLATE_KIND, "The link to the " + wanted + " template names <" + link + ">, which is a template for the " + kind));
                }
            }
        }
    }

    private static String getName(Graph graph) {
        return switch (graph) {
            case ASSERTION -> "assertion";
            case PROVENANCE -> "provenance";
            case PUBINFO -> "publication info";
        };
    }

    // The kind of the template a link names: "assertion", "provenance" or "publication
    // info"; "" for something that is no template; null if it could not be retrieved.
    private String getTemplateKind(String link) {
        if (templateKinds.containsKey(link)) return templateKinds.get(link);
        Nanopub templateNanopub = retrieve(link);
        if (templateNanopub == null) return null;
        String kind;
        try {
            Template template = new Template(templateNanopub, NanopubResolver.NONE);
            kind = getName(template.isProvenanceTemplate() ? Graph.PROVENANCE : template.isPubinfoTemplate() ? Graph.PUBINFO : Graph.ASSERTION);
        } catch (MalformedTemplateException | RuntimeException ex) {
            kind = "";
        }
        templateKinds.put(link, kind);
        return kind;
    }

    // ---------- 3: a governing space maintains what it governs ----------

    // What is known about whether a space governs this nanopublication.
    private enum Governance {NONE, IN_EFFECT, WITHOUT_EFFECT, UNKNOWN}

    private Governance checkGovernance(Nanopub nanopub, Map<IRI, Nanopub> superseded, List<NetworkFinding> findings) {
        Set<IRI> embedded = getObjects(nanopub, NPX.EMBEDS);
        String uri = nanopub.getUri().stringValue();
        boolean published = TrustyUriUtils.isPotentialTrustyUri(uri);
        Governance governance = Governance.NONE;
        Set<Resource> nodes = new LinkedHashSet<>();
        for (Statement st : nanopub.getAssertion()) {
            if (st.getPredicate().equals(KPXL.GOVERNED_BY)) nodes.add(st.getSubject());
        }
        for (Resource node : nodes) {
            Set<Value> kinds = getObjects(nanopub.getAssertion(), node, DCTERMS.IS_VERSION_OF);
            for (Value space : getObjects(nanopub.getAssertion(), node, KPXL.GOVERNED_BY)) {
                String effect = "so gen:governedBy has no effect";
                if (kinds.isEmpty()) {
                    findings.add(note(Kind.GOVERNANCE_WITHOUT_EFFECT, "<" + node + "> names <" + space + "> as governing it, but says with no dct:isVersionOf what it is a version of, " + effect));
                    governance = worse(governance, Governance.WITHOUT_EFFECT);
                    continue;
                }
                if (!embedded.contains(node)) {
                    findings.add(note(Kind.GOVERNANCE_WITHOUT_EFFECT, "<" + node + "> names <" + space + "> as governing it, but the nanopublication does not declare it with npx:embeds, " + effect));
                    governance = worse(governance, Governance.WITHOUT_EFFECT);
                    continue;
                }
                for (Value kind : kinds) {
                    Governance one = checkGovernedKind(kind, space, uri, published, superseded, findings);
                    governance = governance == Governance.NONE ? one : worse(governance, one);
                }
            }
        }
        return governance;
    }

    private Governance checkGovernedKind(Value kind, Value space, String uri, boolean published, Map<IRI, Nanopub> superseded, List<NetworkFinding> findings) {
        String effect = "so gen:governedBy has no effect";
        for (Map.Entry<IRI, Nanopub> e : superseded.entrySet()) {
            if (e.getValue() == null) continue;
            Set<Value> earlierKinds = getObjects(e.getValue().getAssertion(), null, DCTERMS.IS_VERSION_OF);
            if (!earlierKinds.isEmpty() && !earlierKinds.contains(kind)) {
                findings.add(note(Kind.GOVERNANCE_WITHOUT_EFFECT, "<" + kind + "> is not what the superseded nanopublication <" + e.getKey() + "> is a version of (" + list(earlierKinds)
                        + "). A new version keeps that IRI; with another one it starts a line of its own, which the space does not govern together with the earlier versions"));
                return Governance.WITHOUT_EFFECT;
            }
        }
        if (kind.stringValue().startsWith(uri) && !published) {
            findings.add(note(Kind.GOVERNANCE_WITHOUT_EFFECT, "<" + kind + "> is minted by this nanopublication, so <" + space
                    + "> can only come to maintain it once it is published; until then gen:governedBy has no effect"));
            return Governance.WITHOUT_EFFECT;
        }
        Set<String> spaces;
        try {
            spaces = maintainers.getMaintainingSpaces(kind.stringValue());
        } catch (Exception ex) {
            spaces = null;
        }
        if (spaces == null) {
            findings.add(note(Kind.UNCHECKED, "Whether <" + space + "> maintains <" + kind + "> could not be found out, so it is not checked whether gen:governedBy has an effect"));
            return Governance.UNKNOWN;
        }
        if (spaces.contains(space.stringValue())) return Governance.IN_EFFECT;
        String elsewhere = spaces.isEmpty() ? "" : " (it is maintained by " + spaces.stream().sorted().map(s -> "<" + s + ">").collect(Collectors.joining(", ")) + ")";
        findings.add(note(Kind.GOVERNANCE_WITHOUT_EFFECT, "<" + space + "> does not maintain <" + kind + ">" + elsewhere + ", " + effect
                + ": new versions are found only along npx:supersedes. An admin of the space can declare the resource as maintained"));
        return Governance.WITHOUT_EFFECT;
    }

    // Of two things known about governance, the one that weighs more for whether a new
    // version depends on its key: without effect, then unknown, then in effect.
    private static Governance worse(Governance a, Governance b) {
        if (a == Governance.WITHOUT_EFFECT || b == Governance.WITHOUT_EFFECT) return Governance.WITHOUT_EFFECT;
        if (a == Governance.UNKNOWN || b == Governance.UNKNOWN) return Governance.UNKNOWN;
        return a == Governance.NONE ? b : a;
    }

    // ---------- 4: a new version is signed with the key of the old one ----------

    private void checkKey(Nanopub nanopub, Map<IRI, Nanopub> superseded, Governance governance, List<NetworkFinding> findings) {
        if (superseded.isEmpty() || governance == Governance.IN_EFFECT) return;
        String key = getPublicKey(nanopub);
        if (key == null) {
            findings.add(note(Kind.UNCHECKED, "The nanopublication is not signed, so it is not checked whether it is signed with the key of the nanopublication it supersedes."
                    + " It takes the place of that one only if it is"));
            return;
        }
        for (Map.Entry<IRI, Nanopub> e : superseded.entrySet()) {
            if (e.getValue() == null) continue;
            String oldKey = getPublicKey(e.getValue());
            if (oldKey == null) {
                findings.add(note(Kind.UNCHECKED, "The key of the superseded nanopublication <" + e.getKey() + "> could not be read, so it is not checked whether this version is signed with it"));
            } else if (!oldKey.equals(key)) {
                if (governance == Governance.UNKNOWN) {
                    findings.add(note(Kind.UNCHECKED, "The nanopublication is signed with another key than <" + e.getKey()
                            + "> that it supersedes. It takes the place of that one only if a space governs it, which could not be found out"));
                } else {
                    findings.add(error(Kind.OTHER_KEY, "The nanopublication is signed with another key than <" + e.getKey() + "> that it supersedes, and no space governs it."
                            + " npx:supersedes has an effect only between nanopublications signed with the same key, so this one does not take the place of the other"));
                }
            }
        }
    }

    private static String getPublicKey(Nanopub nanopub) {
        try {
            NanopubSignatureElement signature = SignatureUtils.getSignatureElement(nanopub);
            if (signature == null) signature = LegacySignatureUtils.getSignatureElement(nanopub);
            return signature == null ? null : signature.getPublicKeyString();
        } catch (MalformedCryptoElementException | RuntimeException ex) {
            return null;
        }
    }

    // ---------- helpers ----------

    private Nanopub retrieve(String id) {
        try {
            return resolver.getNanopub(id);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    // The IRIs the publication info gives for the nanopublication with a predicate, in order.
    private static Set<IRI> getObjects(Nanopub nanopub, IRI predicate) {
        Set<IRI> objects = new TreeSet<>((a, b) -> a.stringValue().compareTo(b.stringValue()));
        for (Statement st : nanopub.getPubinfo()) {
            if (st.getSubject().equals(nanopub.getUri()) && st.getPredicate().equals(predicate) && st.getObject() instanceof IRI iri) objects.add(iri);
        }
        return objects;
    }

    private static Set<Value> getObjects(Set<Statement> statements, Resource subject, IRI predicate) {
        Set<Value> objects = new TreeSet<>((a, b) -> a.stringValue().compareTo(b.stringValue()));
        for (Statement st : statements) {
            if ((subject == null || st.getSubject().equals(subject)) && st.getPredicate().equals(predicate)) objects.add(st.getObject());
        }
        return objects;
    }

    private static String list(java.util.Collection<? extends Value> values) {
        return values.stream().map(v -> "<" + v.stringValue() + ">").collect(Collectors.joining(", "));
    }

    private static NetworkFinding error(Kind kind, String message) {
        return new NetworkFinding(Severity.ERROR, kind, message);
    }

    private static NetworkFinding note(Kind kind, String message) {
        return new NetworkFinding(Severity.NOTE, kind, message);
    }

}
