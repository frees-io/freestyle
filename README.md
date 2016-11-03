---
layout: home
---

[![Build Status](https://travis-ci.org/47deg/freestyle.svg?branch=master)](https://travis-ci.org/47deg/freestyle) [![Join the chat at https://gitter.im/47deg/freestyle](https://badges.gitter.im/47deg/freestyle.svg)](https://gitter.im/47deg/freestyle?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

# Freestyle

**Freestyle** is a boilerplate reduction library to make working with `Free` monads based architectures a breeze.

# Rationale

[`Free`]() monads based architectures have become popular in Scala as a way to organize libraries and applications.
When using `Free` we model effects and actions as Algebraic Data Types (ADTs).
Scala emulates [`Algebraic Data Types`]() through sealed hierarchies on which each class extending the root class represents one of the monadic steps you can perform when
constructing your program. Unfortunately this results in a decent amount of boilerplate in order to properly
combine and [compose ADTs]().

Freestyle simplifies this process by automatically generating all the boilerplate you need in order to compose `Free` monads originating from unrelated ADTs.

The following Freestyle code:

```scala

import io.freestyle._
import cats.free._

@free trait Interacts[F[_]] {
  def tell(msg: String): Free[F, Unit]
  def ask(prompt: String): Free[F, String]
}

@free trait DataOps[F[_]] {
  def addCat(a: String): Free[F, String]
  def getAllCats: Free[F, List[String]]
}

@module trait Application[F[_]] {
  val interacts: Interacts[F]
  val dataOps: DataOps[F]
}

```

It's closely equivalent to:

```scala

import cats._
import cats.data._
import cats.free._
import cats.implicits._

import scala.util.Try

/** An application as a Coproduct of it's ADTs */
type Application[A] = Coproduct[Interact, DataOp, A]

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

```

Freestyle generates all the necessary boilerplate machinery including ADTs, Inject instances and companions with
proper implicits.
Eliminating this boilerplate allows developers to concentrate in the business logic by simply
describing abstract functions where one can define the needed arguments and expected return type without providing
implementations.

This is all you need to start building a Free program free of runtime interpretation.

Freestyle provides a lot more utilities and it's compatible with Hybrid approaches where you define your own
ADTs in a more traditional way and combine them with parts built with Freestyle.
More details about how Freestyle generates boilerplate and other utilities that make functional programming
with Free monads in Scala easier can be found in its [documentation]()

# Getting started

Freestyle is compatible with both Scala JVM and Scala.js.

This project supports Scala 2.10 and 2.11. The project is based on macro paradise.

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

- Documentation
  - * Algebras
  - * Modules
  - Runtime
	- Abstracting over return types
	- Purely functional state
	- Paralell and Non-Determinism
  - Application and Library composition
  - Syntax
  - Testing
  - Debugging
  - 3rd Party Integrations
  - Alternatives
  - Reporting issues
