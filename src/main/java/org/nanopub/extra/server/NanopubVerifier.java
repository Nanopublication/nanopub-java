package org.nanopub.extra.server;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.nanopub.Nanopub;
import org.nanopub.NanopubImpl;
import org.nanopub.NanopubUtils;
import org.nanopub.extra.security.MalformedCryptoElementException;
import org.nanopub.extra.security.NanopubSignatureElement;
import org.nanopub.extra.security.SignatureUtils;
import org.nanopub.vocabulary.NTEMPLATE;

import java.util.*;

/**
 * Verifies if a technically fine nanopublication meets some standards or best practices.
 */
public class NanopubVerifier {
    private final List<String> issues = new ArrayList<>();
    private final Nanopub nanopub;

    public NanopubVerifier(Nanopub nanopub) {
        this.nanopub = nanopub;
    }

    public List<String> getIssues() {
        return issues;
    }

    /**
     * @return true, iff there were no problems
     */
    public boolean verify() {
        checkTimestamp();
        checkHasLabel();
        checkHasType();
        checkTemplate();
        checkSigner();
        checkGraph();
        checkTripleCount();
        checkByteCount();
        checkUriProtocol();

        return issues.isEmpty();
    }

    /**
     * Check if the triple size is not greater than 1200
     */
    private void checkTripleCount() {
        if (nanopub.getTripleCount() > 1200) {
            issues.add("Triple count exceeds maximum of 1200. count = " + nanopub.getTripleCount());
        }
    }

    private void checkUriProtocol() {
        Set<Statement> allStatements = new HashSet<>();
        allStatements.addAll(nanopub.getHead());
        allStatements.addAll(nanopub.getAssertion());
        allStatements.addAll(nanopub.getProvenance());
        allStatements.addAll(nanopub.getPubinfo());

        for (Statement st : allStatements) {
            if (!isHttpOrHttps(st.getSubject())) {
                issues.add("Invalid URI protocol: " + st.getSubject().stringValue());
            }
            if (!isHttpOrHttps(st.getPredicate())) {
                issues.add("Invalid URI protocol: " + st.getPredicate().stringValue());
            }
            if (st.getObject() instanceof IRI) {
                if (!isHttpOrHttps(((IRI) st.getObject()))) {
                    issues.add("Invalid URI protocol: " + st.getObject().stringValue());
                }
            }
        }
    }

    private boolean isHttpOrHttps(Resource uri) {
        return uri.stringValue().startsWith("https://") || uri.stringValue().startsWith("http://");
    }

    /**
     * Check if the byte size is not greater than 10 MB
     */
    private void checkByteCount() {
        if (nanopub.getByteCount() > 10*1024*1024) {
            issues.add("Byte count exceeds maximum of 10MB. count = " + nanopub.getByteCount());
        }
    }

    /**
     * Check if the creation Time is not in the future and not older than 1 hour.
     */
    private void checkTimestamp() {
        Calendar creationTime = nanopub.getCreationTime();
        if (creationTime == null) {
            issues.add("Nanopub has no creation time.");
            return;
        }
        long now = new Date().getTime();

        if (creationTime.getTimeInMillis() > now) {
            issues.add("Nanopub creation time is in the future.");
            return;
        }

        long oneHourBeforeNow = now - (60 * 60 * 1000);
        if (creationTime.getTimeInMillis() < oneHourBeforeNow) {
            issues.add("Nanopub creation time is older than one hour." );
        }
    }

    private void checkHasLabel() {
        String label = NanopubUtils.getLabel(nanopub);
        if  (label == null) {
            issues.add("Nanopub has no label." );
        }
    }

    /**
     * Check if the Nanopub has a type.
     */
    private void checkHasType() {
        if (NanopubUtils.getTypes(nanopub).isEmpty()) {
            issues.add("Nanopub has no types." );
        }
    }

    /**
     * Check if there is an assertion template and a provenance template.
     */
    private void checkTemplate() {
        boolean hasAssertionTemplate = false;
        boolean hasProvenanceTemplate = false;

        for (Statement st : nanopub.getPubinfo()) {
            if (!(st.getObject() instanceof IRI)) continue;
            IRI pred = st.getPredicate();
            if (pred.equals(NTEMPLATE.WAS_CREATED_FROM_TEMPLATE)) {
                hasAssertionTemplate = true;
            } else if (pred.equals(NTEMPLATE.WAS_CREATED_FROM_PROVENANCE_TEMPLATE)) {
                hasProvenanceTemplate = true;
            }
        }

        if (!hasAssertionTemplate) {
            issues.add("Nanopub has no assertion template." );
        }
        if (!hasProvenanceTemplate) {
            issues.add("Nanopub has no provenance template." );
        }
    }

    /**
     * Check if there is a Signer and if the Signer is also a Creator.
     */
    private void checkSigner() {
        NanopubImpl ni = (NanopubImpl) nanopub;
        nanopub.getCreators();

        NanopubSignatureElement se = null;
        try {
            se = SignatureUtils.getSignatureElement(nanopub);
            if (se == null) {
                issues.add("Nanopub has no signature element.");
                return;
            }
            Set<IRI> signers = se.getSigners();
            if (signers == null || signers.isEmpty()) {
                issues.add("Nanopub has no signer." );
            } else if (signers.size() > 1) {
                issues.add("Nanopub has more than one signer." );
            } else {
                IRI signer = signers.iterator().next();
                if (!nanopub.getCreators().contains(signer)) {
                    issues.add("The signer is not a creator." );
                }
            }
        } catch (MalformedCryptoElementException e) {
            issues.add("Nanopub has no signature element.");
        }
    }

    /**
     * Check if we have only expected graph uris.
     */
    private void checkGraph() {
        String sub = nanopub.getUri() + "/";
        for (IRI st : nanopub.getGraphUris()) {
            String graphUri = st.stringValue();
            if (!(graphUri.startsWith(sub + "Head") ||
                    graphUri.startsWith(sub + "assertion") ||
                    graphUri.startsWith(sub + "provenance") ||
                    graphUri.startsWith(sub + "pubinfo"))
            ) {
                issues.add("Unexpected graph uri: " + graphUri);
            }
        }
    }

}
