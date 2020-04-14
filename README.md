nanopub-java
============

This is a Java library for nanopublications (see http://nanopub.org) based on
RDF4J.

It implements the formal structure defined in the [official nanopublication
guidelines](http://nanopub.org/guidelines/working_draft/).


## Publication

- Tobias Kuhn.  [nanopub-java: A Java Library for
  Nanopublications](http://arxiv.org/pdf/1508.04977.pdf). In Proceedings of the
  5th Workshop on Linked Science (LISC 2015). 2015.


## Usage as Java Library

The easiest way to use this library in your project is to let Maven download it
from The Central Repository. Just include the following lines in your `pom.xml`
file:

    <dependency>
      <groupId>org.nanopub</groupId>
      <artifactId>nanopub</artifactId>
      <version>1.30</version>
    </dependency>

Alternatively, you might want to use one of the [pre-built
jar files](https://github.com/Nanopublication/nanopub-java/releases).


## Usage on Unix Command-Line

To use this library on the command line, just download the [np
script](https://raw.githubusercontent.com/Nanopublication/nanopub-java/master/bin/np).
Make sure it is executable and then you can invoke it with `./np`, for example:

```bash
./np check nanopubfile.trig
```

This automatically downloads the latest release as a jar file on the first run.
You can also directly use the [prebuilt jar
files](https://github.com/Nanopublication/nanopub-java/releases):

```bash
java -jar nanopub-1.30-jar-with-dependencies.jar check nanopubfile.trig
```

Note: For Mac users, before running `np` ensure that the GNU version of `curl`
is installed (not the default BSD versions), and are the ones being used when
the `curl` command is invoked.


## Usage with Docker

You can use this [image from
DockerHub](https://hub.docker.com/repository/docker/umids/nanopub-java).

Sign a nanopublication (`nanopub.trig` file in current dir here):

```bash
docker run -it --rm -v ~/.nanopub:/root/.nanopub -v $(pwd):/data umids/nanopub-java sign /data/nanopub.trig
```

Publish a signed nanopublication:

```bash
docker run -it --rm -v ~/.nanopub:/root/.nanopub -v $(pwd):/data umids/nanopub-java publish /data/signed.nanopub.trig
```

Build the Docker image:

```shell
docker build -t umids/nanopub-java .
```

## Compilation

Maven has to be installed to compile the library:

```bash
mvn clean package
```

The library features can then be accessed by calling `scripts/run.sh` (with the
same commands as for the `np` script above, but using the locally compiled code
and not the jar file).


## Developers

- Tobias Kuhn (http://www.tkuhn.org)


## License

nanopub-java is free software under the MIT License. See LICENSE.txt.


## Usage Tracking

This is an incomplete (and outdated) list of software projects using this library:

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
- https://github.com/tkuhn/npop
- https://github.com/rajaram5/NanopublicationVisualization
- https://github.com/rajaram5/Nanopubviz
- https://github.com/wikipathways/nanopublications
- https://github.com/jhpoelen/eol-globi-data/tree/master/eol-globi-data-tool
