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

To also look for mistakes that only show against other nanopublications, such as a new version
that gives the resource it introduces a new IRI, use `np check --network`, or in Java:

```java
List<NetworkFinding> findings = new NetworkChecker().check(nanopub);
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

## Checking a Nanopublication against its Templates

A nanopublication names the templates it was made from in its publication info.
`TemplateComplianceChecker` retrieves them and checks whether the nanopublication is what filling
them in would give:

```java
TemplateComplianceChecker checker = new TemplateComplianceChecker();
ComplianceReport report = checker.check(nanopub);
if (!report.isCompliant()) {
    for (ComplianceIssue issue : report.getErrors()) {
        System.out.println(issue.graph() + ": " + issue.message());
    }
}
```

An error is, for instance, a statement of a template that is not optional and has no triples, a
triple of the assertion that belongs to no statement, or a value that breaks a constraint of its
placeholder. `report.getWarnings()` has what does not keep the nanopublication from complying,
such as a triple of the publication info that no template has a statement for. A checker keeps
the templates it has retrieved, so use one for many nanopublications.

The classes underneath can be used on their own. `TemplateMatcher` assigns the triples of one
graph to the statements of one template, `PlaceholderMatcher` checks a single value against a
placeholder, and `BindingContext` holds the values bound:

```java
BindingContext context = BindingContext.forNanopub(template, nanopub);
TemplateMatch match = new TemplateMatcher(template).match(nanopub.getAssertion(), context);
match.getMissingStatements();   // statements that are not optional and have no triples
match.getUnusedTriples();       // triples that belong to no statement
match.getViolations();          // values that break a constraint
Value name = context.get(placeholderIri);
```

## Filling a Template

`TemplateFiller` makes a nanopublication from templates and the values of their placeholders, as
the publishing form of Nanodash does. A `FillRequest` names the templates and gives the values,
each under the name of its placeholder:

```java
FillRequest request = new FillRequest("https://w3id.org/np/RAEFAt-QcFK0ZhqfvlsmS10BnzGJA0xwOICZXkO-ai87k")
        .set("query", "get-things")
        .set("title", "Get things")
        .set("description", "Returns three things.")
        .set("endpoint", "https://w3id.org/np/l/nanopub-query-1.1/repo/full")
        .set("license", "http://www.apache.org/licenses/LICENSE-2.0")
        .set("sparql", "select * where { ?s ?p ?o } limit 3")
        .setCreator(creatorIri);
try {
    Nanopub nanopub = new TemplateFiller().fill(request);   // not signed
} catch (TemplateFillException ex) {
    ex.getProblems().forEach(System.out::println);
}
```

The nanopublication gets the template links, the introduced and embedded resources, the types and
the label that the form would give it. Its provenance and publication info are made from the
templates the request names, and otherwise from the default provenance template of the assertion
template, or "Attributed to myself/others", and from the templates for the creator and the license.

A value is written as it would be typed into the form: an IRI in full, a name that is minted
within the nanopublication, or the text of a literal. A statement that is repeated takes the
values `name`, `name__1`, `name__2` and so on; `request.getValues().add("name", "...")` gives the
next one. Nothing is made if something is wrong: `TemplateFillException` lists all values that are
missing or break a constraint, all names no placeholder answers to, and all values that would be
lost because the statement they belong to is left out.

A request has the parameters of a link to the publishing form of Nanodash, and is read from and
written as one with `FillRequest.fromUrl`, `fromJson`, `toQueryString` and `toJson`.

A form can check each value while it is typed, by the rules the filler applies, and start a field
with the default value of its placeholder:

```java
PlaceholderMatcher matcher = PlaceholderMatcher.of(template, placeholderIri);
BindingContext context = BindingContext.forNewNanopub(template, "http://purl.org/nanopub/temp/np001/", creatorIri);
MatchResult result = matcher.match(new RawValue("10.1234/abc"), context);
result.violations();            // what is wrong with the value as typed, each with its kind
Value start = matcher.getDefault(context);     // null if the placeholder has no default
RawValue shown = matcher.toRaw(start, context);  // the default as it would be typed
```

A text that cannot be a value at all, such as a name with a colon in it, gives a violation whose
value is null.

To make a new version of a nanopublication, read its request back, change it, and say what the
new nanopublication is to the old one:

```java
ComplianceReport report = new TemplateComplianceChecker().check(published);
FillRequest request = FillRequest.of(report)
        .set("title", "Get three things")
        .setSource(FillRequest.Mode.SUPERSEDE, published.getUri().stringValue());
Nanopub newVersion = new TemplateFiller().fill(request);
```

With `SUPERSEDE` and `OVERRIDE` the resources the old nanopublication introduces keep their IRIs
and the root definition stays; with `DERIVE` everything is minted anew. The request that is read
back does not have the link the nanopublication itself has to an earlier version. Read it only
from a nanopublication that complies with its templates (`report.isCompliant()`), as content the
templates do not account for is not in it.

To read an existing nanopublication for other templates than the ones it names, as the form does
when it is opened with `use`, `supersede` or `derive`, use a `SourceReader`. It takes the creator
of the nanopublication to make, if there is one, which `nt:CREATOR` then stands for:

```java
SourceReader reader = new SourceReader(published, creatorIri);
TemplateMatch assertion = reader.readAssertion(newerAssertionTemplate);
TemplateValues values = reader.getValues(assertion);   // by the names of the placeholders
assertion.getUnusedTriples();                          // what the template has no statement for
Map<Template, TemplateMatch> pubinfo = reader.readPubinfo(pubinfoTemplates);
reader.getUnreadPubinfo();                             // what no publication info template took
```

Each publication info template is matched against all of the publication info, except what a
template marked as transient has taken: what it says is about that nanopublication alone, and it
gives no values. A template that takes any triple, such as the one for statements written by hand,
gets only what no other template took, and nothing of what a fill writes by itself, such as the
creation time. `readPubinfo` with a list of templates reads them in that order. `FillRequest.of`
reads a nanopublication this way, for the templates it names.
