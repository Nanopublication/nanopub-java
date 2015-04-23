nanopub-java
============

This is a Java library for nanopublications (see http://nanopub.org) based on
Sesame.


Usage
-----

The easiest way to use this library in your project is to let Maven download it
from The Central Repository. Just include the following lines in your `pom.xml`
file:

    <dependency>
      <groupId>org.nanopub</groupId>
      <artifactId>nanopub</artifactId>
      <version>1.5</version>
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

One can also directly use the JAR file (as generated in `target/`):

    $ java -jar nanopub-1.6-jar-with-dependencies.jar check nanopubfile.trig


Developers
----------

- Tobias Kuhn (http://www.tkuhn.ch)


License
-------

nanopub-java is free software under the MIT License. See LICENSE.txt.
