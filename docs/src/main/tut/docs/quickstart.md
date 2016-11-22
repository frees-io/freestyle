---
layout: docs
title: Quick Start Guide
---

# Quick Start

**Freestyle** is a library that enables building large-scale modular Scala applications and libraries on top of Free monads/applicatives.

## Getting Started

Freestyle is compatible with both Scala JVM and Scala.js.

This project supports Scala 2.10, 2.11 and 2.12. The project is based on macro paradise.

To use the project, add the following to your build.sbt:

```scala
addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)
```

For Scala.jvm

```scala
libraryDependencies += "io.freestyle" %% "freestyle" % "0.1.0"
```

For Scala.js

```scala
libraryDependencies += "io.freestyle" %%% "freestyle" % "0.1.0"
```

## Algebras

Freestyle main feature is the definition of `Free` algebras without boilerplate that support both sequential and parallel
style computations. 

In the example below we will define two algebras with intermixed sequential and parallel computations.

```tut:silent
import io.freestyle._
import io.freestyle.implicits._

@free trait Validation[F[_]] {
  def minSize(s: String, n: Int): FreeS.Par[F, Boolean]
  def hasNumber(s: String): FreeS.Par[F, Boolean]
}

@free trait Interaction[F[_]] {
  def tell(msg: String): FreeS[F, Unit]
  def ask(prompt: String): FreeS[F, String]
}
```

Learn more about [algebras]() and how Freestyle works behind the scenes in the extended [documentation]()

## Modules

Freestyle algebras can be combined into `@module` definitions which provide agreggation and unification over the
parametrization of Free programs.

```tut:silent
@module trait Application[F[_]] {
  val validation: Validation[F]
  val interaction: Interaction[F]
}
```

Freestyle automatically wires all dependencies through implicit evidences that are generated so you don't have to worry about the boilerplate required to build Free based programs.

Once you have these abstract definitions you can combine them in whichever way you want. Freestyle supports nested modules enabling
onion style architectures of any arbitrary depth.

Learn more about [modules]() and how Freestyle works behind the scenes in the extended [documentation]()

## Building programs

Abstract definitions it's all it takes to start building programs that support sequential and parallel operations and that are 
entirely decoupled from their runtime interpretation.

The example below combines both algebras to produce a more complex program

```tut:silent
def program[F[_]](implicit A: Application[F]) = {
  import A._
  import cats.implicits._

  for {
    userInput <- interaction.ask("Give me something with at least 3 chars and a number on it")
    valid <- (validation.minSize(userInput, 3) |@| validation.hasNumber(userInput)).map(_ && _).seq
    _ <- if (valid)
            interaction.tell("awesomesauce!") 
         else
            interaction.tell(s"$userInput is not valid")
  } yield ()
}
```

## Running programs

In order to run programs we need interpreters. We define interpreters providing implementations for the operations defined in our algebras.

```tut:silent
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

implicit val validationInterpreter = new Validation.Interpreter[Future] {
  override def minSizeImpl(s: String, n: Int): Future[Boolean] = Future(s.size >= n)
  override def hasNumberImpl(s: String): Future[Boolean] = Future(s.exists(c => "0123456789".contains(c)))
}

implicit val interactionInterpreter = new Interaction.Interpreter[Future] {
  override def tellImpl(s: String): Future[Unit] = Future.successful(println(s))
  override def askImpl(s: String): Future[String] = Future.successful { println(s); "This could have been user input 1" }
}
```

The mere fact that you provide implicit evidences for the individual steps enables Freestyle to automatically discover
and unify all interpreters involved in a program definition.

At this point we can run our pure programs at the edge of the world.

```tut:silent
import cats.implicits._
import scala.concurrent.duration.Duration
import scala.concurrent.Await

val futureValue = program[Application.T].exec[Future]
Await.result(futureValue, Duration.Inf) //blocking only for demo purposes. Don't do this at home.
```

## There is more

You may want to consider using Freestyle if among your concerns are:

- Decoupling program declaration from runtime interpretation
- Automatic composition of dispair monadic/applicative style actions originating from independent ADTs.
- Automatic onion style architectures through composable modules without the complexity of manually aligning Coproducts and interpreters.
- Boilerplate-free application and libraries.

Freestyle optionally includes

- Ready to use integrations to achieve parallelism through [`scala.concurrent.Future`](), [`Akka`]() Actors and [`Monix`]() Task.
- Ready to use integrations that cover most of the commons applications concerns such as [logging](), [configuration](), [dependency injection](), [persistence](), etc.

Learn more about how Freestyle works behind the scenes in the extended [documentation](algebras.html)
