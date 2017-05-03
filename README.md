
[comment]: # (Start Badges)

[![Build Status](https://travis-ci.org/47deg/freestyle.svg?branch=master)](https://travis-ci.org/47deg/freestyle) [![codecov.io](http://codecov.io/github/47deg/freestyle/coverage.svg?branch=master)](http://codecov.io/github/47deg/freestyle?branch=master) [![Maven Central](https://img.shields.io/badge/maven%20central-0.1.0-green.svg)](https://oss.sonatype.org/#nexus-search;gav~com.47deg~freestyle*) [![Latest version](https://img.shields.io/badge/freestyle-0.1.0-green.svg)](https://index.scala-lang.org/47deg/freestyle) [![License](https://img.shields.io/badge/license-Apache%202-blue.svg)](https://raw.githubusercontent.com/47deg/freestyle/master/LICENSE) [![Join the chat at https://gitter.im/47deg/freestyle](https://badges.gitter.im/47deg/freestyle.svg)](https://gitter.im/47deg/freestyle?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge) [![GitHub Issues](https://img.shields.io/github/issues/47deg/freestyle.svg)](https://github.com/47deg/freestyle/issues) [![Scala.js](http://scala-js.org/assets/badges/scalajs-0.6.15.svg)](http://scala-js.org)

[comment]: # (End Badges)

<a href="http://frees.io"><img src="docs/src/main/resources/microsite/img/freestyle_header.png" alt="A Cohesive & Pragmatic Framework of FP centric Scala libraries" width="100%"/></a>

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

+ [fetch](http://frees.io/docs/integrations/fetch/) - Integration with the Fetch library for efficient data access from heterogenous datasources.
 
+ [fs2](http://frees.io/docs/integrations/fs2/) - Integration to run fs2 Streams in Freestyle programs.

+ [monix](http://frees.io/docs/integrations/monix/) -  Instances and utilities to interpret to `monix.eval.Task`.

+ [logging](http://frees.io/docs/patterns/logging/) - A purely functional logging algebra over Verizon's Journal. 

+ [slick](http://frees.io/docs/integrations/slick/) - Embedding of DBIO actions in Freestyle programs.

+ [doobie](http://frees.io/docs/integrations/doobie/) - Embedding of Doobie ConnectionIO actions in Freestyle programs.

+ [cache](http://frees.io/docs/effects/Cache/) - A generic cache with in memory and redis based implementations.

+ http - Adapters and marshallers to run the Freestyle program in endpoint return types for [akka-http](http://frees.io/docs/integrations/akkahttp/), [finch](http://frees.io/docs/integrations/finch/), [http4s](http://frees.io/docs/integrations/http4s/) and [play](http://frees.io/docs/integrations/play/). 

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
