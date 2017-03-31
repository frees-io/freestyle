layout: docs
title: Doobie
permalink: /docs/integrations/doobie/
---

# Doobie integration

It is easy to embed a doobie program in a freestyle program using the _freestyle-doobie_ module. This module provides a very simple algebra with only one operation: `transact`. Handlers for this algebra are available if there is a doobie `Transactor` for the target type.


The standard freestyle and cats imports:

```scala
import freestyle._
import freestyle.implicits._

import cats.implicits._
```

And some imports for the _freestyle-doobie_ module and doobie itself:

```scala
import freestyle.doobie._
import freestyle.doobie.implicits._

import _root_.doobie.imports._
import _root_.doobie.h2.h2transactor._
```

We will ask the database for a `Person`. To keep this example consise, we just use a simple query and don't bother to create actual tables.

```scala
case class Person(name: String, birthYear: Int)
// defined class Person

val getPerson: ConnectionIO[Person] = sql"SELECT 'Alonzo Church', 1903".query[Person].unique
// getPerson: doobie.imports.ConnectionIO[Person] = Free(...)
```

We can embed this doobie `ConnectionIO` program in a freestyle program. We start with the most trivial case by only using the `DoobieM` algebra.

```scala
val doobieFrees: FreeS[DoobieM.Op, Person] =
  DoobieM[DoobieM.Op].transact(getPerson)
// doobieFrees: freestyle.FreeS[freestyle.doobie.DoobieM.Op,Person] = Free(...)
```

To execute this `FreeS` program we need a doobie `Transactor` for our target type; in this example we have chosen `fs2.Task`.

```scala
import _root_.fs2.Task
// import _root_.fs2.Task

implicit val xa: Transactor[Task] =
  H2Transactor[Task]("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "sa", "").unsafeRunSync.
    toOption.getOrElse(throw new Exception("Could not create example transactor"))
// xa: doobie.imports.Transactor[fs2.Task] = doobie.h2.h2transactor$H2Transactor@4eaa85cc
```

By using the _fs2-cats_ interop project, we get the necessary type class instances for `Task`, so we can translate our `FreeS` program into a `Task`.

```scala
import _root_.fs2.interop.cats._
// import _root_.fs2.interop.cats._

val task = doobieFrees.exec[Task]
// task: fs2.Task[Person] = Task
```

To check if we actually get Alonzo Church as a `Person` we can use `Task#unsafeRunSync` in this example.

```scala
task.unsafeRunSync.toOption
// res2: Option[Person] = Some(Person(Alonzo Church,1903))
```

## `DoobieM` in a module

Using only `DoobieM` is however not exactly useful as in that case it just adds an extra level of indirection on top of `ConnectionIO`. As a more realistic example we will use `DoobieM` in a module together with another algebra.


```scala
@free trait Calc[F[_]] {
  def subtract(a: Int, b: Int): FreeS[F, Int]
}
// defined trait Calc
// defined object Calc

@module trait Example[F[_]] {
  val doobieM: DoobieM[F]
  val calc: Calc[F]
}
// defined trait Example
// defined object Example
```

A `Handler[Task]` for `Calc` :

```scala
implicit val calcHandler: Calc.Handler[Task] =
  new Calc.Handler[Task] {
    def subtract(a: Int, b: Int): Task[Int] = Task.now(a - b)
  }
// calcHandler: Calc.Handler[fs2.Task] = $anon$1@15e5bd5d
```

A program using the `Example` module:

```scala
def example[F[_]: DoobieM](implicit example: Example[F]): FreeS[F, (Person, Int)] =
  for {
    person <- getPerson.liftFS[F]  // analogous to example.doobieM.transact(getPerson)
    age    <- example.calc.subtract(2017, person.birthYear)
  } yield (person, age)
// example: [F[_]](implicit evidence$1: freestyle.doobie.DoobieM[F], implicit example: Example[F])freestyle.FreeS[F,(Person, Int)]
```

We can use `Example.Op` as the functor and translate the resulting program to `Task`.

```scala
val task2 = example[Example.Op].exec[Task]
// task2: fs2.Task[(Person, Int)] = Task

task2.unsafeRunSync.toOption
// res3: Option[(Person, Int)] = Some((Person(Alonzo Church,1903),114))
```
