package org.nanopub.op.fingerprint;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.nanopub.Nanopub;

public interface FingerprintHandler {

	public static final IRI nanopubUriPlaceholder = SimpleValueFactory.getInstance().createIRI("http://purl.org/nanopub/placeholders/nanopuburi");
	public static final IRI headUriPlaceholder = SimpleValueFactory.getInstance().createIRI("http://purl.org/nanopub/placeholders/head");
	public static final IRI assertionUriPlaceholder = SimpleValueFactory.getInstance().createIRI("http://purl.org/nanopub/placeholders/assertion");
	public static final IRI provUriPlaceholder = SimpleValueFactory.getInstance().createIRI("http://purl.org/nanopub/placeholders/provenance");
	public static final IRI pubinfoUriPlaceholder = SimpleValueFactory.getInstance().createIRI("http://purl.org/nanopub/placeholders/pubinfo");
	public static final IRI timestampPlaceholder = SimpleValueFactory.getInstance().createIRI("http://purl.org/nanopub/placeholders/timestamp");

	public String getFingerprint(Nanopub np);

}