package org.nanopub.extra.security;

import java.security.KeyPair;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.nanopub.trusty.CrossRefResolver;

import net.trustyuri.TrustyUriUtils;
import net.trustyuri.rdf.RdfFileContent;

public class TransformContext {

	// TODO: Use this also for MakeTrustyNanopub

	private SignatureAlgorithm algorithm;
	private KeyPair key;
	private IRI signer;
	private Map<Resource,IRI> tempRefMap;
	private Map<String,String> tempPrefixMap;

	public TransformContext(SignatureAlgorithm algorithm, KeyPair key, IRI signer, boolean resolveCrossRefs, boolean resolveCrossRefsPrefixBased) {
		this.algorithm = algorithm;
		this.key = key;
		this.signer = signer;
		if (resolveCrossRefsPrefixBased) {
			tempPrefixMap = new HashMap<>();
			tempRefMap = new HashMap<>();
		} else if (resolveCrossRefs) {
			tempRefMap = new HashMap<>();
		}
	}

	public SignatureAlgorithm getSignatureAlgorithm() {
		return algorithm;
	}

	public KeyPair getKey() {
		return key;
	}

	public IRI getSigner() {
		return signer;
	}

	public Map<Resource,IRI> getTempRefMap() {
		return tempRefMap;
	}

	public Map<String,String> getTempPrefixMap() {
		return tempPrefixMap;
	}

	public RdfFileContent resolveCrossRefs(RdfFileContent input) {
		if (tempRefMap == null) return input;
		RdfFileContent output = new RdfFileContent(RDFFormat.TRIG);
		input.propagate(new CrossRefResolver(tempRefMap, tempPrefixMap, output));
		return output;
	}

	public void mergeTransformMap(Map<Resource,IRI> map) {
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
