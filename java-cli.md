Java Command Line Interface
===========================

This is the documentation of the java cli of nanopub.java.

## Available Commands

You can get the list of commands by typing:

```java -jar nanopub-<version>-jar-with-dependencies.jar```

In the following list you can see the short command first, followed by the classname 
implementing that command. 

- check / CheckNanopub
- get / GetNanopub
- publish / PublishNanopub
- sign / SignNanopub
- mktrusty / MakeTrustyNanopub
- fix / FixTrustyNanopub
- status / NanopubStatus
- server / GetServerInfo
- mkindex / MakeIndex
- mkkeys / MakeKeys
- html / Nanopub2Html
- now / TimestampNow
- op / Run
- setting / ShowSetting
- query / RunQuery
- udtime / TimestampUpdater
- strip / StripDown
- shacl / ShaclValidator
- rocrate / RoCrateImporter

Some commands are grouped as subcommands of the `op` command. 
- filter / Filter
- extract / Extract
- gml / Gml
- fingerprint / Fingerprint
- topic / Topic
- reuse / Reuse
- count / Count
- decontext / Decontextualize
- union / Union
- ireuse / IndexReuse
- exportjson / ExportJson
- namespaces / Namespaces
- aggregate / Aggregate
- import / Import
- create / Create
- build / Build
- tar / Tar

To get more information on the usage of a command just type it in. E.g.

```java -jar nanopub-<version>-jar-with-dependencies.jar check```

will output:

```
Usage: <main class> [options] input-nanopubs
  Options:
    -s
      Load nanopubs from given SPARQL endpoint
    -v
      Verbose
      Default: false
```

## Keys and Profile

Some commands use the keys in `~/.nanopub/`. To generate the key pair use:

```java -jar nanopub-<version>-jar-with-dependencies.jar mkkeys```

The command `sign` takes a profile file in yaml format:

#### profile.yaml
```yaml
orcid_id: https://orcid.org/0009-0008-3635-347X
public_key: /Users/name/.nanopub/id_rsa.pub
private_key: /Users/name/.nanopub/id_rsa
```

## Shacl Validation

```
Usage: java -jar nanopub.jar shacl [options]
  Options:
  * -n
      nanopub-to-be-validated
  * -s
      SHACL shape file
```

The command takes two files in the .trig format, the nanopublication and the SHACL *specification* or 
*shape* file. This shape-file corresponds to the profile in handle-based FDOs. 
