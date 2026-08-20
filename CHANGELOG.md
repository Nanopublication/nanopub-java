## [1.92.0](https://github.com/Nanopublication/nanopub-java/compare/nanopub-1.91.0...nanopub-1.92.0) (2026-08-20)

### Features

* Added size check to NanopubVerifier ([2913132](https://github.com/Nanopublication/nanopub-java/commit/2913132f25c891c109dec438cea6b1c1159fc966))
* Added URI Protocol Check to NanopubVerifier ([71ba8ef](https://github.com/Nanopublication/nanopub-java/commit/71ba8ef2a8321190e050ccce92541cae8501c171))
* Blacklist Check in Verifier ([4daf1db](https://github.com/Nanopublication/nanopub-java/commit/4daf1db1b6cc84d543076ec43cdfbbe9521bac55))
* Try query instances sequentially instead of concurrently ([bbd7944](https://github.com/Nanopublication/nanopub-java/commit/bbd794409fdaf97096fd671569a1936b706ced34)), closes [#130](https://github.com/Nanopublication/nanopub-java/issues/130)
* **verify:** check that literal values are valid for their datatype ([765f409](https://github.com/Nanopublication/nanopub-java/commit/765f409b335f2a34d85a34aac35033709619ce70)), closes [#12](https://github.com/Nanopublication/nanopub-java/issues/12)

### Dependency updates

* **core-deps:** update dependency jakarta.activation:jakarta.activation-api to v2.1.4 ([9ebaf1a](https://github.com/Nanopublication/nanopub-java/commit/9ebaf1a268ef19590966fdc633933ec8969bd915))
* **core-deps:** update dependency org.junit:junit-bom to v6.1.3 ([5ea9086](https://github.com/Nanopublication/nanopub-java/commit/5ea9086579f6663aa15dcb8b0e590f2dbf36607f))
* **deps:** update dependency mkdocs-material to v9.7.7 ([9b4b40a](https://github.com/Nanopublication/nanopub-java/commit/9b4b40a49bd79719d4766cd790445d6fc1be4ac1))

### Bug Fixes

* add warning for ill-typed trusty nanopubs ([999221b](https://github.com/Nanopublication/nanopub-java/commit/999221b1432a9d6bf50db5ef1ad70181f1931589))
* apply temp-URI replacement to the signer IRI ([6770a95](https://github.com/Nanopublication/nanopub-java/commit/6770a958b11c98afe91293e811776eefe9ebeccd)), closes [#127](https://github.com/Nanopublication/nanopub-java/issues/127)
* **fdo:** close the static TransformContext mock after FdoRecordTest ([be666b9](https://github.com/Nanopublication/nanopub-java/commit/be666b997af23efdfaeb151e18e3e100f9631b49))
* **security:** accept PEM-formatted key files in loadKey ([e8b9c0a](https://github.com/Nanopublication/nanopub-java/commit/e8b9c0a231ec16b5f93e2ab71bdcfd917084d395)), closes [PKCS#1](https://github.com/Nanopublication/PKCS/issues/1) [#13](https://github.com/Nanopublication/nanopub-java/issues/13)
* treat ill-typed literals as invalid in plain nanopubs ([8ab55da](https://github.com/Nanopublication/nanopub-java/commit/8ab55dabae9a0ceab1be5d65b21165157b8532f6))
* **vocabulary:** add private constructor to disable vocabulary class instantiation ([fe48f0e](https://github.com/Nanopublication/nanopub-java/commit/fe48f0ec44b4c559775b472019d29996ac2d0946))
* **vocabulary:** update `IS_HASH_OF` with correct term in NPA vocabulary ([0b2dfcc](https://github.com/Nanopublication/nanopub-java/commit/0b2dfcc5706848af772b2140ab214a933d083072))

### Documentation

* **cli:** point the install instructions at the docs site ([a03c0b2](https://github.com/Nanopublication/nanopub-java/commit/a03c0b215be2162c8cecd0ddac3432796d848f2a))
* fix the broken favicon reference ([e3da9d9](https://github.com/Nanopublication/nanopub-java/commit/e3da9d904cdd8d852462b5a2dd8b1a1b64f3e078))
* mention self-signing in the sign -s description ([1b30cd1](https://github.com/Nanopublication/nanopub-java/commit/1b30cd1d279d59f91b5ad8bddc5219778e7fd545))

### Tests

* **check:** add unit test ([772878e](https://github.com/Nanopublication/nanopub-java/commit/772878eb43c066e9bc4de470c53711b4a1a2b16d))
* **cli:** add unit test ([e16ee4b](https://github.com/Nanopublication/nanopub-java/commit/e16ee4bf56eef1fc8984776d4ef650ca8943f9d8))
* **crypto:** add unit test ([e53a878](https://github.com/Nanopublication/nanopub-java/commit/e53a8784b6bf55b7618e335655e27969bce4ebbb))
* **equality:** add unit test ([4b10753](https://github.com/Nanopublication/nanopub-java/commit/4b107530483b0f4a6ec33b0999360b442e133ed9))
* **fixtrusty:** add unit test ([fd516fe](https://github.com/Nanopublication/nanopub-java/commit/fd516fef2c44f6185ba3d8ab99f46c3154b1114a))
* **handlers:** add unit test ([fe5cd66](https://github.com/Nanopublication/nanopub-java/commit/fe5cd66d876d6459871e5ef0ee6c0722a919fd2e))
* **html:** add unit test ([600cd39](https://github.com/Nanopublication/nanopub-java/commit/600cd39481db56316f9e0fce13e821a5c3c900fa))
* **index:** add unit test ([5684398](https://github.com/Nanopublication/nanopub-java/commit/5684398d88f79619325791ef671a2f511e1659d9))
* **intro:** add unit test ([b481d72](https://github.com/Nanopublication/nanopub-java/commit/b481d720fce2a062bb82f7bbe51e8b5c207ee953))
* **jelly-stream:** add unit test ([85550cd](https://github.com/Nanopublication/nanopub-java/commit/85550cdc2c4b88803bdcaaf24c49b6b8ef9e9cd9))
* **jelly:** add unit test ([0d0beec](https://github.com/Nanopublication/nanopub-java/commit/0d0beec69d45220703211b7556a6be9a80447798))
* **mktrusty:** add unit test ([e829b89](https://github.com/Nanopublication/nanopub-java/commit/e829b89778083e8339cd910e7bfe679041ff834d))
* **mock-utils:** serve a response body and content type ([60ce8ba](https://github.com/Nanopublication/nanopub-java/commit/60ce8ba843de9eef977142541a6916d233ee7e5b))
* **Nanopub:** add unit test ([429fecd](https://github.com/Nanopublication/nanopub-java/commit/429fecdfe8c72e3fcdb480e740aa53d324839b67))
* **NanopubAlreadyFinalizedException:** add unit test ([90c2557](https://github.com/Nanopublication/nanopub-java/commit/90c2557b934eb84e52fe42602abe4c58ed5f216d))
* **NanopubCreator:** add unit test ([b5b4ed5](https://github.com/Nanopublication/nanopub-java/commit/b5b4ed5105f28067d05600e14dcbb4c62b4341f6))
* **NanopubImpl:** add unit test ([4b06d64](https://github.com/Nanopublication/nanopub-java/commit/4b06d6437428c50f674e1d718647b9ffa7218747))
* **NanopubProfile:** add unit test ([40584ca](https://github.com/Nanopublication/nanopub-java/commit/40584ca82dbde2ce36b887ee75de96d51ad078b1))
* **NanopubUtils:** add unit test ([8c9d0b2](https://github.com/Nanopublication/nanopub-java/commit/8c9d0b2a365a396b9edb3f2d8f044201bf0f1053))
* **patterns:** add unit test ([47b85ec](https://github.com/Nanopublication/nanopub-java/commit/47b85ec5da59b17b3be087c8e99f5d1fa425f292))
* **query-api:** add unit test ([f40c36a](https://github.com/Nanopublication/nanopub-java/commit/f40c36a4713d212776a9c41c8c2d7b9f218ad530))
* **retraction:** add unit test ([e4b50c8](https://github.com/Nanopublication/nanopub-java/commit/e4b50c890492aaeadb2a3a3616a62ba967b79f52))
* **rocrate:** add unit test ([675024f](https://github.com/Nanopublication/nanopub-java/commit/675024f2bcdad87756075b83d63b2447b83d2c05))
* **service-lookup:** add unit test ([2ae081c](https://github.com/Nanopublication/nanopub-java/commit/2ae081cdf5626f13e978a2dde44240a6c70d27de))
* **signatures:** add unit test ([2d2c8d4](https://github.com/Nanopublication/nanopub-java/commit/2d2c8d4afcd023202cdb3a8060ca9eeecdd42c92))
* **transform:** add unit test ([1672dc7](https://github.com/Nanopublication/nanopub-java/commit/1672dc7483a16aed295c4d146ad9886abe60e357))
* **trig:** add unit test ([a352a9b](https://github.com/Nanopublication/nanopub-java/commit/a352a9b7f093318089d5807d1bcae8bc69358872))
* **trusty:** add unit test ([7ceae6e](https://github.com/Nanopublication/nanopub-java/commit/7ceae6e96b7aa4eff717702bf80df26ff4a8a00b))
* **uri-rewriting:** add unit test ([277f64b](https://github.com/Nanopublication/nanopub-java/commit/277f64b3c04f3af9f0165780f89d746c4138eb09))
* **vocabulary:** add parameterized unit tests for classes ([5abba55](https://github.com/Nanopublication/nanopub-java/commit/5abba55dcc33f6e9f428e85352a1a9cf89a0f538))

### Build and continuous integration

* **deps:** update actions/checkout action to v7 ([4798d7d](https://github.com/Nanopublication/nanopub-java/commit/4798d7d0879788adc14539ab07bc4a3fd183b816))
* **deps:** update actions/setup-java action to v5.7.0 ([a2ee266](https://github.com/Nanopublication/nanopub-java/commit/a2ee266acc0001bd7822e8211096a785a8c6cb54))
* **deps:** update actions/setup-node action to v7 ([1ffa67c](https://github.com/Nanopublication/nanopub-java/commit/1ffa67c95fd4cfe47b7e4f41353539e6035fe8d7))
* **deps:** update actions/setup-python action to v7 ([b3cd9c1](https://github.com/Nanopublication/nanopub-java/commit/b3cd9c1068bb24ac22160d7af34ff3e51a0ddc3d))
* **deps:** update coverallsapp/github-action action to v2.3.8 ([8fe9519](https://github.com/Nanopublication/nanopub-java/commit/8fe951985f35f36a8dc9f8e802371f066ff6dfba))
* **deps:** update dependency org.apache.maven.plugins:maven-failsafe-plugin to v3.5.6 ([18e5e68](https://github.com/Nanopublication/nanopub-java/commit/18e5e6803ff41a7bdc1566ca41b3acd2d2ae5fdd))
* **deps:** update dependency org.apache.maven.plugins:maven-jar-plugin to v3.5.1 ([2d05f23](https://github.com/Nanopublication/nanopub-java/commit/2d05f23e07932faeea97202169252572323a961d))
* **deps:** update dependency org.apache.maven.plugins:maven-surefire-plugin to v3.5.6 ([8997586](https://github.com/Nanopublication/nanopub-java/commit/8997586d1d4b8cfd095a38f29b99401ed1149ea7))
* **deps:** update docker/login-action action to v4 ([07c293f](https://github.com/Nanopublication/nanopub-java/commit/07c293fad33510a43e97c77bfdc88c8f245ad39d))
* **deps:** update lock file ([65ffb4b](https://github.com/Nanopublication/nanopub-java/commit/65ffb4b4b7288b974714ff9a0dffe021c19c28ef))
* **release:** update node to v24.11.1 ([943341d](https://github.com/Nanopublication/nanopub-java/commit/943341ddc66e03296181287ef73c1ea282ba0694))

### General maintenance

* add missing imports after merge ([1f1226b](https://github.com/Nanopublication/nanopub-java/commit/1f1226b2a98d5708cb2aec9c5c586a92557d04ce))
* add new IRIs for language-tagged literals and advanced statements ([24ba999](https://github.com/Nanopublication/nanopub-java/commit/24ba9990b4c97c19b08a9bc5726a1861ff53f7dd))
* **cli:** fetch the CLI jar from Maven Central instead of the GitHub API ([1c9a0cf](https://github.com/Nanopublication/nanopub-java/commit/1c9a0cfee5d0019f59355254e20a43fef04a061e))
* **doc:** Documentation about best practices and verification ([3c8fdad](https://github.com/Nanopublication/nanopub-java/commit/3c8fdad52e582f827723289ec968f58a6206eaf3))
* fix integration test and merge some stuff ([6b272e9](https://github.com/Nanopublication/nanopub-java/commit/6b272e9a46b7cdbc407a9803d5f1be6a148ecd74))
* **jelly:** make the utility classes non-instantiable ([2c3ca48](https://github.com/Nanopublication/nanopub-java/commit/2c3ca48ffdc17508e9dc64d9fc3d82305f35eb28))
* let claude do the rest of the merge ([5c2eec0](https://github.com/Nanopublication/nanopub-java/commit/5c2eec01afbf1a7e402f01d5128bf31ce2c5bcd3))
* **logger:** unify on SLF4J and fix library logging behaviour ([2d6b05d](https://github.com/Nanopublication/nanopub-java/commit/2d6b05d630e07d661985fdec8b36a39de206e55d))
* **renovate:** add config ([5f4b374](https://github.com/Nanopublication/nanopub-java/commit/5f4b3747164942c911a996c82892ff34b497a6a3))
* **renovate:** tag JUnit updates as test(deps) ([1d74a9c](https://github.com/Nanopublication/nanopub-java/commit/1d74a9c05b6129b95349ebee53ba9cd0adf36524))
* setting next snapshot version [skip ci] ([a360c0b](https://github.com/Nanopublication/nanopub-java/commit/a360c0b1b5faf5b18e7914ce1f9fd9033c5f492f))

### Refactoring

* update after merge ([8f791e4](https://github.com/Nanopublication/nanopub-java/commit/8f791e4d3e90b2781d34176c7fe0a9af82e14303))

## [1.91.0](https://github.com/Nanopublication/nanopub-java/compare/nanopub-1.90.0...nanopub-1.91.0) (2026-07-07)

### Features

* cli *check* verifies more issues ([67db829](https://github.com/Nanopublication/nanopub-java/commit/67db8291c585e0d3303e79701d0769b5ca0ccd4e))
* Show nanopub-java version in cli ([e33ff93](https://github.com/Nanopublication/nanopub-java/commit/e33ff935dc85172f6ed73e30ef7a27a97a330227))
* Strict mode for publishing nanopubs ([fda62fb](https://github.com/Nanopublication/nanopub-java/commit/fda62fbfa9fad43e134f4bc244e5961c3b3e7f8a))
* Validate Nanopubs Before Publishing ([0e4d18d](https://github.com/Nanopublication/nanopub-java/commit/0e4d18d6deccf70d64896d645c23775314c38e02))

### Dependency updates

* **core-deps:** replace com.beust:jcommander with relocated org.jcommander:jcommander dependency and update to v1.83 ([be24ca5](https://github.com/Nanopublication/nanopub-java/commit/be24ca51a9da46b851cce08bb6f99a8746b143e0))
* **core-deps:** update jelly-rdf4j.version to v3.7.3 ([e7d079c](https://github.com/Nanopublication/nanopub-java/commit/e7d079cd727f0a512e6e4b1df389aa221f81ff87))
* **core-deps:** update rdf4j.version to v5.3.2 ([6deb00c](https://github.com/Nanopublication/nanopub-java/commit/6deb00cbb4b7ac0c6b7bd32c91e2cec124227b4f))
* **core-deps:** update slf4j.version to v2.0.18 ([eed499e](https://github.com/Nanopublication/nanopub-java/commit/eed499e8275779ce4344e22ff4995bd2b708eba3))
* **deps:** update central-publishing-plugin.version to v0.11.0 ([3381994](https://github.com/Nanopublication/nanopub-java/commit/3381994f159e1081dc7477d386b2e16634588201))
* **deps:** update deps for docs ([98bbdf7](https://github.com/Nanopublication/nanopub-java/commit/98bbdf788406e87107b36d379e801e116e7e202c))
* **deps:** update Maven Wrapper to v3.3.4 and Maven distribution to v3.9.16 ([0db6400](https://github.com/Nanopublication/nanopub-java/commit/0db6400cf395169736818f80998e7d9b91da4b5e))

### Tests

* **deps:** update junit-framework monorepo to v6.1.0 ([a4582f2](https://github.com/Nanopublication/nanopub-java/commit/a4582f234e72d967aeb4d21c346a3a3a05fddb7b))
* **deps:** update junit-jupiter.version to v6.1.1 ([1b3fd4a](https://github.com/Nanopublication/nanopub-java/commit/1b3fd4a218a93a79674fa71199d6091c573308b8))

### Build and continuous integration

* **deps:** lock file maintenance ([dc3191d](https://github.com/Nanopublication/nanopub-java/commit/dc3191d372d59aa4879b0ef0b31ee22d3ff688f1))
* **deps:** update actions/checkout action to v6.0.3 ([2b799c9](https://github.com/Nanopublication/nanopub-java/commit/2b799c946560de09bba7ea5a6f5e877de647c34e))
* **deps:** update actions/setup-java action to v5.2.0 ([a09c19d](https://github.com/Nanopublication/nanopub-java/commit/a09c19d9eaebbe542541c0be2203af41fdbd49cc))
* **deps:** update build-helper-plugin.version to v3.6.1 ([689aef6](https://github.com/Nanopublication/nanopub-java/commit/689aef6381da3ebcf1c62da05745c105f2a00bbf))
* **deps:** update jacoco.version to v0.8.15 ([c52eec1](https://github.com/Nanopublication/nanopub-java/commit/c52eec18890fe168106aebc34daa62f76ed14c41))
* **deps:** update maven-assembly-plugin.version to v3.8.0 ([8c4d2db](https://github.com/Nanopublication/nanopub-java/commit/8c4d2db60ba7ac2a292e41ac9ffb3d318d131ec6))
* **deps:** update maven-dependency-plugin.version to v3.11.0 ([3bf6caa](https://github.com/Nanopublication/nanopub-java/commit/3bf6caa8d3f365f683636c88e8c93aacd2cd4014))
* **deps:** update maven-gpg-plugin.version to v3.2.8 ([b09dc9d](https://github.com/Nanopublication/nanopub-java/commit/b09dc9d361ddd6953f4fbc3f41a5be6d4db83841))
* **deps:** update maven-jar-plugin.version to v3.5.0 ([3c218d5](https://github.com/Nanopublication/nanopub-java/commit/3c218d5c2e0bd3a58728a49be1dd23e474802995))
* **deps:** update maven-javadoc-plugin.version to v3.12.0 ([81d4188](https://github.com/Nanopublication/nanopub-java/commit/81d4188d8a270a442168a8ca7ddb4c8eea4121d1))
* **deps:** update maven-source-plugin.version to v3.4.0 ([bf0e3a8](https://github.com/Nanopublication/nanopub-java/commit/bf0e3a8464d382f98ba7eb9810fce45e151193ac))
* **deps:** update maven.release.plugin.version to v3.3.1 ([f71506f](https://github.com/Nanopublication/nanopub-java/commit/f71506fb35f0715afc52b1600fa1d0a5a28cc327))

### General maintenance

* **doc:** Ad info on how to build cli by yourself ([e4416b2](https://github.com/Nanopublication/nanopub-java/commit/e4416b291ff857bcf0329e40f82cfcc2beac993a))
* **QueryCall:** improve log messages ([d7c7b7f](https://github.com/Nanopublication/nanopub-java/commit/d7c7b7fe82858ef7a590593630515869ac605b14))
* setting next snapshot version [skip ci] ([b675a5e](https://github.com/Nanopublication/nanopub-java/commit/b675a5ecee95c958d65458da0d81f50ea8f30c7d))

## [1.90.0](https://github.com/Nanopublication/nanopub-java/compare/nanopub-1.89.0...nanopub-1.90.0) (2026-05-27)

### Features

* **services:** support _multi_val placeholder suffix in QueryTemplate ([f800782](https://github.com/Nanopublication/nanopub-java/commit/f800782de6dea59f1e05b1db39dc56ca2b9d2eb3))

### Tests

* **services:** cover VALUES-block removal, incl. _multi_val and SPARQL validity ([8800558](https://github.com/Nanopublication/nanopub-java/commit/8800558ef1f2f0d9550e498985c4db2575abb6c7))

### General maintenance

* setting next snapshot version [skip ci] ([72c5b92](https://github.com/Nanopublication/nanopub-java/commit/72c5b92fe35b6535208756a06f5867b0ebfb27b0))

## [1.89.0](https://github.com/Nanopublication/nanopub-java/compare/nanopub-1.88.0...nanopub-1.89.0) (2026-05-24)

### Features

* **services:** add QueryTemplate for grlc-style query nanopubs ([77c6786](https://github.com/Nanopublication/nanopub-java/commit/77c67867a9ba549e2730d521f40a315e59d6175e)), closes [#87](https://github.com/Nanopublication/nanopub-java/issues/87)

### Bug Fixes

* Command 'rocrate' did not handle duplicates correctly ([f41695c](https://github.com/Nanopublication/nanopub-java/commit/f41695c5c355f7273ec663e435beba9a03ed61ea))

### Tests

* **services:** expand QueryTemplate test coverage ([3aa0c96](https://github.com/Nanopublication/nanopub-java/commit/3aa0c96a88c9e3a0498a8120928b50fd3bc432cc))

### General maintenance

* setting next snapshot version [skip ci] ([09575dd](https://github.com/Nanopublication/nanopub-java/commit/09575dd933a31a971657ac3c5c98948a5c33615d))

## [1.88.0](https://github.com/Nanopublication/nanopub-java/compare/nanopub-1.87.1...nanopub-1.88.0) (2026-05-11)

### Features

* **server:** discover registry instances from nanopub setting ([1b64ee9](https://github.com/Nanopublication/nanopub-java/commit/1b64ee956a0a87b5da8d02404e5362bb392621e5)), closes [#78](https://github.com/Nanopublication/nanopub-java/issues/78)
* **server:** gate registry instances on Nanopub-Registry-Status ([65337ab](https://github.com/Nanopublication/nanopub-java/commit/65337abcc4d2c0176327274acbbbf1cac2ea8360)), closes [#81](https://github.com/Nanopublication/nanopub-java/issues/81)
* **services:** discover query API instances from nanopub setting ([27cf1e1](https://github.com/Nanopublication/nanopub-java/commit/27cf1e16805c53ae83f85086f78a7e00381d35e9))
* **services:** gate query instances on Nanopub-Query-Status header ([655bfac](https://github.com/Nanopublication/nanopub-java/commit/655bfac29f5ce504000a4f155e6e76a021ee37fd))
* **services:** make query parallel-call count configurable; relax instance threshold ([655e088](https://github.com/Nanopublication/nanopub-java/commit/655e088c849cf434bd629f152b8a0c06f35e3413))

### Dependency updates

* **deps:** bump gson from 2.13.2 to 2.14.0 ([52f1f86](https://github.com/Nanopublication/nanopub-java/commit/52f1f866a0dab5f3e35e5a230d9d903d55e81e36))

### Bug Fixes

* **services:** re-check query instances rejected at startup after cool-down ([04abe04](https://github.com/Nanopublication/nanopub-java/commit/04abe0447dcc51625304e428e1961caf6d290028))

### Build and continuous integration

* **release:** automate master branch update after release ([6eea1b5](https://github.com/Nanopublication/nanopub-java/commit/6eea1b533f8f038d9ec881c9d63ef284eaa53a6a))

### General maintenance

* remove old release notes ([62700f5](https://github.com/Nanopublication/nanopub-java/commit/62700f5cecb8de7f7e96a6246d45fc933cde045d))
* setting next snapshot version [skip ci] ([8035895](https://github.com/Nanopublication/nanopub-java/commit/80358953ddeaf338d71d238058d1190591dee512))

### Refactoring

* **server:** drop coreReady from registry ready-set ([40ef962](https://github.com/Nanopublication/nanopub-java/commit/40ef962654d6f4f0cb2fce7d54eb05c6283ac1a3))

## [1.87.1](https://github.com/Nanopublication/nanopub-java/compare/nanopub-1.87.0...nanopub-1.87.1) (2026-04-24)

### Dependency updates

* **core-deps:** update net.trustyuri:trustyuri dependency to v1.24.1 ([e10a809](https://github.com/Nanopublication/nanopub-java/commit/e10a809dc9f7432b66124fe7a60125775278bb47))
* **deps:** add semantic release ([656ae8e](https://github.com/Nanopublication/nanopub-java/commit/656ae8ef741a7ccab7bb021019cfd6ff79c15fe0))
* **deps:** update JUnit to v6.0.3 and add dependency management for JUnit BOM ([ca8acf3](https://github.com/Nanopublication/nanopub-java/commit/ca8acf36f29043bbe40bb4ecdfb3ce666063fdfb))
* **deps:** update release dependencies ([2a1fa1d](https://github.com/Nanopublication/nanopub-java/commit/2a1fa1df9d8b80baca257afa8f6b0f4dc0dc2dec))

### Documentation

* **cli:** remove unnecessary flags from installation command examples ([3280e3b](https://github.com/Nanopublication/nanopub-java/commit/3280e3bbf6d23a96017dd550db1cdc35413083a5))
* **cli:** update command usage examples and installation instructions ([369ed02](https://github.com/Nanopublication/nanopub-java/commit/369ed02e2e772d71a6a97fd35c50799cf8664971))

### Tests

* refactor assert statements with junit-jupiter assertions ([516b1d5](https://github.com/Nanopublication/nanopub-java/commit/516b1d5c35b882728f81ec86e4d5c124bf9be265))

### Build and continuous integration

* **release:** add workflow for automatic releases ([6951cc5](https://github.com/Nanopublication/nanopub-java/commit/6951cc5673b12d528c5beea269d667f196c48bfe))
* **release:** remove GPG passphrase server configuration from settings ([49e1711](https://github.com/Nanopublication/nanopub-java/commit/49e171164f365e46a0277f858811e6023ac9ef38))
* **release:** update GPG configuration and credentials for CI workflow ([863b1f4](https://github.com/Nanopublication/nanopub-java/commit/863b1f4b8447a96bc12c85fe4713d0653a141613))
* **release:** update Java setup and testing strategy ([5f07a87](https://github.com/Nanopublication/nanopub-java/commit/5f07a87ceca9dd94ca16de2fbe0021122fafa970))

### General maintenance

* add Maven settings for publishing releases ([a233964](https://github.com/Nanopublication/nanopub-java/commit/a233964242af7b7f15dbaa48c4016454977ad0f2))
* **cli:** add CLI installer script for nanopub ([dceda8f](https://github.com/Nanopublication/nanopub-java/commit/dceda8fd2e9dab236ad940937b52b2a825c1d3fe))
* **cli:** update installation scripts for compatibility with Unix and Windows ([aa7d250](https://github.com/Nanopublication/nanopub-java/commit/aa7d2502d30ebaf1732e0721ac3d28ad089e7948))
* **dockerfile:** JAR is now built only once and packed into the Docker image ([29b7b6d](https://github.com/Nanopublication/nanopub-java/commit/29b7b6d0e369d90355b86794957a782b74d4b6cf))
* **docker:** update Dockerfile for versioned builds and use wrapper script ([e6500d1](https://github.com/Nanopublication/nanopub-java/commit/e6500d1ada6130d0b068e493e7362bd245565581))
* **gitignore:** add node_modules directory to ignore list ([60557bf](https://github.com/Nanopublication/nanopub-java/commit/60557bf0f81f6bf8b4b51281a8b6eea3ca74b113))
* refactor dependency versions and add GPG configuration for release ([41a5282](https://github.com/Nanopublication/nanopub-java/commit/41a52821f3b71dfc91c046cd38efae596915766c))
* **release-config:** add JAR file to GitHub release asset for CLI usage ([9977246](https://github.com/Nanopublication/nanopub-java/commit/99772469c2e16f9c0df622a0ad4759464ed5e145))
* **sem-release:** add configuration for Maven and Docker publishing ([7f0b6e4](https://github.com/Nanopublication/nanopub-java/commit/7f0b6e4767d93e08fab9c19e0b04f82d47bf939a))
* **sem-release:** update Docker build commands and asset label for versioning ([01d16c6](https://github.com/Nanopublication/nanopub-java/commit/01d16c6dc384ffb16bc4d15651cc3c38524b9e34))
