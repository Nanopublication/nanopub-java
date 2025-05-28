nanopub-java
============

This is a Java library for nanopublications (see http://nanopub.net) based on
RDF4J.

It implements the formal structure defined in the [official nanopublication
guidelines](http://nanopub.net/guidelines/working_draft/).


## Usage as Java Library

The easiest way to use this library in your project is to let Maven download it
from The Central Repository. Just include the following lines in your `pom.xml`
file:

    <dependency>
      <groupId>org.nanopub</groupId>
      <artifactId>nanopub</artifactId>
      <version>1.69</version>
    </dependency>

Alternatively, you might want to use one of the [pre-built
jar files](https://github.com/Nanopublication/nanopub-java/releases).

## Quickstart Java Instructions

In a nutshell, this is how nanopublications can be created and published
programmatically:

    ```java
    System.err.println("# Creating nanopub...");
    NanopubCreator npCreator = new NanopubCreator(true);
    final ValueFactory vf = SimpleValueFactory.getInstance();
    final IRI anne = vf.createIRI("https://example.com/anne");
    npCreator.addAssertionStatement(anne, RDF.TYPE, vf.createIRI("https://schema.org/Person"));
    npCreator.addProvenanceStatement(PROV.WAS_ATTRIBUTED_TO, anne);
    npCreator.addPubinfoStatement(RDF.TYPE, vf.createIRI("http://purl.org/nanopub/x/ExampleNanopub"));
    Nanopub np = npCreator.finalizeNanopub(true);
    System.err.println("# Nanopub before signing:");
    NanopubUtils.writeToStream(np, System.err, RDFFormat.TRIG);
    Nanopub signedNp = SignNanopub.signAndTransform(np, TransformContext.makeDefault());
    System.err.println("# Final nanopub after signing:");
    NanopubUtils.writeToStream(signedNp, System.err, RDFFormat.TRIG);
    System.err.println("# Publishing to test server...");
    PublishNanopub.publishToTestServer(signedNp);
    //System.err.println("# Publishing to real server...");
    //PublishNanopub.publish(signedNp);
    ```

## Usage on Unix Command-Line

To use this library on the command line, just download the [np
script](https://raw.githubusercontent.com/Nanopublication/nanopub-java/master/bin/np).
Make sure it is executable and then you can invoke it with `./np ...` (or simply
`np ...` if you make sure it's included in the PATH variable), for example:

```bash
./np check nanopubfile.trig
```

This automatically downloads the latest release as a jar file on the first run.
You can also directly use the [prebuilt jar
files](https://github.com/Nanopublication/nanopub-java/releases):

```bash
java -jar nanopub-1.67-jar-with-dependencies.jar check nanopubfile.trig
```

Note: For Mac users, before running `np` ensure that the GNU version of `curl`
is installed (not the default BSD versions), and are the ones being used when
the `curl` command is invoked.


## Usage with Docker

You can use this [image from
DockerHub](https://hub.docker.com/repository/docker/umids/nanopub-java).

Sign a nanopublication (`nanopub.trig` file in current dir here):

```bash
docker run -it --rm -v ~/.nanopub:/root/.nanopub -v $(pwd):/data nanopub/nanopub-java sign /data/nanopub.trig
```

Publish a signed nanopublication:

```bash
docker run -it --rm -v ~/.nanopub:/root/.nanopub -v $(pwd):/data nanopub/nanopub-java publish /data/signed.nanopub.trig
```

Build the Docker image:

```shell
docker build -t nanopub/nanopub-java .
```

## Compilation

Maven has to be installed to compile the library:

```bash
mvn clean package
```

The library features can then be accessed by calling `scripts/run.sh` (with the
same commands as for the `np` script above, but using the locally compiled code
and not the jar file).

## Test Coverage
Create the file target/jacoco.exec which includes the test coverage information in a binary format.
```bash
mvn clean verify
```
To create a html report out of jacoco.exec (target/site/jacoco/index.html) use:
```bash
mvn jacoco:report
```


## Publication

- Tobias Kuhn.  [nanopub-java: A Java Library for
  Nanopublications](http://arxiv.org/pdf/1508.04977.pdf). In Proceedings of the
  5th Workshop on Linked Science (LISC 2015). 2015.


## License

nanopub-java is free software under the MIT License. See LICENSE.txt.

