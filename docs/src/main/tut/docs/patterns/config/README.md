---
layout: docs
title: Configuration
permalink: /docs/patterns/config/
---

## Configuration

The `Config` effect algebra is part of the `freestyle-config` module, and it allows obtaining values from configuration files at any point in the monadic computation flow of a Freestyle program.
The current implementation exposes the most important combinators found in [Typesafe Config](https://github.com/typesafehub/config).

In order to enable this integration, you can depend on _freestyle-config_:

```scala
libraryDependencies += "com.47deg" %% "freestyle-config" % "0.1.0"
```

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
}
```

The _freestyle-config_ module contains a built-in handler which you can use out of the box with target types such as `Try`, `Future`, `monix.eval.Task`, `fs2.Task`, and any other type in general that can satisfy a `MonadError[M, Throwable]` constraint.


### Example

In the following example, we will show how easy it is to add the config algebra and use it in a pure program.

Provided we have a configuration file in our classpath following the typesafe config conventions called `application.conf` with the following value:

```
disallowedStates = ["reverted", "closed"]
```

Before we do anything else, we’ll need to add the usual set of imports from freestyle and cats to create our algebras:

```tut:silent
import freestyle._
import freestyle.implicits._
```

We will define a very simple algebra with a stub handler that returns a list of issue states for illustration purposes:

```scala
@free trait IssuesService {
  def states: FS[List[String]]
}
```

At this point, we may aggregate our issues algebra with any other algebras in a _@module_ which will automatically compose monadic actions
derived from using different algebras:

```tut:book
import freestyle.config._
import freestyle.config.implicits._
import freestyle.docs.patterns.IssuesService

@module trait App[F[_]] {
  val issuesService: IssuesService[F]
  val config: ConfigM[F]
}
```

And finally, we can create a program and compose our config algebra in the same monadic comprehension along with our issues service:

```tut:book
def filteredStates[F[_]](implicit app : App[F]): FreeS[F, List[String]] =
  for {
    currentStates <- app.issuesService.states
	config <- app.config.load
	disallowedStates = config.stringList("disallowedStates")
  } yield currentStates.filterNot(disallowedStates.contains)
```

Once we have a program, we can interpret it to our desired runtime. In this case, we want to interpret it to `scala.util.Try`, for which we need two things. 
First, we have to provide a _handler_ of type `IssuesService.Handler[Try]`, to specify how to interpret the basic operations in the algebra. 
Second, we need an instance of `Monad[Try]`, for which we use the one from `cats`. 

```tut:book
import scala.util.Try
import cats.instances.try_._

implicit val issuesServiceHandler: IssuesService.Handler[Try] = new IssuesService.Handler[Try] {
  def states: Try[List[String]] = Try(List("open", "reverted", "in progress", "closed"))
}

filteredStates[App.Op].exec[Try]
```

