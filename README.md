# SaltyRTC Java Client

[![Travis](https://img.shields.io/travis/saltyrtc/saltyrtc-client-java/master.svg)](https://travis-ci.org/saltyrtc/saltyrtc-client-java)
[![Codacy](https://img.shields.io/codacy/grade/d322a8e504ef4461b4cd2a2b17d0fa2b/master.svg)](https://www.codacy.com/app/saltyrtc/saltyrtc-client-java/dashboard)
[![Java Version](https://img.shields.io/badge/java-7%2B-orange.svg)](https://github.com/saltyrtc/saltyrtc-client-java)
[![License](https://img.shields.io/badge/license-MIT%20%2F%20Apache%202.0-blue.svg)](https://github.com/saltyrtc/saltyrtc-client-java)
[![CII Best Practices](https://bestpractices.coreinfrastructure.org/projects/535/badge)](https://bestpractices.coreinfrastructure.org/projects/535)

This is a [SaltyRTC](https://github.com/saltyrtc/saltyrtc-meta) implementation
for Java 7+.

The development is still ongoing, the current version is only at alpha-level
and should not be used for production yet.


## Installing

The package is available [on Bintray](https://bintray.com/saltyrtc/maven/saltyrtc-client/).

Gradle:

```groovy
compile 'org.saltyrtc.client:saltyrtc-client:0.8.2'
```

Maven:

```xml
<dependency>
  <groupId>org.saltyrtc.client</groupId>
  <artifactId>saltyrtc-client</artifactId>
  <version>0.8.2</version>
  <type>pom</type>
</dependency>
```

## Usage / Documentation

Documentation can be found at
[http://saltyrtc.org/saltyrtc-client-java/](http://saltyrtc.org/saltyrtc-client-java/).


## Security

### Dependency Verification

This project uses [gradle-witness](https://github.com/WhisperSystems/gradle-witness)
to make sure that you always get the exact same versions of your dependencies.

These are the SHA256 hashes for the published releases of this project:

- v0.8.2: `386ee658e3c365b67c562632d469334cb5dd987b1c67b79bbdb65ca246edc89c`
- v0.8.1: `3e39f14f75d8b9a374c667c4f6b562ec6d2a43f751140ae4e023a07d58ea6bd9`
- v0.8.0: `82f1b3161e6775f460c64f34d6b9a7cf0f2956f30da5e51f3f30872ab19e995b`
- v0.7.1: `677d17be2c5ea209275acd872a3874305f8b064ae2aed741bc15aaf764ec0024`
- v0.7.0: `db3e17e0b8e1ad7ab2ccf2529d292f7afda6ad52b146fe5da396844ad4d0b5c4`
- v0.6.2: `f7fa5c46c946e08867d5ffe9c819e29047068cf18e2acf72a1182493099ed807`
- v0.6.1: `7861660e81377b525313dc136c74e758abde05f154e52309251b761ac5c8fe0e`
- v0.6.0: `394f5ce12bada22c483cc86ebc92598743f143b250e49d18e3c2a1292cf5abdc`

### Responsible Disclosure / Reporting Security Issues

Please report security issues directly to one or both of the following contacts:

- Danilo Bargen
    - Email: mail@dbrgn.ch
    - Threema: EBEP4UCA
    - GPG: [EA456E8BAF0109429583EED83578F667F2F3A5FA][keybase-dbrgn]
- Lennart Grahl
    - Email: lennart.grahl@gmail.com
    - Threema: MSFVEW6C
    - GPG: [3FDB14868A2B36D638F3C495F98FBED10482ABA6][keybase-lgrahl]

[keybase-dbrgn]: https://keybase.io/dbrgn
[keybase-lgrahl]: https://keybase.io/lgrahl


## Publishing

Set variables:

    export VERSION=X.Y.Z
    export GPG_KEY=E7ADD9914E260E8B35DFB50665FDE935573ACDA6
    export BINTRAY_USER=...
    export BINTRAY_KEY=...

Update version numbers:

    vim -p build.gradle README.md CHANGELOG.md docs/docs/installing.md

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

    Copyright (c) 2016-2017 Threema GmbH

    Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
    or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
    copied, modified, or distributed except according to those terms.
