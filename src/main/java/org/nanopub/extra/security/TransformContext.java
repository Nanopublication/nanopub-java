package org.nanopub.extra.security;

import net.trustyuri.TrustyUriUtils;
import net.trustyuri.rdf.RdfFileContent;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.nanopub.NanopubProfile;
import org.nanopub.trusty.CrossRefResolver;

import java.security.KeyPair;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static org.nanopub.extra.security.SignatureAlgorithm.RSA;

/**
 * Context for transformations that require a signature, such as signing a nanopub or making it trustworthy.
 */
public class TransformContext {
    /**
     * Constant <code>DEFAULT_KEY_PATH="~/.nanopub/id_rsa"</code>
     */
    public static final String DEFAULT_KEY_PATH = "~/.nanopub/id_rsa";

    // TODO: Use this also for MakeTrustyNanopub

    private static ValueFactory vf = SimpleValueFactory.getInstance();

    /**
     * Creates a default TransformContext.
     *
     * @return a TransformContext with the default RSA algorithm, a key loaded from ~/.nanopub/id_rsa, and the signer IRI from the NanopubProfile.
     */
    public static TransformContext makeDefault() {
        IRI signerIri = null;
        NanopubProfile profile = new NanopubProfile(NanopubProfile.IMPLICIT_PROFILE_FILE_NAME);
        if (profile.getOrcidId() != null) {
            signerIri = vf.createIRI(profile.getOrcidId());
        }
        KeyPair key = null;
        try {
            key = SignNanopub.loadKey(DEFAULT_KEY_PATH, RSA);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return new TransformContext(RSA, key, signerIri, false, false, false);
    }

    private SignatureAlgorithm algorithm;
    private KeyPair key;
    private IRI signer;
    private Map<Resource, IRI> tempRefMap;
    private Map<String, String> tempPrefixMap;
    private boolean ignoreSigned;

    /**
     * Creates a TransformContext with the specified parameters.
     *
     * @param algorithm                   the signature algorithm to use
     * @param key                         the key pair to use for signing
     * @param signer                      the IRI of the signer
     * @param resolveCrossRefs            whether to resolve cross-references in the nanopub
     * @param resolveCrossRefsPrefixBased whether to resolve cross-references based on prefixes
     * @param ignoreSigned                whether to ignore signed statements in the nanopub
     */
    public TransformContext(SignatureAlgorithm algorithm, KeyPair key, IRI signer, boolean resolveCrossRefs, boolean resolveCrossRefsPrefixBased, boolean ignoreSigned) {
        this.algorithm = algorithm;
        this.key = key;
        this.signer = signer;
        this.ignoreSigned = ignoreSigned;
        if (resolveCrossRefsPrefixBased) {
            tempPrefixMap = new HashMap<>();
            tempRefMap = new HashMap<>();
        } else if (resolveCrossRefs) {
            tempRefMap = new HashMap<>();
        }
    }

    /**
     * Returns the signature algorithm used in this context.
     *
     * @return the signature algorithm
     */
    public SignatureAlgorithm getSignatureAlgorithm() {
        return algorithm;
    }

    /**
     * Returns the key pair used for signing.
     *
     * @return the key pair
     */
    public KeyPair getKey() {
        return key;
    }

    /**
     * Returns the IRI of the signer.
     *
     * @return the IRI of the signer
     */
    public IRI getSigner() {
        return signer;
    }

    /**
     * Returns true if to ignore signed is enabled.
     *
     * @return true if ignore signed is enabled, false otherwise
     */
    public boolean isIgnoreSignedEnabled() {
        return ignoreSigned;
    }

    /**
     * Returns a map of temporary references.
     *
     * @return a map of temporary references
     */
    public Map<Resource, IRI> getTempRefMap() {
        return tempRefMap;
    }

    /**
     * Returns a map of temporary prefixes.
     *
     * @return a map of temporary prefixes, where the key is a string representing the prefix and the value is the corresponding IRI
     */
    public Map<String, String> getTempPrefixMap() {
        return tempPrefixMap;
    }

    /**
     * Resolves cross-references in the given RdfFileContent.
     *
     * @param input the RdfFileContent to resolve cross-references in
     * @return a new RdfFileContent with resolved cross-references, or the input if no temporary reference map is available
     */
    public RdfFileContent resolveCrossRefs(RdfFileContent input) {
        if (tempRefMap == null) return input;
        RdfFileContent output = new RdfFileContent(RDFFormat.TRIG);
        input.propagate(new CrossRefResolver(tempRefMap, tempPrefixMap, output));
        return output;
    }

    /**
     * Merges the given map of transformations into the temporary reference map and prefix map.
     *
     * @param map the map of transformations to merge, where the key is a Resource and the value is an IRI
     */
    public void mergeTransformMap(Map<Resource, IRI> map) {
        if (map == null) return;
        if (tempRefMap != null) {
            for (Resource r : new HashSet<>(tempRefMap.keySet())) {
                IRI v = tempRefMap.get(r);
                if (map.containsKey(v)) {
                    tempRefMap.put(r, map.get(v));
                    map.remove(v);
                }
            }
            for (Resource r : map.keySet()) {
                tempRefMap.put(r, map.get(r));
            }
        }
        if (tempPrefixMap != null) {
            for (Resource r : map.keySet()) {
                if (r instanceof IRI && TrustyUriUtils.isPotentialTrustyUri(map.get(r).stringValue())) {
                    tempPrefixMap.put(r.stringValue(), map.get(r).stringValue());
                }
            }
        }
    }

}
