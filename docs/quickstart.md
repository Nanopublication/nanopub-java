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