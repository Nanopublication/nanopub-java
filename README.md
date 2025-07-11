# nanopub-java-fdo

This is a Java library for nanopublications (see http://nanopub.net) based on
RDF4J, with additional support for FAIR Digital Objects (FDOs).

It implements the formal structure defined in the [official nanopublication
guidelines](http://nanopub.net/guidelines/working_draft/) and the specification
for FDOs as defined by the [FDO Forum](https://fairdo.org/).


## Documentation

See the [JavaDocs](https://javadoc.io/doc/org.nanopub/nanopub/latest/index.html) for the API and
source code documentation.

The FDO-specific classes are documented [here](https://javadoc.io/doc/org.nanopub/nanopub/latest/org/nanopub/fdo/package-summary.html).


## Usage as Java Library

The easiest way to use this library in your project is to let Maven download it
from The Central Repository, with an entry in your pom.xml file as shown
[here](https://central.sonatype.com/artifact/org.nanopub/nanopub).

Alternatively, you might want to use one of the [pre-built
jar files](https://github.com/Nanopublication/nanopub-java/releases).


## Quickstart Java Instructions

In a nutshell, to create and publish nanopublications, you need to first make sure you have a
local keypair. To create such a keypair, run just once:

```java
    MakeKeys.make("~/.nanopub/id", SignatureAlgorithm.RSA);
```

And then nanopublications can be created and published programmatically like this:

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

    System.err.println("# Signing nanopub...");
    Nanopub signedNp = SignNanopub.signAndTransform(np, TransformContext.makeDefault());
    System.err.println("# Final nanopub after signing:");
    NanopubUtils.writeToStream(signedNp, System.err, RDFFormat.TRIG);

    System.err.println("# Publishing to test server...");
    PublishNanopub.publishToTestServer(signedNp);
    //System.err.println("# Publishing to real server...");
    //PublishNanopub.publish(signedNp);
    System.err.println("# Published");
```

For the complete code checkout ``UsageExamples.java``. 


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

Build the Docker image:

```shell
docker build -t nanopub/nanopub-java-fdo .
```

Sign a nanopublication (`nanopub.trig` file in current dir here):

```bash
docker run -it --rm -v ~/.nanopub:/root/.nanopub -v $(pwd):/data nanopub/nanopub-java-fdo sign /data/nanopub.trig
```

Publish a signed nanopublication:

```bash
docker run -it --rm -v ~/.nanopub:/root/.nanopub -v $(pwd):/data nanopub/nanopub-java-fdo publish /data/signed.nanopub.trig
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


## License

nanopub-java is free software under the MIT License. See [LICENSE.txt](LICENSE.txt).

For an overview of the dependencies and their licenses, run `mvn project-info-reports:dependencies` and then visit `target/reports/dependencies.html`.
