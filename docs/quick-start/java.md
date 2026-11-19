## Create a Nanopublication

!!! info "Prerequisite for creating/publishing"

	To create and publish nanopublications, you need to first make sure you have a local keypair. To create such a keypair, run just once:
    ``` Java
    MakeKeys.make("~/.nanopub/id", SignatureAlgorithm.RSA);
    ```

``` Java
NanopubCreator npCreator = new NanopubCreator(true);
final ValueFactory vf = SimpleValueFactory.getInstance();
final IRI anne = vf.createIRI("https://example.com/anne");
npCreator.addAssertionStatement(anne, RDF.TYPE, vf.createIRI("https://schema.org/Person"));
npCreator.addProvenanceStatement(PROV.WAS_ATTRIBUTED_TO, anne);
npCreator.addPubinfoStatement(RDF.TYPE, vf.createIRI("http://purl.org/nanopub/x/ExampleNanopub"));
Nanopub np = npCreator.finalizeNanopub(true);
```

## Sign a Nanopublication

!!! info "Prerequisite for signing/publishing"

    Before you can sign and publish you should [setup your profile](/nanopub/getting-started/setup), check if it is properly set by running `np profile` in your terminal.

```java
Nanopub signedNp = SignNanopub.signAndTransform(np, TransformContext.makeDefault());
```

## Publish a Nanopublication

!!! info "Prerequisite for creating/publishing"

	To create and publish nanopublications, you need to first make sure you have a local keypair. To create such a keypair, run just once:
    ```java
    MakeKeys.make("~/.nanopub/id", SignatureAlgorithm.RSA);
    ```

### Publish to the Test Server

```java
PublishNanopub.publishToTestServer(signedNp);
```

### Publish to the Production Server

```java
PublishNanopub.publish(signedNp);
```

## Verification of unpublished Nanopublications

Besides strict syntactical requirements a nanopub has to fulfill to be published 
there are a bunch of best practices, which should be honored. With the command line
tool you can use 

```bash
np check myNanopub.trig -v 
```

to see a list of issues. To ensure you only publish nanopubs that meet all best practices 
use the flag `--strict` when publishing:

```bash
np publish --strict myNanopub.trig -v 
```

In the java API you can use 

```java
NanopubVerifier verifier = new NanopubVerifier(nanopub);
verifier.verify();
verifier.getIssues();
```

### Best Practices

The best practices that are verified are the following:

- The creation timestamp of the nanopub is not in the future and not older than one hour.
- The nanopub has a label.
- The nanopub has a type.
- The nanopub uses an assertion template and a provenance template.
- The nanopub has a signer and the signer is also a creator of the nanopub.
- The nanopub contains the graph uris: ...Head, ...assertion, ...provenance, and ...pubinfo.
- The nanopub consists of below than 1200 triples and is smaller than 10MB.

## Reading a Template

A template is a nanopublication that describes how nanopublications of a certain kind are
structured: the statements they consist of, the placeholders to be filled in, and the
constraints on the values. `Template` reads one:

```java
// Fetches the template nanopublication from the network:
Template template = new Template("https://w3id.org/np/RAEzjboN_ipuwsFnvhqAl9Fy7fZl34cGZ1QhJ-7cbbGGM");
// Or from a nanopublication at hand, such as a file that is not published yet:
Template template = new Template(new NanopubImpl(new File("my-template.trig")));

for (IRI statement : template.getStatementIris()) {
    IRI subject = template.getSubject(statement);
    IRI predicate = template.getPredicate(statement);
    Value object = template.getObject(statement);
    boolean optional = template.isOptionalStatement(statement);
    if (object instanceof IRI placeholder && template.isPlaceholder(placeholder)) {
        String label = template.getLabel(placeholder);
        String regex = template.getRegex(placeholder);       // null if unconstrained
        boolean isLiteral = template.isLiteralPlaceholder(placeholder);
    }
}
```

A grouped statement is one entry of `getStatementIris()`, and `getStatementIris(group)`
lists its members. `getStatementErrors()` reports statements that cannot produce valid
nanopublications.

Reading a template needs no network access. Only the possible values of a restricted-choice
placeholder that are kept in another nanopublication (`nt:possibleValuesFrom`) are fetched,
when `getPossibleValues(placeholder)` is first called. Pass a `NanopubResolver` to decide
where nanopublications come from, or `NanopubResolver.NONE` to never fetch any:

```java
Template template = new Template(nanopub, NanopubResolver.NONE);
```

`getDescription()` and `getStatementDescription(statement)` return the text as published.
It may contain HTML and is not sanitized.
