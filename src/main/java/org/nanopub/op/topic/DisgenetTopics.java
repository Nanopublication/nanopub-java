package org.nanopub.op.topic;

import org.nanopub.Nanopub;

public class DisgenetTopics extends DefaultTopics {

	public DisgenetTopics() {
		super("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
	}

	@Override
	public String getTopic(Nanopub np) {
		String t = super.getTopic(np);
		t = t.replaceFirst("^http://rdf.disgenet.org/gene-disease-association.ttl#", "http://rdf.disgenet.org/resource/gda/");
		return t;
	}

}
