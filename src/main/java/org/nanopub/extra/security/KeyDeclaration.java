package org.nanopub.extra.security;

import java.util.LinkedHashSet;
import java.util.Set;

import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;

public class KeyDeclaration extends CryptoElement {

	private static final long serialVersionUID = 1L;

	public static final URI DECLARED_BY = new URIImpl("http://purl.org/nanopub/x/declaredBy");

	private Set<URI> declarers = new LinkedHashSet<>();

	KeyDeclaration(URI uri) {
		super(uri);
	}

	void addDeclarer(URI declarer) {
		declarers.add(declarer);
	}

	public Set<URI> getDeclarers() {
		return declarers;
	}

	public boolean hasDeclarer(URI declarer) {
		return declarers.contains(declarer);
	}

}
