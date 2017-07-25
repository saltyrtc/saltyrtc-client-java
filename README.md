# SaltyRTC Java Client

[![Travis](https://img.shields.io/travis/saltyrtc/saltyrtc-client-java/master.svg)](https://travis-ci.org/saltyrtc/saltyrtc-client-java)
[![Codacy](https://img.shields.io/codacy/grade/d322a8e504ef4461b4cd2a2b17d0fa2b/master.svg)](https://www.codacy.com/app/saltyrtc/saltyrtc-client-java/dashboard)
[![Java Version](https://img.shields.io/badge/java-7%2B-orange.svg)](https://github.com/saltyrtc/saltyrtc-client-java)
[![License](https://img.shields.io/badge/license-MIT%20%2F%20Apache%202.0-blue.svg)](https://github.com/saltyrtc/saltyrtc-client-java)
[![CII Best Practices](https://bestpractices.coreinfrastructure.org/projects/535/badge)](https://bestpractices.coreinfrastructure.org/projects/535)

This is a [SaltyRTC](https://github.com/saltyrtc/saltyrtc-meta) v1
implementation for Java 7+.

**Warning: This is beta software. Use at your own risk. Testing and review is
welcome!**

## Installing

The package is available [on Bintray](https://bintray.com/saltyrtc/maven/saltyrtc-client/).

Gradle:

```groovy
compile 'org.saltyrtc.client:saltyrtc-client:0.10.1'
```

Maven:

```xml
<dependency>
  <groupId>org.saltyrtc.client</groupId>
  <artifactId>saltyrtc-client</artifactId>
  <version>0.10.1</version>
  <type>pom</type>
</dependency>
```

## Usage / Documentation

Documentation can be found at
[https://saltyrtc.github.io/saltyrtc-client-java/](https://saltyrtc.github.io/saltyrtc-client-java/).

## Manual Testing

To try a development version of the library, you can build a local version to
the maven repository at `/tmp/maven`:

    ./gradlew uploadArchives

Include it in your project like this:

    repositories {
        ...
        maven { url "/tmp/maven" }
    }

## Automated Testing

### 1. Preparing the Server

First, clone the `saltyrtc-server-python` repository.

    git clone https://github.com/saltyrtc/saltyrtc-server-python
    cd saltyrtc-server.python

Then create a test certificate for localhost, valid for 5 years.

    openssl req -new -newkey rsa:1024 -nodes -sha256 \
        -out saltyrtc.csr -keyout saltyrtc.key \
        -subj '/C=CH/O=SaltyRTC/CN=localhost/'
    openssl x509 -req -days 1825 \
        -in saltyrtc.csr \
        -signkey saltyrtc.key -out saltyrtc.crt

Create a Java keystore containing this certificate.

    keytool -import -trustcacerts -alias root \
        -file saltyrtc.crt -keystore saltyrtc.jks \
        -storetype JKS -storepass saltyrtc -noprompt

Create a Python virtualenv with dependencies:

    python3 -m virtualenv venv
    venv/bin/pip install .[logging]

Finally, start the server with the following test permanent key:

    export SALTYRTC_SERVER_PERMANENT_KEY=0919b266ce1855419e4066fc076b39855e728768e3afa773105edd2e37037c20 # Public: 09a59a5fa6b45cb07638a3a6e347ce563a948b756fd22f9527465f7c79c2a864
    venv/bin/saltyrtc-server -v 5 serve -p 8765 \
        -sc saltyrtc.crt -sk saltyrtc.key \
        -k $SALTYRTC_SERVER_PERMANENT_KEY

### 2. Running Tests

Make sure that the certificate keystore from the server is copied or symlinked
to this repository:

    ln -s path/to/saltyrtc-server-python/saltyrtc.jks

With the server started in the background and the `saltyrtc.jks` file in the
current directory, run the tests:

    ./gradlew test


## Security

### Dependency Verification

This project uses [gradle-witness](https://github.com/WhisperSystems/gradle-witness)
to make sure that you always get the exact same versions of your dependencies.

These are the SHA256 hashes for the published releases of this project:

- v0.10.1: `4b32aa260032034d3f7dfc95332a2bed9663e17176b2341b52a181c1325d6e5e`
- v0.10.0: `6cdd5ee1562fce06fd14f931b2222f15d5e5580021c4fd22643e6218fa78e69b`
- v0.9.3: `6a17b4adce4c52987a058b2ae95e57b57bed94ad8a28b6324243b3da72167bf0`
- v0.9.2: `ed16493362af0077e703d74068d74f82dca9a69d9f741bc4613c1215acfb4c15`
- v0.9.1: `13c74b79cb03e3115b19d2f79eff7523e6f03f0b122c12eec6c0a58499cb67ae`
- v0.9.0: `ea4f39d6f91953a934a5da43161dbcde2ae3d9c2bcaed2d637c4b545cc446bea`
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


## License

    Copyright (c) 2016-2017 Threema GmbH

    Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
    or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
    copied, modified, or distributed except according to those terms.
