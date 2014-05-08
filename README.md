nanopub-java
============

This is a Java library for nanopublications based on Sesame.


Usage
-----

The easiest way to use this library in your project is to let Maven download it from The Central
Repository. Just include the following lines in your `pom.xml` file:

    <dependency>
      <groupId>org.nanopub</groupId>
      <artifactId>nanopub</artifactId>
      <version>1.0</version>
    </dependency>


Formal Structure of a Nanopub
-----------------------------

According to the [official guidelines](http://nanopub.org/guidelines/working_draft/),
these are the well-formedness criteria for nanopublications:

1.  A nanopublication consists of a set of RDF quads (i.e. subject-predicate-object + context)
2.  The context (i.e. graph) of each triple has to be specified as a valid URI (i.e. no null values)
3.  There is exactly one quad of the form '[N] rdf:type np:Nanopublication [H]', which identifies
    [N] as the nanopublication URI, and [H] as the head URI
4.  Given the nanopublication URI [N] and its head URI [H], there is exactly one quad of the form
    '[N] np:hasAssertion [A] [H]', which identifies [A] as the assertion URI
5.  Given the nanopublication URI [N] and its head URI [H], there is exactly one quad of the form
    '[N] np:hasProvenance [P] [H]', which identifies [P] as the provenance URI
6.  Given the nanopublication URI [N] and its head URI [H], there is exactly one quad of the form
    '[N] np:hasPublicationInfo [I] [H]', which identifies [I] as the publication information URI
7.  Given the head URI [H], there are zero or more quads of the form '[S] rdfg:subGraphOf [G] [H]'
    introducing a super-graph [G], where [S] is one of [H] or [A] or [P] or [I]
8.  Given the head URI [H], there are zero or more quads of the form '[G] rdfg:subGraphOf [S] [H]'
    introducing a sub-graph [G], where [S] is one of [A] or [P] or [I]
9.  There are no other quads in the context [H]
10. The URIs for [N], [H], [A], [P], [I], and the introduced sub-graphs must all be different
11. All triples must be placed in one of [H] or [A] or [P] or [I] or one of the introduced
    sub-graphs
12. Triples in [P] must refer to [A] or one of the introduced super-graph/sub-graph of [A].
13. Triples in [I] must refer to [N] or one of the introduced super-graph/sub-graph of [N].


This library does not currently check points 12 and 13.


Compilation and Dependencies
----------------------------

Maven has to be installed to compile the code:

    $ mvn clean package

To use the Trusty-URI features, trustyuri-java has to be installed (loaded dynamically). Either
uncomment the respective dependency in the pom.xml file, or manually install the latest version:

    $ git clone git@github.com:trustyuri/trustyuri-java.git
    $ cd trustyuri-java
    $ mvn install


Developers
----------

- Tobias Kuhn (http://www.tkuhn.ch)


License
-------

nanopub-java is free software under the MIT License. See LICENSE.txt.
