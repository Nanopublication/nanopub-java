# UNIX Command-Line (CLI)

Java Command Line Interface
===========================

This is the documentation of the java cli of nanopub.java.

## Available Commands

You can get the list of commands by typing:

```np help```

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

Some commands are grouped as subcommands of the `op` command. You can list those by running:
```np op help```

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

```np check```

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

```np mkkeys```

The command `sign` takes a profile file in yaml format:

#### profile.yaml

```yaml
orcid_id: https://orcid.org/0009-0008-3635-347X
public_key: /Users/name/.nanopub/id_rsa.pub
private_key: /Users/name/.nanopub/id_rsa
```

### Checking the key against the network

Signing with a key that no introduction declares for your ORCID produces a nanopublication that is
cryptographically valid and publishes normally, but that nobody can attribute to you: the registry
has nothing tying the key to the person, so it shows up under an unapproved agent. A nanopublication
cannot be edited afterwards, so the only remedy is publishing it again under a declared key and
retracting the first.

`sign` and `publish` therefore ask the network about the key first, and warn:

```text
WARNING: https://orcid.org/0000-... is introduced on the network, but by a different key than the
one about to sign. Nanopublications signed with this key cannot be attributed, and will show as
coming from an unapproved agent. Publish an introduction declaring this key, or sign with the
declared one.
```

Add `--strict` to refuse rather than warn:

```bash
np sign --strict nanopub.trig
np publish --strict signed.nanopub.trig
```

The check reads the network, so it fails open: when the query service cannot be reached, nothing is
reported and signing goes ahead. `--strict` does not turn an unreachable service into a refusal.

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
