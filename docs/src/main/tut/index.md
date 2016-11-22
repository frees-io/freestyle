---
layout: home
---

EXPERIMENTAL WIP.

[![Build Status](https://travis-ci.org/47deg/freestyle.svg?branch=master)](https://travis-ci.org/47deg/freestyle) [![Join the chat at https://gitter.im/47deg/freestyle](https://badges.gitter.im/47deg/freestyle.svg)](https://gitter.im/47deg/freestyle?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

# Freestyle

**Freestyle** is a library that enables building large-scale modular Scala applications and libraries on top of Free monads/applicatives.

You may want to consider using Freestyle if among your concerns are:

- Decoupling program declaration from runtime interpretation
- Automatic composition of dispair monadic/applicative style actions originating from independent ADTs.
- Automatic onion style architectures through composable modules without the complexity of manually aligning Coproducts and interpreters.
- Boilerplate-free application and libraries.

Freestyle optionally includes

- Ready to use integrations to achieve parallelism through [`scala.concurrent.Future`](), [`Akka`]() Actors and [`Monix`]() Task.
- Ready to use integrations that cover most of the commons applications concerns such as [logging](), [configuration](), [dependency injection](), [persistence](), etc.

## Quick Start

Take a look at the [quick start guide](docs/quickstart.html) to understand the main features of Freestyle

## Documentation

Freestyle includes extensive [documentation](docs/algebras.html) for each one of its features and third party integrations

## Rationale

[`Free`]() monads based architectures have become popular in Scala as a way to organize libraries and applications.
When using `Free` we model effects and actions as Algebraic Data Types (ADTs).
Scala emulates [`Algebraic Data Types`]() through sealed hierarchies on which each class extending the root class represents one of the monadic steps or applicative computations you can perform when
constructing your program. Unfortunately this results in a decent amount of boilerplate in order to properly combine and [compose ADTs]().

Freestyle simplifies this process by automatically generating all the boilerplate you need in order to compose `Free` monads/applicatives operations originating from unrelated ADTs.

Freestyle also provides the necessary implicit machinery to agreggate algebras into modules and submodules to achieve Onion style architectures built atop Free.
where you can group your concerns into logical components.

Freestyle goal is to empower users unleashing the full power of Functional Programming based architectures in Scala while remaining beginner friendly.

## Example

The following Freestyle code:

```tut:silent
import io.freestyle._

@free trait Interacts[F[_]] {
  def tell(msg: String): FreeS[F, Unit]
  def ask(prompt: String): FreeS[F, String]
}

@free trait DataOps[F[_]] {
  def addCat(a: String): FreeS[F, String]
  def getAllCats: FreeS[F, List[String]]
}

@module trait Application[F[_]] {
  val interacts: Interacts[F]
  val dataOps: DataOps[F]
}
```

It's closely equivalent to it's non Freestyle version:

```tut:silent
import cats._
import cats.data._
import cats.free._
import cats.implicits._

/** User Interaction Algebra */
sealed abstract class Interact[A] extends Product with Serializable
case class Ask(prompt: String) extends Interact[String]
case class Tell(msg: String) extends Interact[Unit]

/** Data Operations Algebra */
sealed trait DataOp[A] extends Product with Serializable
case class AddCat(a: String) extends DataOp[String]
case class GetAllCats() extends DataOp[List[String]]

/** Smart Constructors */
class Interacts[F[_]](implicit I: Inject[Interact, F]) {
  def tell(msg: String): Free[F, Unit] = Free.inject[Interact, F](Tell(msg))
  def ask(prompt: String): Free[F, String] = Free.inject[Interact, F](Ask(prompt))
}

object Interacts {
  implicit def interacts[F[_]](implicit I: Inject[Interact, F]): Interacts[F] = new Interacts[F]
}

class DataOps[F[_]](implicit I: Inject[DataOp, F]) {
  def addCat(a: String): Free[F, String] = Free.inject[DataOp, F](AddCat(a))
  def getAllCats: Free[F, List[String]] = Free.inject[DataOp, F](GetAllCats())
}

object DataOps {
  implicit def dataOps[F[_]](implicit I: Inject[DataOp, F]): DataOps[F] = new DataOps[F]
}

/** An application as a Coproduct of it's ADTs */
type Application[A] = Coproduct[Interact, DataOp, A]
```

Freestyle generates all the necessary boilerplate machinery including ADTs, Inject instances and companions with proper implicits.
Eliminating this boilerplate allows developers to concentrate in the business logic by simply
describing abstract functions where one can define the needed arguments and expected return type without providing implementations.

This is all you need to start building a Free program free of runtime interpretation.

Freestyle is compatible with Hybrid approaches where you define your own ADTs in a more traditional way and combine them with parts built with Freestyle.
More details about how Freestyle generates boilerplate and other utilities that make functional programming
with Free monads in Scala easier can be found in its [documentation]()

# Dependencies

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

# Credits

Freestyle it's a project sponsored and promoted by [47 Degrees](http://47deg.com), a functional programming consultancy
specializing in Scala technologies.

Freestyle is inspired among others by [`simulacrum`](https://github.com/mpilquist/simulacrum) and it's possible thanks to great libraries such as:

- [cats](http://typelevel.org/cats)
- [kind-projector](https://github.com/non/kind-projector)
- [sbt-microsites](https://47deg.github.io/sbt-microsites/)
- [monix]()
- [akka]()
- [doobie]()
