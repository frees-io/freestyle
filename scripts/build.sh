#!/bin/bash

set -e

DOCS_REPO="https://github.com/frees-io/freestyle-docs.git"
INTEGRATIONS_REPO="https://github.com/frees-io/freestyle-integrations.git"

VERSION="$(grep -F -m 1 'version in ThisBuild :=' version.sbt)"; VERSION="${VERSION#*\"}"; VERSION="${VERSION%\"*}"

echo "Checking projects $DOCS_REPO and $INTEGRATIONS_REPO for freestyle version $VERSION"

set -x
if [ $(git ls-remote $INTEGRATIONS_REPO | grep "refs/heads/$TRAVIS_BRANCH" | wc -l) = 1 ] ; then
    git clone --branch $TRAVIS_BRANCH --depth=1 $INTEGRATIONS_REPO
else
    git clone --branch master --depth=1 $INTEGRATIONS_REPO
fi
set +x

sbt ++$TRAVIS_SCALA_VERSION publishLocal
cd freestyle-integrations && sbt ++$TRAVIS_SCALA_VERSION -Dfrees.version=$VERSION "clean" "compile" "test" "publishLocal" && cd ..

if [ "$TRAVIS_SCALA_VERSION" = "2.12.2" ]; then
  set -x
  if [ $(git ls-remote $DOCS_REPO | grep "refs/heads/$TRAVIS_BRANCH" | wc -l) = 1 ] ; then
      git clone --branch $TRAVIS_BRANCH --depth=1 $DOCS_REPO
  else
      git clone --branch master --depth=1 $DOCS_REPO
  fi
  set +x

  cd freestyle-docs && sbt ++$TRAVIS_SCALA_VERSION -Dfrees.version=$VERSION "clean" "tut"  && cd ..
fi
