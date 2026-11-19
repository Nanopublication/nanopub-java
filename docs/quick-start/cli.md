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
- check-template / CheckTemplateCompliance
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

An introduction counts only when it is signed by one of the keys it declares. The first
introduction of a signer is signed with the key it declares; one adding a further key is signed with
a key the signer already has, and restates that key alongside the new one. An introduction that
anyone could have published for somebody else declares no key it is signed with, so it establishes
nothing and the warning still comes.

#### `--strict`

Refuse rather than warn:

```bash
np sign --strict nanopub.trig
np publish --strict signed.nanopub.trig
```

| Command | `--strict` | Without it |
| --- | --- | --- |
| `sign` | Signs nothing, and exits with an error naming the problem | Warns on stderr and signs anyway |
| `publish` | Leaves the offending nanopublication unpublished, and carries on with the rest | Warns and publishes anyway |

On `publish` the flag is not new: it already refused nanopublications that fail verification, and it
now covers the signing-key check as well.

The check reads the network, so it fails open: when the query service cannot be reached, nothing is
reported and signing goes ahead. `--strict` does not turn an unreachable service into a refusal, on
the grounds that a check which could not be made is not evidence of a problem.

## Template Compliance

```
Usage: np check-template [options] input-nanopubs
  Options:
    -t
      File with a template nanopub to use instead of retrieving it; can be
      given several times
    -v
      Verbose: also show the templates checked against
```

The command checks whether nanopublications are what filling in the templates they name would
give. A nanopublication names its templates in its publication info, with
`nt:wasCreatedFromTemplate` for the assertion, `nt:wasCreatedFromProvenanceTemplate` for the
provenance, and `nt:wasCreatedFromPubinfoTemplate` for the publication info. The files can hold
one or several nanopublications, signed or not, so the check can be made before signing and
publishing.

For each nanopublication the command prints `COMPLIES` or `DOES NOT COMPLY`, followed by its
findings:

- An **error** is a statement of a template that is not optional and has no triples, a triple of
  the assertion or the provenance that belongs to no statement, a value that breaks a constraint
  of its placeholder, a template that cannot be retrieved or is for another graph, or a type the
  assertion template sets that the nanopublication lacks.
- A **warning** does not keep the nanopublication from complying: a triple of the publication
  info that no template has a statement for, a graph that no template is named for (it is not
  checked), an introduced or embedded resource that the publication info does not declare, or a
  value under a name the nanopublication keeps for one of its own parts, such as `assertion`.

The exit status is 1 if a nanopublication has an error or a file cannot be read.

The templates are retrieved from the nanopublication network, at exactly the versions named. To
check against a template that is not published yet, give its file with `-t`:

```np check-template -t my-template-signed.trig my-nanopub.trig```

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
