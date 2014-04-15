nanopub-java
============

This is a Java library for nanopublications based on Sesame.


Formal Structure of a Nanopub
-----------------------------

According to the [official guidelines](http://nanopub.org/guidelines/working_draft/),
which are still work in progress, these are the well-formedness criteria for nanopublications:

1.  A nanopublication consists of a set of RDF quads (i.e. subject-predicate-object + context)
2.  The context (i.e. graph) of each triple has to be specified as a valid URI (i.e. no null
    values)
3.  There is exactly one quad of the form '[N] rdf:type np:Nanopublication [H]', where [N] is the
    nanopublication URI, and [H] is the head URI
4.  There is exactly one quad of the form '[N] np:hasAssertion [A] [H]', where [N] is the
    nanopublication URI, [A] is the assertion URI, and [H] is the head URI
5.  There is exactly one quad of the form '[N] np:hasProvenance [P] [H]', where [N] is the
    nanopublication URI, [P] is the provenance URI, and [H] is the head URI
6.  There is exactly one quad of the form '[N] np:hasPublicationInfo [I] [H]', [N] is the
    nanopublication URI, [I] is the nanopublication information URI, and [H] is the head URI
7.  There are zero or more quads of the form '[S] rdfg:subGraphOf [G] [H]' introducing a
    super-graph [G], where [H] is the head URI and [S] is one of [H] or [A] or [P] or [I]
8.  There are zero or more quads of the form '[G] rdfg:subGraphOf [S] [H]' introducing a sub-graph
    [G], where [H] is the head URI and [S] is one of [A] or [P] or [I]
9.  There are no other quads in the context [H]
10. The URIs for [N] and [H] may be the same.
11. The URIs for [H], [A], [P], [I], and the introduced sub-graphs must all be different
12. Triples must be placed in one of [H] or [A] or [P] or [I] or one of the introduced sub-graphs
13. Triples in [P] must refer to [A] or a supergraph of [A].
14. Triples in [I] must refer to [N] or a supergraph of [N].

This library does not currently check points 9, 13, and 14.


Dependencies
------------

Maven has to be installed.

Installation of trustyuri-java:

    $ git clone git@github.com:trustyuri/trustyuri-java.git
    $ cd trustyuri-java
    $ mvn install


Compilation and Execution
-------------------------

Compile and package with Maven:

    $ mvn clean package


Developers
----------

- Tobias Kuhn (http://www.tkuhn.ch)


License
-------

nanopub-java is free software under the MIT License. See LICENSE.txt.
