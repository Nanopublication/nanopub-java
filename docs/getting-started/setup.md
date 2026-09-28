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
    curl -LsSf https://nanopublication.github.io/nanopub-java/install.sh | bash
    ```

=== "Windows PowerShell"
    ``` bash
    irm https://nanopublication.github.io/nanopub-java/install.ps1 | iex
    ```

This automatically downloads the latest release as a jar file on the first run.

The jar is fetched from [Maven Central](https://central.sonatype.com/artifact/org.nanopub/nanopub).

!!! tip "Environment variables"

    Set these before running the installer to change its behaviour:

    | Variable | Purpose |
    | --- | --- |
    | `NANOPUB_VERSION` | Install a specific version instead of the latest one |
    | `NANOPUB_INSTALL_DIR` | Where the `np` launcher is placed (default: `~/.nanopub/bin`) |
    | `NANOPUB_JAR_DIR` | Where the jar file is saved (default: `~/.nanopub/lib`) |