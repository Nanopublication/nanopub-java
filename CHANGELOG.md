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
