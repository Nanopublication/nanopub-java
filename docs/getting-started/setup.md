# Setup instructions

The tool is released to Maven central as a Maven artifact and Docker image. There is also a CLI available.

## :package: Install from Maven Central

=== "Maven"
    ``` xml
    <dependency>
        <groupId>org.nanopub</groupId>
        <artifactId>nanopub</artifactId>
        <version>1.77</version>
    </dependency>
    ```

=== "Gradle (Kotlin)"
    ``` kotlin
    implementation("org.nanopub:nanopub:1.77")
    ```

=== "sbt"
    ``` scala
    libraryDependencies += "org.nanopub" % "nanopub" % "1.77"
    ```

If you use a package manager that is included in the proposed ones, check
out [Nanopub on Maven Central](https://central.sonatype.com/artifact/org.nanopub/nanopub) to know how to import the package into your application.

## :simple-docker: Run as Docker Image 

An image is released on Docker Hub

## :octicons-command-palette-16: Run through the UNIX Command-Line (CLI)

To use this library on the command line, just download
the [np script](https://raw.githubusercontent.com/Nanopublication/nanopub-java/master/bin/np). Make sure it is
executable, and then you can invoke it with `./np` ... (or simply `np` ... if you make sure it's included in the PATH
variable), for example:

```
./np check nanopubfile.trig
```

This automatically downloads the latest release as a jar file on the first run. You can also directly use
the [pre-built jar files](https://github.com/Nanopublication/nanopub-java/releases/latest):

```
java -jar nanopub-1.77-jar-with-dependencies.jar check nanopubfile.trig
```

**Note:** For Mac users, before running np ensure that the GNU version of `curl` is installed (not the default BSD
versions),
and are the ones being used when the `curl` command is invoked.