package org.nanopub.vocabulary;

import org.eclipse.rdf4j.model.IRI;

public final class KPXL {

    private KPXL() {
    }

    public static final String NAMESPACE = "https://w3id.org/kpxl/gen/terms/";

    /**
     * <a href="https://w3id.org/kpxl/gen/terms/RoCrateNanopub">https://w3id.org/kpxl/gen/terms/RoCrateNanopub</a>
     */
    public static final IRI RO_CRATE_NANOPUB = VocabUtils.createIRI(NAMESPACE, "RoCrateNanopub");

    public static final IRI RO_CRATE_BOT = VocabUtils.createIRI(NAMESPACE, "RoCrateBot");

    /**
     * Links a version of a definition (such as a template or a view) to the space that
     * governs it. A governed version is resolved by space membership rather than by
     * following its supersedes chain.
     */
    public static final IRI GOVERNED_BY = VocabUtils.createIRI(NAMESPACE, "governedBy");

    /**
     * Class of role properties whose triples point from the space to the agent. Declared as
     * the type of a role predicate to pin its direction.
     */
    public static final IRI INVERSE_ROLE_PROPERTY = VocabUtils.createIRI(NAMESPACE, "InverseRoleProperty");

    /**
     * Class of role properties whose triples point from the agent to the space. Declared as
     * the type of a role predicate to pin its direction.
     */
    public static final IRI REGULAR_ROLE_PROPERTY = VocabUtils.createIRI(NAMESPACE, "RegularRoleProperty");

    /**
     * Type of a nanopublication that grants a role to an agent in a space.
     */
    public static final IRI ROLE_INSTANTIATION = VocabUtils.createIRI(NAMESPACE, "RoleInstantiation");

}
