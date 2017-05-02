---
layout: docs
title: Doobie
permalink: /docs/integrations/doobie/
---

# Doobie integration

It's easy to embed a doobie program in a Freestyle program using the _freestyle-doobie_ module. This module provides a very simple algebra with only one operation: `transact`. Handlers for this algebra are available if there is a doobie `Transactor` for the target type.

The standard freestyle and cats imports:

```tut:silent
import freestyle._
import freestyle.implicits._

import cats.implicits._
```

And imports for the _freestyle-doobie_ module and doobie itself:

```tut:silent
import freestyle.doobie._
import freestyle.doobie.implicits._

import _root_.doobie.imports._
import _root_.doobie.h2.h2transactor._
```

We will ask the database for a `Person`. To keep this example concise, we just use a simple query and don't bother to create actual tables:

```tut:book
case class Person(name: String, birthYear: Int)

val getPerson: ConnectionIO[Person] = sql"SELECT 'Alonzo Church', 1903".query[Person].unique
```

We can embed this doobie `ConnectionIO` program in a freestyle program. We start with the most trivial case by only using the `DoobieM` algebra:

```tut:book
val doobieFrees: FreeS[DoobieM.Op, Person] =
  DoobieM[DoobieM.Op].transact(getPerson)
```

To execute this `FreeS` program, we need a doobie `Transactor` for our target type; in this example we have chosen `fs2.Task`:

```tut:book
import _root_.fs2.Task

implicit val xa: Transactor[Task] =
  H2Transactor[Task]("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "sa", "").unsafeRunSync.
    toOption.getOrElse(throw new Exception("Could not create example transactor"))
```

By using the _fs2-cats_ interop project, we get the necessary type class instances for `Task`, so we can translate our `FreeS` program into a `Task`:

```tut:book
import _root_.fs2.interop.cats._

val task = doobieFrees.interpret[Task]
```

To check if we actually get Alonzo Church as a `Person`, we can use `Task#unsafeRunSync` in this example:

```tut:book
task.unsafeRunSync.toOption
```

## `DoobieM` in a module

Only using `DoobieM` is not exactly useful however, as it just adds an extra level of indirection on top of `ConnectionIO` in this case. As a more realistic example, we will use `DoobieM` in a module together with another algebra:


```tut:book
@free trait Calc {
  def subtract(a: Int, b: Int): FS[Int]
}

@module trait Example {
  val doobieM: DoobieM
  val calc: Calc
}
```

A `Handler[Task]` for `Calc`:

```tut:book
implicit val calcHandler: Calc.Handler[Task] =
  new Calc.Handler[Task] {
    def subtract(a: Int, b: Int): Task[Int] = Task.now(a - b)
  }
```

A program using the `Example` module:

```tut:book
def example[F[_]: DoobieM](implicit example: Example[F]): FreeS[F, (Person, Int)] =
  for {
    person <- getPerson.liftFS[F]  // analogous to example.doobieM.transact(getPerson)
    age    <- example.calc.subtract(2017, person.birthYear)
  } yield (person, age)
```

We can use `Example.Op` as the functor and translate the resulting program to `Task`:

```tut:book
val task2 = example[Example.Op].interpret[Task]

task2.unsafeRunSync.toOption
```
