package org.nanopub.extra.server;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.datatypes.XMLDatatypeUtil;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.nanopub.Nanopub;
import org.nanopub.NanopubImpl;
import org.nanopub.NanopubUtils;
import org.nanopub.SimpleTimestampPattern;
import org.nanopub.extra.security.MalformedCryptoElementException;
import org.nanopub.extra.security.NanopubSignatureElement;
import org.nanopub.extra.security.SignatureUtils;
import org.nanopub.vocabulary.NTEMPLATE;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Verifies if a technically fine nanopublication meets some standards or best practices.
 */
public class NanopubVerifier {

    private static final Logger logger = LoggerFactory.getLogger(NanopubVerifier.class);

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
        checkLiteralDatatypes();

        if (issues.isEmpty()) {
            logger.debug("Nanopub {} passed verification with no issues", nanopub.getUri());
        } else {
            logger.debug("Nanopub {} has {} verification issue(s): {}", nanopub.getUri(), issues.size(), issues);
        }
        return issues.isEmpty();
    }

    private Set<Statement> getAllStatements() {
        Set<Statement> allStatements = new HashSet<>();
        allStatements.addAll(nanopub.getHead());
        allStatements.addAll(nanopub.getAssertion());
        allStatements.addAll(nanopub.getProvenance());
        allStatements.addAll(nanopub.getPubinfo());
        return allStatements;
    }

    /**
     * Check that the value of each literal is valid for the datatype it declares. Nanopubs with
     * malformed literals can be published, but are rejected by strict RDF stores and therefore end up
     * being unavailable through the SPARQL endpoint.
     */
    private void checkLiteralDatatypes() {
        for (Statement st : getAllStatements()) {
            if (!(st.getObject() instanceof Literal l)) continue;
            IRI datatype = l.getDatatype();
            // only XML Schema datatypes have a lexical space we can check here
            if (!XMLDatatypeUtil.isBuiltInDatatype(datatype)) continue;
            if (!XMLDatatypeUtil.isValidValue(l.getLabel(), datatype)) {
                issues.add("Invalid value for datatype " + datatype.stringValue() + ": \"" + l.getLabel() +
                        "\" (as object of " + st.getPredicate().stringValue() + ")");
            }
        }
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
        for (Statement st : getAllStatements()) {
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
            issues.add(getMissingTimestampIssue());
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

    /**
     * A creation time that does not declare the datatype xsd:dateTime is ignored when the timestamp is
     * read, and is therefore reported as its own issue rather than as a missing creation time.
     */
    private String getMissingTimestampIssue() {
        for (Statement st : nanopub.getPubinfo()) {
            if (!st.getSubject().equals(nanopub.getUri())) continue;
            if (!SimpleTimestampPattern.isCreationTimeProperty(st.getPredicate())) continue;
            if (st.getObject() instanceof Literal l && !l.getDatatype().equals(XSD.DATETIME)) {
                return "Nanopub creation time has datatype " + l.getDatatype().stringValue() + " instead of xsd:dateTime.";
            }
        }
        return "Nanopub has no creation time.";
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
            // Reported to the caller as a missing signature element; the actual cause is only visible here.
            logger.debug("Signature element of nanopub {} is malformed: {}", nanopub.getUri(), e.getMessage(), e);
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
