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
      <version>1.2</version>
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
7.  The URIs for [N], [H], [A], [P], [I] must all be different
8.  All triples must be placed in one of [H] or [A] or [P] or [I]
9.  Triples in [P] have at least one reference to [A]
10. Triples in [I] have at least one reference to [N]

All these criteria are checked by this library.


Compilation and Dependencies
----------------------------

Maven has to be installed to compile the code:

    $ mvn clean package

Snapshot versions of this code might require the latest snapshot of the trustyuri library:

    $ git clone git@github.com:trustyuri/trustyuri-java.git
    $ cd trustyuri-java
    $ mvn install


Developers
----------

- Tobias Kuhn (http://www.tkuhn.ch)


License
-------

nanopub-java is free software under the MIT License. See LICENSE.txt.
