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
- fill / FillTemplate
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

## Checking against the Network

```np check --network my-nanopub.trig```

Some mistakes leave a nanopublication valid, so that it signs and publishes, and still keep it
from doing what it is meant to do. They only show against other nanopublications. With
`--network` (or `-n`), `np check` also looks for these:

- **A new version that gives its resource a new IRI.** A nanopublication that supersedes another
  has to introduce the same resources again, under the same IRIs. Written with the prefix of the
  new nanopublication, an IRI changes with the trusty URI, and each version introduces a resource
  of its own. Write the IRI of the earlier version in full, in the assertion and with
  `npx:introduces`. For a query this is a note, as a query is specific to its version; it should
  then be declared with `npx:embeds`. It is a note too for a nanopublication that introduces its
  own assertion, as a template in the old shape does.
- **A template link of the wrong kind.** `nt:wasCreatedFromTemplate` has to name an assertion
  template, `nt:wasCreatedFromProvenanceTemplate` a provenance template, and
  `nt:wasCreatedFromPubinfoTemplate` a publication info template.
- **A new version signed with another key.** A nanopublication takes the place of the one it
  supersedes only if it is signed with the same key, unless a space governs it.
- **Governance without effect.** A definition that names a space with `gen:governedBy` is governed
  by it only if the space maintains what the definition is a version of (`dct:isVersionOf`), and
  the nanopublication declares the definition with `npx:embeds`.

The first three are errors, the last is a note:

```
CHECK AGAINST THE NETWORK FAILED: https://w3id.org/np/RA...
- ERROR: The superseded nanopublication <https://w3id.org/np/RA...> introduces <.../thing>, and this version introduces <.../thing> instead, which is another resource. ...
```

The exit status is 1 if there is an error. What could not be checked, because a nanopublication
could not be retrieved or no query service answered, is a note and never an error. A
nanopublication that is not signed yet is checked too, apart from its key.

Whether a nanopublication is what its templates describe is another check: `np check-template`.

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

## Filling a Template

```
Usage: np fill [options] requests
  Options:
    --profile
      Profile file for the creator IRI if neither -c nor the request gives
      one, defaults to ~/.nanopub/profile.yaml
    --request-of
      Print the request of an existing nanopub instead of filling one: its
      URI, or a file that holds it
    -c
      The IRI of the creator, typically an ORCID IRI; overrides the one of the
      request
    -o
      Output file; standard output if not given
    -t
      File with a template nanopub to use instead of retrieving it; can be
      given several times
```

The command makes a nanopublication from templates and the values of their placeholders, as the
publishing form of Nanodash does. The nanopublication is not signed: sign and publish it with
`np sign` and `np publish`.

A request has the parameters of a link to the publishing form of Nanodash. It is given as such a
link or its query string, or as a JSON object with the parameters as its members, or as the name
of a file that holds one of these:

```
np fill -o my-query.trig request.json
np fill 'https://nanodash.example.org/publish?template=https://w3id.org/np/RA...&param_name=Example'
```

```json
{
  "template": "https://w3id.org/np/RAEFAt-QcFK0ZhqfvlsmS10BnzGJA0xwOICZXkO-ai87k",
  "param_query": "get-things",
  "param_title": "Get things",
  "param_description": "Returns three things.",
  "param_endpoint": "https://w3id.org/np/l/nanopub-query-1.1/repo/full",
  "param_license": "http://www.apache.org/licenses/LICENSE-2.0",
  "param_sparql": "select * where { ?s ?p ?o } limit 3",
  "creator": "https://orcid.org/0000-0000-0000-0000"
}
```

| Parameter | Meaning |
| --- | --- |
| `template` | The template of the assertion. |
| `param_<name>` | The value of the placeholder of that name. A placeholder that has a value of its own in each repetition of a statement takes `param_<name>` for the first, `param_<name>__1` for the second, and so on. An empty value keeps the default value of the template from being taken. |
| `param_<name>__lang` | The language tag of the value, where the placeholder leaves the language open. |
| `prtemplate`, `prparam_<name>` | The template of the provenance and the values for it. Without one, the default provenance template of the assertion template is used, or else "Attributed to myself/others". |
| `pitemplate1`, `piparam1_<name>` | A template of the publication info and the values for it; further ones with 2, 3 and so on. The templates for the creator and the license (CC BY 4.0 unless a value is given) are used in any case, as are those the assertion template requires. |
| `supersede` | The nanopublication the new one is a new version of. It is linked with `npx:supersedes`, and the resources it introduces and its root definition are kept. |
| `override` | The nanopublication the new one is another definition of. It is linked with `prov:wasDerivedFrom`, and the resources it introduces and its root definition are kept. |
| `derive` | The nanopublication the new one is derived from. It is linked with `prov:wasDerivedFrom`; nothing is kept. |
| `target-namespace` | The namespace the nanopublication gets its URI in, instead of `https://w3id.org/np/`. |
| `creator`, `creator-name` | The creator, whom a form takes from the person logged in. Without one, the `orcid_id` of the profile is taken. |
| `space`, `namespace` | What `~~SPACE~~` and `~~NAMESPACE~~` stand for in the prefixes of a template, which a form takes from the page it is opened on. |

The value of a placeholder is written as it would be typed into the form: an IRI in full, a name
that is minted within the nanopublication or put after the prefix of the placeholder, or the text
of a literal.

Unlike the form, the command reads no values from the nanopublication named by `supersede`,
`override` or `derive`: all values have to be given. Parameters that would have the form read
values from elsewhere, such as `use`, are refused.

To make a new version of a nanopublication, have its request printed, change it, and add the
`supersede` parameter:

```
np fill --request-of https://w3id.org/np/RA... > request.json
# change values in request.json, and add:  "supersede": "https://w3id.org/np/RA..."
np fill -o new-version.trig request.json
```

The request is read out of the way the nanopublication matches its templates. It has the
templates the nanopublication names, the values of their placeholders, and its creator, but not
a link the nanopublication itself has to an earlier version. A template for statements written
by hand gets only what the other templates leave, and nothing of what a fill writes by itself,
such as the creation time.

A nanopublication that does not comply with its templates has no request; the command then
lists what `np check-template` finds. Warnings are printed to standard error: those of that
check, which can point to content the templates say nothing about and which is therefore not in
the request, and the problems of the request if it cannot be filled as a new version as it is.

Nothing is written if something is wrong with a request. The command then lists all it found,
and the exit status is 1:

```
CANNOT FILL: request.json
- The statement st1 of the assertion template lacks a value for 'param_title' (title of the query)
- No placeholder of the assertion template answers to 'param_titel'; its placeholders are: description, endpoint, license, query, sparql, title
```

The templates are retrieved from the nanopublication network, at exactly the versions named. To
use a template that is not published yet, give its file with `-t`.

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
