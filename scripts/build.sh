#!/bin/bash

set -e

clone_repo () {
  if [ -z "$1" ]                           # REPO URL is mandatory
  then
    echo "-REPO URL is mandatory.-"
    return 1
  else
    echo "-REPO URL = \"$1\".-"
  fi

  set -x
  if [ $(git ls-remote $1 | grep "refs/heads/$TRAVIS_BRANCH" | wc -l) = 1 ] ; then
    git clone --branch $TRAVIS_BRANCH --depth=1 $1
  else
    git clone --branch master --depth=1 $1
  fi
  set +x

  return 0
}

DOCS_REPO="https://github.com/frees-io/freestyle-docs.git"
INTEGRATIONS_REPO="https://github.com/frees-io/freestyle-integrations.git"

VERSION="$(grep -F -m 1 'version in ThisBuild :=' version.sbt)"; VERSION="${VERSION#*\"}"; VERSION="${VERSION%\"*}"

echo "Checking projects $DOCS_REPO and $INTEGRATIONS_REPO for freestyle version $VERSION"

clone_repo $INTEGRATIONS_REPO

sbt ++$TRAVIS_SCALA_VERSION publishLocal
cd freestyle-integrations && sbt ++$TRAVIS_SCALA_VERSION -Dfrees.version=$VERSION "clean" "compile" "test" "publishLocal" && cd ..

if [ "$TRAVIS_SCALA_VERSION" = "2.12.2" ]; then
  clone_repo $DOCS_REPO

  cd freestyle-docs && sbt ++$TRAVIS_SCALA_VERSION -Dfrees.version=$VERSION "clean" "tut"  && cd ..
fi

