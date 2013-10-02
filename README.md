nanopub-java
============

This is a Java library for nanopublications based on Sesame.


Formal Structure of a Nanopub
-----------------------------

This library interprets the [official guidelines](http://nanopub.org/guidelines/working_draft/),
which are still work in progress and sometimes vague, in a way that leads to the following
well-formedness criteria:

- A nanopub consists of a set of RDF quads (i.e. triples with a context URI)
- The context (i.e. graph) of each triple has to be specified (no null values)
- There is exactly one quad of the form '[N] rdf:type np:Nanopublication [H]', whereby [N] is
  identified as the nanopub URI and [H] as the head URI (these two may be the same URI)
- There is exactly one quad of the form '[N] hasAssertion [A] [H]', where [N] and [H] are the
  nanopub and head URIs, respectively, and whereby [A] is identified as the assertion URI
- In the same way, there is exactly one quad of the form '[N] hasProvenance [P] [H]', whereby
  [P] is identified as the provenance URI
- In the same way, there is exactly one quad of the form '[N] hasPublicationInfo [I] [H]',
  whereby [I] is identified as the publication info URI
- Quads of the form '[S] rdfg:subgraphof [G] [H]' where [H] is the head URI and [G] is one
  of assertion, provenance, or publication info URI define a subgraph [S] of the respective
  graph
- All triples have to belong to head, assertion, provenance, publication info, or one of their
  subgraphs; triples that have a graph URI that is different from these are not allowed


License
-------

nanopub-java is free software under the MIT License. See LICENSE.txt.
