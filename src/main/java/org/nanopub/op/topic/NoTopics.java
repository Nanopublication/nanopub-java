package org.nanopub.op.topic;

import org.nanopub.Nanopub;
import org.nanopub.op.Topic.TopicHandler;

public class NoTopics implements TopicHandler {

	@Override
	public String getTopic(Nanopub np) {
		return np.getUri().stringValue();
	}

}
