layout: docs
title: Cats
permalink: /docs/cats/
---

# Cats

Freestyle is build on top of Cats' `Free` and `FreeApplicative` and `FreeS` and `FreeS.Par` are just type aliases.

```tut:silent
import cats.free.{ Free, FreeApplicative }

object aliases {
  type FreeS[F[_], A] = Free[FreeApplicative[F, ?], A]

  object FreeS {
    type Par[F[_], A] = FreeApplicative[F, A]
  }
}
```

As `FreeS` is a monad and `FreeS.Par` an applicative, they can be used like any other monad/applicative with Cats.

A simple algebra with a sum and a product operation:

```tut:book
import freestyle._

@free trait Calc[F[_]] {
  def sum(a: Int, b: Int): FreeS.Par[F, Int]
  def product(a: Int, b: Int): FreeS.Par[F, Int]
}
```

A simple `Id` handler.

```tut:book
import cats.Id

implicit val idHandler = new Calc.Handler[Id] {
  def sum(a: Int, b: Int) = a + b
  def product(a: Int, b: Int) = a * b
}
```

A handler translating the `Calc` algebra to `Future`, which introduces a little bit of artificial latency.

```tut:book
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

implicit val futureHandler = new Calc.Handler[Future] {
  def sum(a: Int, b: Int) =
    Future { Thread.sleep(a * 100L); a + b }
  def product(a: Int, b: Int) =
    Future { Thread.sleep(a * 100L); a * b }
}
```

We can use `Calc#sum` to create a function which increments an integer.

We have `incrPar` and `incrSeq`, where multiple `incrPar` calls could potentially be executed in parallel.

```tut:book
val incrPar: Int => FreeS.Par[Calc.Op, Int] =
  Calc[Calc.Op].sum(_, 1)

val incrSeq: Int => FreeS[Calc.Op, Int] =
  incrPar.andThen(_.freeS)
```

## Traversing

We will use `traverse` to increment a list of integers with `incrPar` and `incrSeq`.

```tut:book
import cats.implicits._
import freestyle.implicits._

val numbers = List.range(1, 10)

val traversingPar = numbers.traverse(incrPar)
val traversingSeq = numbers.traverse(incrSeq)
```

Executing these with our `Id` handler:

```tut:book
traversingPar.exec[Id]
traversingSeq.exec[Id]
```

A simple timer method giving a rough estimate of the execution time of a piece of code.

```tut:book
def simpleTime[A](th: => A): A = {
  val start = System.currentTimeMillis
  val result = th
  val end = System.currentTimeMillis
  println("time: " + (end - start))
  result
}
```

When we execute the increment traversals again using `Future` we can observe the side effect that the parallel execution is quicker than the sequential.

```tut:book
import freestyle.nondeterminism._
import scala.concurrent.Await
import scala.concurrent.duration.Duration

val futPar = traversingPar.exec[Future]
simpleTime(Await.result(futPar, Duration.Inf))

val futSeq = traversingSeq.exec[Future]
simpleTime(Await.result(futSeq, Duration.Inf))
```

## Applicative and Monadic behaviour

We can combine applicative and monadic steps by using the cartesian builder (`|@|`) inside a for comprehension.

```tut:book
val incrAndMultiply =
  for {
    xy <- (incrPar(10) |@| incrPar(11)).tupled
    (x, y) = xy
    z <- Calc[Calc.Op].product(x, y)
    i <- FreeS.pure(1)
  } yield z + i

incrAndMultiply.exec[Id]
```
