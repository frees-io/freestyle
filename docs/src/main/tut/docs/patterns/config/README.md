---
layout: docs
title: Configuration
permalink: /docs/patterns/config/
---

## Configuration

The `Config` effect algebra is part of the `frees-config` module, and it allows obtaining values from configuration files at any point in the monadic computation flow of a Freestyle program.
The current implementation exposes the most important combinators found in [Typesafe Config](https://github.com/typesafehub/config).

To enable this integration, you can depend on _frees-config_:

[comment]: # (Start Replace)

```scala
libraryDependencies += "io.frees" %% "frees-config" % "0.6.2"
```

[comment]: # (End Replace)

### Operations

The set of abstract operations of the `ConfigM` algebra and `Config` object are specified as follows:

```scala
sealed trait Config {
  def hasPath(path: String): Boolean
  def config(path: String): Option[Config]
  def string(path: String): Option[String]
  def boolean(path: String): Option[Boolean]
  def int(path: String): Option[Int]
  def double(path: String): Option[Double]
  def stringList(path: String): List[String]
  def duration(path: String, unit: TimeUnit): Option[Long]
}

@free sealed trait ConfigM {
  def load: FS[Config]
  def empty: FS[Config]
  def parseString(s: String): FS[Config]
  def loadAs[T: ConfigDecoder]: FS[T]
  def parseStringAs[T: ConfigDecoder](s: String): FS[T]
}
```

The _frees-config_ module contains a built-in handler which you can use out of the box with target types such as `Try`, `Future`, `monix.eval.Task`, `fs2.Task`, and any other type in general that can satisfy a `MonadError[M, Throwable]` constraint.

### Example by using Case Classy

Freestyle integrates with the Case Classy library to make it easy to decode Typesafe config - or other untyped structured data - into case class hierarchies.

In the following example, we will review the steps we have to take to use the Case Classy library along with config algebra in a pure program.
 
Provided we have a configuration file in our classpath following the Typesafe config conventions called `application.conf` with the following value:

```
disallowedStates = ["reverted", "closed"]
```

Before we do anything else, we’ll need to add the usual set of imports from Freestyle and cats to create our algebras:

```tut:silent
import freestyle.free._
import freestyle.free.implicits._
import cats._
import cats.implicits._

import scala.util.Try
```

We'll create a case class containing all the config values required by our program:

```tut:book
case class AppConfig(disallowedStates: List[String])
``` 

Additionally, we have to write the config decoder for that case class:

```tut:book
import classy.config._

implicit val configDecoder: ConfigDecoder[AppConfig] =
  readConfig[List[String]]("disallowedStates").map(AppConfig.apply)
``` 

We will define a simple algebra with a stub handler that returns a list of issue states for illustration purposes:

```tut:book
@free trait IssuesService {
  def states: FS[List[String]]
}

implicit val issuesServiceHandler: IssuesService.Handler[Try] = new IssuesService.Handler[Try] {
  def states: Try[List[String]] = Try(List("open", "reverted", "in progress", "closed"))
}
```

At this point, we may aggregate our issues algebra with any other algebras in a _@module_ which will automatically compose monadic actions
derived from using different algebras:

```tut:book
import freestyle.free.config._
import freestyle.free.config.implicits._

@module trait App {
  val issuesService: IssuesService
  val config: ConfigM
}
```

And finally, we can create a program and compose our config algebra in the same monadic comprehension along with our issues service. As you may observe, we're calling `loadAs[AppConfig]` method to populate config values into our case class.

```tut:book
def filteredStates[F[_]](implicit app : App[F]): FreeS[F, List[String]] =
  for {
    currentStates <- app.issuesService.states
    config <- app.config.loadAs[AppConfig]
  } yield currentStates.filterNot(config.disallowedStates.contains)
```

Once we have a program, we can interpret it to our desired runtime, in this case `scala.util.Try`:

```tut:book
filteredStates[App.Op].interpret[Try]
```

### Example by using Typesafe Config

If we decide to use the Typesafe Config object instead of the Case Classy library, we won't need to create a case class or a config decoder.

So, given the example described above, the required code will be pretty similar except we'll load the Config object and use the methods that it provides:

```
tut:book
def filteredStates[F[_]](implicit app : App[F]): FreeS[F, List[String]] =
  for {
    currentStates <- app.issuesService.states
	  config <- app.config.load
	  disallowedStates = config.stringList("disallowedStates")
  } yield currentStates.filterNot(disallowedStates.getOrElse(Nil).contains)
```