nanopub-java
============

This is a Java library for nanopublications (see http://nanopub.org) based on
Sesame.


Publication
-----------

- Tobias Kuhn.  [nanopub-java: A Java Library for
  Nanopublications](http://arxiv.org/pdf/1508.04977.pdf). In Proceedings of the
  5th Workshop on Linked Science (LISC 2015). 2015.


Usage
-----

The easiest way to use this library in your project is to let Maven download it
from The Central Repository. Just include the following lines in your `pom.xml`
file:

    <dependency>
      <groupId>org.nanopub</groupId>
      <artifactId>nanopub</artifactId>
      <version>1.8</version>
    </dependency>


Formal Structure of a Nanopub
-----------------------------

This library implements the formal restrictions defined in the [official
nanopublication guidelines](http://nanopub.org/guidelines/working_draft/).


Compilation and Installation
----------------------------

Maven has to be installed to compile and install the library:

    $ mvn clean install

The library features can then be accessed using the scripts in the `scripts/`
directory or using the command `np` in `bin/`. For example:

    $ np check nanopubfile.trig

One can also directly use the JAR file (as generated in `target/`) or [download
a prebuilt one](https://github.com/Nanopublication/nanopub-java/releases):

    $ java -jar nanopub-1.6-jar-with-dependencies.jar check nanopubfile.trig


Developers
----------

- Tobias Kuhn (http://www.tkuhn.org)


License
-------

nanopub-java is free software under the MIT License. See LICENSE.txt.


Usage Tracking
--------------

This is an incomplete list of software projects using this library:

- https://github.com/Nanopublication/nanopub-store-api
- https://github.com/Nanopublication/landmark-publication-tool
- https://github.com/ISA-tools/NanoMaton
- https://github.com/tkuhn/bel2nanopub
- https://github.com/tkuhn/nanobrowser
- https://github.com/tkuhn/nanolytics
- https://github.com/tkuhn/nanopub-validator
- https://github.com/tkuhn/nanopub-server
- https://github.com/tkuhn/bio2rdf2nanopub
- https://github.com/tkuhn/nanopub-monitor
- https://github.com/rajaram5/NanopublicationVisualization
- https://github.com/rajaram5/Nanopubviz
