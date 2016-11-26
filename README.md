# SaltyRTC Java Client

[![Travis](https://img.shields.io/travis/saltyrtc/saltyrtc-client-java/master.svg)](https://travis-ci.org/saltyrtc/saltyrtc-client-java)
[![Codacy](https://img.shields.io/codacy/grade/d322a8e504ef4461b4cd2a2b17d0fa2b/master.svg)](https://www.codacy.com/app/saltyrtc/saltyrtc-client-java/dashboard)
[![Java Version](https://img.shields.io/badge/java-7%2B-orange.svg)](https://github.com/saltyrtc/saltyrtc-client-java)
[![License](https://img.shields.io/badge/license-MIT%20%2F%20Apache%202.0-blue.svg)](https://github.com/saltyrtc/saltyrtc-client-java)

This is a [SaltyRTC](https://github.com/saltyrtc/saltyrtc-meta) implementation
for Java 7+.

The development is still ongoing, the current version is only at alpha-level
and should not be used for production yet.


## Installing

The package is available [on Bintray](https://bintray.com/saltyrtc/maven/saltyrtc-client/).

Gradle:

```groovy
compile 'org.saltyrtc.client:saltyrtc-client:0.7.1'
```

Maven:

```xml
<dependency>
  <groupId>org.saltyrtc.client</groupId>
  <artifactId>saltyrtc-client</artifactId>
  <version>0.7.1</version>
  <type>pom</type>
</dependency>
```


## Logging

The library uses the slf4j logging API. Configure a logger (e.g. slf4j-simple)
to see the log output.


## Dependency Verification

This project uses [gradle-witness](https://github.com/WhisperSystems/gradle-witness)
to make sure that you always get the exact same versions of your dependencies.


## Hashes

These are the SHA256 hashes for the published releases of this project:

- v0.7.1: `677d17be2c5ea209275acd872a3874305f8b064ae2aed741bc15aaf764ec0024`
- v0.7.0: `db3e17e0b8e1ad7ab2ccf2529d292f7afda6ad52b146fe5da396844ad4d0b5c4`
- v0.6.2: `f7fa5c46c946e08867d5ffe9c819e29047068cf18e2acf72a1182493099ed807`
- v0.6.1: `7861660e81377b525313dc136c74e758abde05f154e52309251b761ac5c8fe0e`
- v0.6.0: `394f5ce12bada22c483cc86ebc92598743f143b250e49d18e3c2a1292cf5abdc`


## Publishing

Set variables:

    export VERSION=X.Y.Z
    export GPG_KEY=E7ADD9914E260E8B35DFB50665FDE935573ACDA6
    export BINTRAY_USER=...
    export BINTRAY_KEY=...

Update version numbers:

    vim -p build.gradle README.md CHANGELOG.md

Build:

    ./gradlew build publish

Add hash to README.md:

    sha256sum build/libs/saltyrtc-client-java.jar

Add and commit:

    git commit -m "Release v${VERSION}"

Publish the library to Bintray:

    ./gradlew bintrayUpload

Tag and push:

    git tag -s -u ${GPG_KEY} v${VERSION} -m "Version ${VERSION}"
    git push && git push --tags

## License

    Copyright (c) 2016 Threema GmbH

    Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
    or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
    copied, modified, or distributed except according to those terms.
