---
layout: docs
title: Quick Start Guide
permalink: /docs/
---

# Quick Start

**Freestyle** is a library that enables the building of large-scale modular Scala applications and libraries on top of Free monads/applicatives.

## Getting Started

Freestyle is compatible with both Scala JVM and Scala.js.

This project supports Scala 2.11 and 2.12 and is based on [scalameta](http://scalameta.org/).

To use the project, add the following to your build.sbt:

```scala
addCompilerPlugin("org.scalameta" % "paradise" % "3.0.0-M10" cross CrossVersion.full)
scalacOptions += "-Ypartial-unification"
```

[comment]: # (Start Replace)

For Scala.jvm:

```scala
libraryDependencies += "io.frees" %% "frees-core" % "0.8.2"
```

For Scala.js:

```scala
libraryDependencies += "io.frees" %%% "frees-core" % "0.8.2"
```

[comment]: # (End Replace)

## Algebras

Freestyle's core feature is the definition of `Free` boilerplate-free algebras that support both sequential and parallel style computations and all the implicit machinery required to turn them into modular programs.

In the example below, we will define two algebras with intermixed sequential and parallel computations.

```tut:book
import freestyle.free._
import freestyle.free.implicits._

@free trait Validation {
  def minSize(s: String, n: Int): FS[Boolean]
  def hasNumber(s: String): FS[Boolean]
}

@free trait Interaction {
  def tell(msg: String): FS[Unit]
  def ask(prompt: String): FS[String]
}
```

Learn more about [algebras](./core/algebras) in the extended documentation.

## Modules

Freestyle algebras can be combined into `@module` definitions which provide aggregation and unification over the parameterization of Free programs.

## Building programs

Abstract definitions are all it takes to start building programs that support sequential and parallel 
operations that are entirely decoupled from their runtime interpretation.

The example below combines both algebras to produce a more complex program:

```tut:book
@module trait Application {
  val validation: Validation
  val interaction: Interaction
  
  import cats.implicits._
  
  def program: FS.Seq[Unit] = 
    for {
      userInput <- interaction.ask("Give me something with at least 3 chars and a number on it")
      valid     <- (validation.minSize(userInput, 3), validation.hasNumber(userInput)).mapN(_ && _).freeS
      _         <- if (valid) 
                      interaction.tell("awesomesauce!") 
                   else 
                      interaction.tell(s"$userInput is not valid")
    } yield ()
}
```

Freestyle automatically wires all dependencies through implicit evidences that are generated, so you don't have to worry about the boilerplate required to build Free-based programs.

Once you have these abstract definitions, you can combine them in whichever way you want. Freestyle supports nested modules enabling onion-style architectures of any arbitrary depth.

Learn more about [modules](./core/modules) in the extended documentation.

## Running programs

In order to run programs, we need interpreters. We define interpreters by providing implementations for the operations defined in our algebras:

```tut:book
import cats.effect.IO

implicit val validationHandler = new Validation.Handler[IO] {
  override def minSize(s: String, n: Int): IO[Boolean] = IO{s.size >= n}
  override def hasNumber(s: String): IO[Boolean] = IO{s.exists(c => "0123456789".contains(c))}
}

implicit val interactionHandler = new Interaction.Handler[IO] {
  override def tell(s: String): IO[Unit] = IO{println(s)}
  override def ask(s: String): IO[String] = IO { println(s); "This could have been user input 1" }
}
```

The mere fact that you provide implicit evidences for the individual steps enables Freestyle to automatically discover and unify all interpreters involved in a program definition.

At this point, we can run our pure programs at the edge of the world:

```tut:book
import cats.implicits._
import cats.effect.IO

Application.instance.program.interpret[IO].unsafeRunSync
```

## There is more

You may want to consider using Freestyle you have any of the following concerns:

- Decoupling program declaration from runtime interpretation.
- Automatic composition of dispair monadic/applicative style actions originating from independent ADTs.
- Automatic onion-style architectures through composable modules without the complexity of manually aligning Coproducts and interpreters.
- Boilerplate-free application and libraries.

Freestyle includes ready to go Algebras and Integrations for the most common application concerns:

- Ready to use integrations to achieve parallelism through [`scala.concurrent.Future`](), [`Akka`]() Actors and [`Monix`]() Task.
- Ready to use integrations that cover most of the commons applications concerns such as [logging](), [configuration](), [dependency injection](), [persistence](), etc.
- Traditional effects stacks (reader, writer, state, error, option, either)

Learn more about how Freestyle works behind the scenes in the extended [documentation](./core/algebras) and check out the [reference application](../TODO) with examples
of multiple algebras in use.
