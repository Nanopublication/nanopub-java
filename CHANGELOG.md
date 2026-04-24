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
