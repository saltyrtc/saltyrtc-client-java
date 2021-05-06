# SaltyRTC Java Client

[![Build status](https://circleci.com/gh/saltyrtc/saltyrtc-client-java.svg?style=shield&circle-token=:circle-token)](https://circleci.com/gh/saltyrtc/saltyrtc-client-java)
[![Codacy](https://img.shields.io/codacy/grade/d322a8e504ef4461b4cd2a2b17d0fa2b/master.svg)](https://www.codacy.com/app/saltyrtc/saltyrtc-client-java/dashboard)
[![Java Version](https://img.shields.io/badge/java-8%2B-orange.svg)](https://github.com/saltyrtc/saltyrtc-client-java)
[![License](https://img.shields.io/badge/license-MIT%20%2F%20Apache%202.0-blue.svg)](https://github.com/saltyrtc/saltyrtc-client-java)
[![CII Best Practices](https://bestpractices.coreinfrastructure.org/projects/535/badge)](https://bestpractices.coreinfrastructure.org/projects/535)
[![Chat on Gitter](https://badges.gitter.im/saltyrtc/Lobby.svg)](https://gitter.im/saltyrtc/Lobby)

This is a [SaltyRTC](https://github.com/saltyrtc/saltyrtc-meta) v1
implementation for Java 8+.

## Installing

The package is available on Maven Central.

Gradle:

```groovy
compile 'org.saltyrtc:saltyrtc-client:0.14.1'
```

Maven:

```xml
<dependency>
  <groupId>org.saltyrtc</groupId>
  <artifactId>saltyrtc-client</artifactId>
  <version>0.14.1</version>
  <type>pom</type>
</dependency>
```

## Usage / Documentation

Documentation can be found at
[https://saltyrtc.github.io/saltyrtc-client-java/](https://saltyrtc.github.io/saltyrtc-client-java/).

Plase note that instances of this library are not considered thread-safe. Thus, an application
using more than one thread needs to take care of synchronisation itself.

## Manual Testing

To try a development version of the library, you can build a local version to
the local maven repository (usually at `$HOME/.m2/repository/`):

    ./gradlew publishToMavenLocal

Include it in your project like this:

    repositories {
        ...
        mavenLocal()
    }

## Coding Guidelines

Unfortunately we cannot use all Java 8 features, in order to be compatible with
Android API <24. Please avoid using the following APIs:

- `java.lang.annotation.Repeatable`
- `AnnotatedElement.getAnnotationsByType(Class)`
- `java.util.stream`
- `java.lang.FunctionalInterface`
- `java.lang.reflect.Method.isDefault()`
- `java.util.function`
- `java.util.Optional`

The CI tests contains a script to ensure that these APIs aren't being called. You can also run it manually:

    bash .circleci/check_android_support.sh

## Automated Testing

### 1. Preparing the Server

First, clone the `saltyrtc-server-python` repository.

    git clone https://github.com/saltyrtc/saltyrtc-server-python
    cd saltyrtc-server-python

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

### Signing

Releases are signed with the following PGP ED25519 public key:

    sec   ed25519 2021-05-05 [SC] [expires: 2025-05-04]
          27655CDD319B686A73661526DCD186BEB204C8FD
    uid           SaltyRTC (Release signing key)

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

    Copyright (c) 2016-2021 Threema GmbH

    Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
    or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
    copied, modified, or distributed except according to those terms.
