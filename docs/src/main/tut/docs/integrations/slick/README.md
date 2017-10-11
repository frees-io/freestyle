---
layout: docs
title: Slick
permalink: /docs/integrations/slick/
---

# Slick integration

It's easy to embed a slick program in a Freestyle program using the _freestyle-slick_ module. This module provides a very simple algebra with only one operation: `run`. Handlers for this algebra are available automatically when interpreting to `scala.concurrent.Future` or to any other target type for which an `AsyncContext` from the module `freestyle-async` is found implicitly. Freestyle already provides `AsyncContext` instances for `fs2.Task` in `freestyle-async-fs2` and `monix.eval.Task` in `freestyle-async-monix`.

Here is an example that uses the Slick integration.

The standard freestyle and cats imports:

```tut:silent
import freestyle._
import freestyle.implicits._

import cats.implicits._
```

And some imports for the _freestyle-slick_ module and slick itself:

```tut:silent
import freestyle.slick._

import _root_.slick.jdbc.JdbcBackend
import _root_.slick.jdbc.H2Profile.api._
import _root_.slick.jdbc.GetResult
```

We will ask the database for a `Person`. To keep this example concise, we just use a simple query and don't bother to create actual tables:

```tut:book
case class Person(name: String, birthYear: Int)


implicit val getPersonResult = GetResult(r => Person(r.nextString, r.nextInt))

val getPerson: DBIO[Person] = sql"SELECT 'Alonzo Church', 1903".as[Person].head
```

We can embed this slick `DBIO` program in a Freestyle program. We start with the most trivial case by only using the `SlickM` algebra:

```tut:book
val slickFrees: FreeS[SlickM.Op, Person] =
  SlickM[SlickM.Op].run(getPerson)
```

To execute this `FreeS` program, we need to import the _freestyle-slick_ module implicits.
In this Example, we are interpreting to `scala.concurrent.Future` and the following imports already include all the implicits needed to achieve that once an implicit `Database` is provided. For simplicity, we will use an `H2` in memory database:

```tut:book
import freestyle.slick.implicits._
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

implicit val db = Database.forURL("jdbc:h2:mem:docs", driver = "org.h2.Driver")

val future = slickFrees.interpret[Future]
```

To check if we actually retrieve Alonzo Church as a `Person`, we can block the future in this example:

```tut:book
import scala.concurrent.Await

Await.result(future, Duration.Inf)
```

## `SlickM` in a module

Only using `SlickM` is not exactly useful in this case as it just adds an extra level of indirection on top of `DBIO`. As a more realistic example, we will use `SlickM` in a module together with another algebra:


```tut:book
@free trait Calc {
  def subtract(a: Int, b: Int): FS[Int]
}

@module trait Example {
  val slickM: SlickM
  val calc: Calc
}
```

A `Handler[Future]` for `Calc`:

```tut:book
implicit val calcHandler: Calc.Handler[Future] =
  new Calc.Handler[Future] {
    def subtract(a: Int, b: Int): Future[Int] = Future.successful(a - b)
  }
```

A program using the `Example` module:

```tut:book
def example[F[_]: SlickM](implicit example: Example[F]): FreeS[F, (Person, Int)] =
  for {
    person <- getPerson.liftFS[F]  // analogous to example.slickM.run(getPerson)
    age    <- example.calc.subtract(2017, person.birthYear)
  } yield (person, age)
```

We can use `Example.Op` as the functor and translate the resulting program to `Future`:

```tut:book
val result = example[Example.Op].interpret[Future]

Await.result(result, Duration.Inf)
```
