#!/usr/bin/env bash

VERSION=`grep -A 1 api-extras pom.xml  | grep version | sed -rn 's/\s+<.+>(.+)<.+>/\1/p'`
wget https://github.com/UoW-IRRs/API-Extras/archive/api-extras-${VERSION}.tar.gz
tar xvzf api-extras-${VERSION}.tar.gz
cd API-Extras-api-extras-${VERSION} && mvn install -DskipTests=true -Dmaven.javadoc.skip=true -B -V
