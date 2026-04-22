# Setup instructions

The tool is released to Maven central as a Maven artifact and Docker image. There is also a CLI available.

## :package: Install from Maven Central

=== "Maven"
    ``` xml
    <dependency>
        <groupId>org.nanopub</groupId>
        <artifactId>nanopub</artifactId>
        <version>1.86.2</version>
    </dependency>
    ```

=== "Gradle (Kotlin)"
    ``` kotlin
    implementation("org.nanopub:nanopub:1.86.2")
    ```

=== "sbt"
    ``` scala
    libraryDependencies += "org.nanopub" % "nanopub" % "1.86.2"
    ```

If you use a package manager that is included in the proposed ones, check
out [Nanopub on Maven Central](https://central.sonatype.com/artifact/org.nanopub/nanopub) to know how to import the package into your application.

## :simple-docker: Run as Docker Image 

An image is released on Docker Hub

## :octicons-command-palette-16: Run through the UNIX Command-Line (CLI)

To use this library on the command line, just run:

=== "macOS, Linux, WSL"
    ``` bash
    curl -LsSf https://raw.githubusercontent.com/Nanopublication/nanopub-java/master/bin/install.sh | bash
    ```

=== "Windows PowerShell"
    ``` bash
    irm https://raw.githubusercontent.com/Nanopublication/nanopub-java/master/bin/install.ps1 | iex
    ```

This automatically downloads the latest release as a jar file on the first run.