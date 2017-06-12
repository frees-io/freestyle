
[comment]: # (Start Badges)

[![Build Status](https://travis-ci.org/frees-io/freestyle.svg?branch=master)](https://travis-ci.org/frees-io/freestyle) [![codecov.io](http://codecov.io/github/frees-io/freestyle/coverage.svg?branch=master)](http://codecov.io/github/frees-io/freestyle?branch=master) [![Maven Central](https://img.shields.io/badge/maven%20central-0.2.0-green.svg)](https://oss.sonatype.org/#nexus-search;gav~io.frees~freestyle*) [![Latest version](https://img.shields.io/badge/freestyle-0.2.0-green.svg)](https://index.scala-lang.org/frees-io/freestyle) [![License](https://img.shields.io/badge/license-Apache%202-blue.svg)](https://raw.githubusercontent.com/frees-io/freestyle/master/LICENSE) [![Join the chat at https://gitter.im/47deg/freestyle](https://badges.gitter.im/47deg/freestyle.svg)](https://gitter.im/47deg/freestyle?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge) [![GitHub Issues](https://img.shields.io/github/issues/frees-io/freestyle.svg)](https://github.com/frees-io/freestyle/issues) [![Scala.js](http://scala-js.org/assets/badges/scalajs-0.6.15.svg)](http://scala-js.org)

[comment]: # (End Badges)

<a href="http://frees.io"><img src="http://frees.io/img/poster.png" alt="A Cohesive & Pragmatic Framework of FP centric Scala libraries" width="100%"/></a>

# Build purely functional applications and libraries
Build stack-safe purely functional applications and libraries that support parallel and sequential computations where declaration is decoupled from interpretation.
Freestyle encourages programs built atop [Free algebras](http://frees.io/docs/core/algebras/) that are interpreted at the edge of your application ensuring effects are localized and performed in a controlled environment.
Applications built with Freestyle can be interpreted to any runtime semantics supported by the interpreter target type.

```scala
import freestyle._

@free trait Database {
  def get(id: UserId): FS[User]
}

@free trait Cache {
  def get(id: UserId): FS[User]
}

@module trait Persistence {
  val database: Database
  val cache: Cache
}
```

# Automatic Dependency Injection

Freestyle includes all the implicit machinery necessary to achieve seamless dependency injection of [`@free`](http://frees.io/docs/core/algebras/) and [`@module`](http://frees.io/docs/core//modules/) Algebras.
Simply require any of your `@free` or `@module` trait as implicits where needed.

```scala
def storedUsers[F[_]]
    (userId: UserId)
    (implicit persistence: Persistence[F]): FreeS[F, (User, User)] = {
  import persistence._
  for {
    cachedUser <- cache.get(userId)
    persistentUser <- database.get(userId)
  } yield (cachedUser, persistentUser)
}
```

# Ready to use integrations
Freestyle ships with [ready to use algebras and convenient syntax extensions](http://frees.io/docs/integrations/) covering most of the application concerns such as persistence, configuration, logging, etc.
In addition Freestyle includes commonly used FP [effects](http://frees.io/docs/effects/) stack such as `option`, `error`, `reader`, `writer`, `state` based on the capabilities of
the target runtime interpreters.

```scala
def loadUser[F[_]]
  (userId: UserId)
  (implicit 
    doobie: DoobieM[F], 
    logging: LoggingM[F]): FreeS[F, User] = {
    import doobie.implicits._
    for {
      user <- (sql"SELECT * FROM User WHERE userId = $userId"
                .query[User]
                .unique
                .liftFS[F])
      - <- logging.debug(s"Loaded User: ${user.userId}")
    } yield user
}
```

## Modules

+ [freestyle](http://frees.io/docs/) - Core module including building blocks for boilerplate free FP programs and apps over Free monads and cats.

+ [tagless](http://frees.io/docs/core/tagless/) - An alternative encoding to Free based on Tagless Final.

+ [effects](http://frees.io/docs/effects/) - MTL style effects such as reader, writer, state, error, and more modeled as free algebras.

+ [logging](http://frees.io/docs/patterns/logging/) - A purely functional logging algebra over Verizon's Journal.

+ [cache](http://frees.io/docs/effects/Cache/) - A generic cache with in memory and redis based implementations.

## Integrations

Freestyle integrations are located at https://github.com/frees-io/freestyle-integrations.

+ [fetch](http://frees.io/docs/integrations/fetch/) - Integration with the Fetch library for efficient data access from heterogenous datasources.

+ [fs2](http://frees.io/docs/integrations/fs2/) - Integration to run fs2 Streams in Freestyle programs.

+ [monix](http://frees.io/docs/integrations/monix/) -  Instances and utilities to interpret to `monix.eval.Task`.

+ [slick](http://frees.io/docs/integrations/slick/) - Embedding of DBIO actions in Freestyle programs.

+ [doobie](http://frees.io/docs/integrations/doobie/) - Embedding of Doobie ConnectionIO actions in Freestyle programs.

+ http - Adapters and marshallers to run the Freestyle program in endpoint return types for [akka-http](http://frees.io/docs/integrations/akkahttp/), [finch](http://frees.io/docs/integrations/finch/), [http4s](http://frees.io/docs/integrations/http4s/) and [play](http://frees.io/docs/integrations/play/).

## Freestyle Artifacts

Freestyle is compatible with both Scala JVM and Scala.js.

This project supports Scala 2.11 and 2.12. The project is based on [scalameta](http://scalameta.org/).

To use the project, add the following to your build.sbt:

```scala
addCompilerPlugin("org.scalameta" % "paradise" % "3.0.0-M8" cross CrossVersion.full)
```

[comment]: # (Start Replace)

For Scala.jvm:

```scala
// required
libraryDependencies += "io.frees" %% "freestyle"              % "0.2.0"

// optional - effects and patterns
libraryDependencies += "io.frees" %% "freestyle-effects"      % "0.2.0"
libraryDependencies += "io.frees" %% "freestyle-tagless"      % "0.2.0"
libraryDependencies += "io.frees" %% "freestyle-async"        % "0.2.0"
libraryDependencies += "io.frees" %% "freestyle-async-fs2"    % "0.2.0"
libraryDependencies += "io.frees" %% "freestyle-async-monix"  % "0.2.0"
libraryDependencies += "io.frees" %% "freestyle-cache"        % "0.2.0"
libraryDependencies += "io.frees" %% "freestyle-config"       % "0.2.0"
libraryDependencies += "io.frees" %% "freestyle-logging"      % "0.2.0"

// optional - integrations
libraryDependencies += "io.frees" %% "freestyle-cache-redis"  % "0.2.0"
libraryDependencies += "io.frees" %% "freestyle-doobie"       % "0.2.0"
libraryDependencies += "io.frees" %% "freestyle-fetch"        % "0.2.0"
libraryDependencies += "io.frees" %% "freestyle-fs2"          % "0.2.0"
libraryDependencies += "io.frees" %% "freestyle-http-akka"    % "0.2.0"
libraryDependencies += "io.frees" %% "freestyle-http-finch"   % "0.2.0"
libraryDependencies += "io.frees" %% "freestyle-http-http4s"  % "0.2.0"
libraryDependencies += "io.frees" %% "freestyle-http-play"    % "0.2.0"
libraryDependencies += "io.frees" %% "freestyle-monix"        % "0.2.0"
libraryDependencies += "io.frees" %% "freestyle-slick"        % "0.2.0"
libraryDependencies += "io.frees" %% "freestyle-twitter-util" % "0.2.0"
```

For Scala.js:

```scala
// required
libraryDependencies += "io.frees" %%% "freestyle"             % "0.2.0"

// optional - effects and patterns
libraryDependencies += "io.frees" %%% "freestyle-effects"     % "0.2.0"
libraryDependencies += "io.frees" %%% "freestyle-tagless"     % "0.2.0"
libraryDependencies += "io.frees" %%% "freestyle-async"       % "0.2.0"
libraryDependencies += "io.frees" %%% "freestyle-async-fs2"   % "0.2.0"
libraryDependencies += "io.frees" %%% "freestyle-async-monix" % "0.2.0"
libraryDependencies += "io.frees" %%% "freestyle-cache"       % "0.2.0"
libraryDependencies += "io.frees" %%% "freestyle-logging"     % "0.2.0"

// optional - integrations
libraryDependencies += "io.frees" %%% "freestyle-fetch"       % "0.2.0"
libraryDependencies += "io.frees" %%% "freestyle-fs2"         % "0.2.0"
libraryDependencies += "io.frees" %%% "freestyle-monix"       % "0.2.0"
```

[comment]: # (End Replace)

## Freestyle in the wild

If you wish to add your library here please consider a PR to include it in the list below.

★ | ★ | ★
--- | --- | ---
![scala-exercises](https://www.scala-exercises.org/assets/images/navbar_brand.svg) | [**scala-exercises**](https://www.scala-exercises.org/) | Scala Exercises is an Open Source project for learning different technologies based in the Scala Programming Language.

[comment]: # (Start Copyright)
# Copyright

Freestyle is designed and developed by 47 Degrees

Copyright (C) 2017 47 Degrees. <http://47deg.com>

[comment]: # (End Copyright)