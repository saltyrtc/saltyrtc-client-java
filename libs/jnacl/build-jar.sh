#!/bin/sh
find com -type f -name "*.java" -exec javac {} \;
jar cf ../jnacl.jar com
