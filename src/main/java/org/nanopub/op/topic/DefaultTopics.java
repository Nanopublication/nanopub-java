package org.nanopub.op.topic;

import java.util.HashMap;
import java.util.Map;

import org.nanopub.Nanopub;
import org.nanopub.op.Topic.TopicHandler;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;

public class DefaultTopics implements TopicHandler {

	private Map<String,Boolean> ignore = new HashMap<>();

	public DefaultTopics(String ignoreProperties) {
		if (ignoreProperties != null) {
			for (String s : ignoreProperties.trim().split("\\|")) {
				if (!s.isEmpty()) ignore.put(s, true);
			}
		}
	}

	@Override
	public String getTopic(Nanopub np) {
		Map<Resource,Integer> resourceCount = new HashMap<>();
		for (Statement st : np.getAssertion()) {
			Resource subj = st.getSubject();
			if (subj.equals(np.getUri())) continue;
			if (ignore.containsKey(st.getPredicate().stringValue())) continue;
			if (!resourceCount.containsKey(subj)) resourceCount.put(subj, 0);
			resourceCount.put(subj, resourceCount.get(subj) + 1);
		}
		int max = 0;
		Resource topic = null;
		for (Resource r : resourceCount.keySet()) {
			int c = resourceCount.get(r);
			if (c > max) {
				topic = r;
				max = c;
			} else if (c == max) {
				topic = null;
			}
		}
		return topic + "";
	}

}
