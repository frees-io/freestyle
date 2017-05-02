---
layout: docs
title: Cats
permalink: /docs/integrations/cats/
---

# Cats

Freestyle is built on top of Cats' [`Free`](http://typelevel.org/cats/datatypes/freemonad.html) and [`FreeApplicative`](http://typelevel.org/cats/datatypes/freeapplicative.html) and `FreeS` and `FreeS.Par` are just type aliases:

```tut:silent
import cats.free.{ Free, FreeApplicative }

object aliases {
  type FreeS[F[_], A] = Free[FreeApplicative[F, ?], A]

  object FreeS {
    type Par[F[_], A] = FreeApplicative[F, A]
  }
}
```

A [freestyle module](../core/modules/) (with `@module`) is an easy way to combine multiple algebras and create a `Coproduct` of the underlying algebras together with the necessary `Inject` instances.

- A `Coproduct` is a combination of data types. An operation of type `type FooBarOp[A] = Coproduct[FooOp, BarOp, A]` can either by a `FooOp` or a `BarOp`.
- `Inject` is a type class which can inject a data type in a `Coproduct` containing that specific data type. An `Inject[FooOp, FooBarOp]` instance can inject a `FooOp` operation into the `FooBarOp` coproduct.

## `FreeS` with Cats

As `FreeS` is a [monad](http://typelevel.org/cats/typeclasses/monad.html) and `FreeS.Par` an [applicative](http://typelevel.org/cats/typeclasses/applicative.html), they can be used like any other monad/applicative in Cats.

To see how we can use `FreeS` and `FreeS.Par` in combination with existing Cats functions, we will create a simple algebra with a sum and a product operation:

```tut:book
import freestyle._

@free trait Calc {
  def sum(a: Int, b: Int): FS[Int]
  def product(a: Int, b: Int): FS[Int]
}
```

A simple `Id` handler:

```tut:book
import cats.Id

implicit val idHandler = new Calc.Handler[Id] {
  def sum(a: Int, b: Int) = a + b
  def product(a: Int, b: Int) = a * b
}
```

A handler translating the `Calc` algebra to `Future`, which introduces a little bit of artificial latency:

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

We have `incrPar` and `incrSeq`, where multiple `incrPar` calls could potentially be executed in parallel:

```tut:book
val incrPar: Int => FreeS.Par[Calc.Op, Int] =
  Calc[Calc.Op].sum(_, 1)

val incrSeq: Int => FreeS[Calc.Op, Int] =
  incrPar.andThen(_.freeS)
```

### Traversing

We will use `traverse` (provided by Cats' [`Traverse`](http://typelevel.org/cats/typeclasses/traverse.html)) to increment a list of integers with `incrPar` and `incrSeq`:

```tut:book
import cats.implicits._
import freestyle.implicits._

val numbers = List.range(1, 10)

val traversingPar = numbers.traverse(incrPar)
val traversingSeq = numbers.traverse(incrSeq)
```

Executing these with our `Id` handler:

```tut:book
traversingPar.interpret[Id]
traversingSeq.interpret[Id]
```

A simple timer method giving a rough estimate of the execution time of a piece of code:

```tut:book
def simpleTime[A](th: => A): A = {
  val start = System.currentTimeMillis
  val result = th
  val end = System.currentTimeMillis
  println("time: " + (end - start))
  result
}
```

When we execute the increment traversals again using `Future`, we can observe that the parallel execution is now quicker than the sequential.

```tut:book
import freestyle.nondeterminism._
import scala.concurrent.Await
import scala.concurrent.duration.Duration

val futPar = traversingPar.interpret[Future]
simpleTime(Await.result(futPar, Duration.Inf))

val futSeq = traversingSeq.interpret[Future]
simpleTime(Await.result(futSeq, Duration.Inf))
```

### Applicative and Monadic behavior

We can combine applicative and monadic steps by using the Cartesian builder (`|@|`) inside a for comprehension:

```tut:book
val incrAndMultiply =
  for {
    ab <- (incrPar(5) |@| incrPar(6)).tupled
    (a, b) = ab
    c <- Calc[Calc.Op].product(a, b)
    d <- FreeS.pure(c + 1)
  } yield d

incrAndMultiply.interpret[Id]
```

In the first line of the for comprehension, the two applicative operations using `incrPar` are independent and could be executed in parallel. In the last line we are using `FreeS.pure` as a handy alternative for `(c + 1).pure[FreeS[Calc.Op, ?]]` or `Applicative[FreeS[CalcOp, ?]].pure(c + 1)`.

To see the effect of replacing multiple independent monadic steps with one applicative step, we can again execute two similar programs using `Future`:

```tut:book
val incrSeqSum = for {
  a <- incrSeq(1)
  b <- incrSeq(2)
  c <- Calc[Calc.Op].sum(a, b)
} yield c

val incrParSum = for {
  ab <- (incrPar(1) |@| incrPar(2)).tupled
  c  <- Function.tupled(Calc[Calc.Op].sum _)(ab)
} yield c

val futSeq2 = incrSeqSum.interpret[Future]
simpleTime(Await.result(futSeq2, Duration.Inf))

val futPar2 = incrParSum.interpret[Future]
simpleTime(Await.result(futPar2, Duration.Inf))
```

## Cats data types

Imagine a scenario where we want to execute our simple calculations by a (remote) service that needs some configuration and our `Calc` algebra has no parameters taking configuration. We don’t want to add this service specific configuration to the `Calc` algebra because it would mean that we would also need to pass the configuration when we want to perform calculations ourselves with our `Id` or `Future` handlers. A possible solution to this dilemma is to use the [`Kleisli`](http://typelevel.org/cats/datatypes/kleisli.html) or `ReaderT` data type from Cats as a target for our `Calc` operations.

`Kleisli` is essentially a function where you can work with the eventual result in a for comprehension, `map` function; while only supplying the input to the `Kleisli` function when you need the final result.

Our demo configuration will only contain a number that we will add to every computation, but in a more realistic case, it could contain an API key, a URI, etc. 

```tut:book
case class Config(n: Int)
```

We will use `Reader[Config, ?]`, which is a type alias for `ReaderT[Id, Config, ?]`, as target here:

```tut:book
import cats.data.Reader
type WithConfig[A] = Reader[Config, A]

implicit val readerHandler =
  new Calc.Handler[WithConfig] {
    def sum(a: Int, b: Int) =
      Reader { cfg => cfg.n + a + b }
    def product(a: Int, b: Int) =
      Reader { cfg => cfg.n + a * b }
  }
```

With this `Reader` handler in place, we can translate some of the previous programs and supply the configuration to get the end result with `Kleisli#run`:

```tut:book
val configTraversing = traversingSeq.interpret[WithConfig]
val configIncrSum    = incrParSum.interpret[WithConfig]

val cfg = Config(1000)
configTraversing.run(cfg)
configIncrSum.run(cfg)
```

The Cats data types can be used as the target of a Freestyle program, but they also cooperate well with `FreeS` and `FreeS.Par`. In the [stack example](../stack/), the `getCustomer` method uses [`OptionT`](http://typelevel.org/cats/datatypes/optiont.html) to combine `FreeS` programs.

## Standing on the shoulders of giant Cats

Freestyle is only possible because of the foundational abstractions provided by Cats. Freestyle shares Cats philosophy to make functional programming in Scala simpler and more approachable to all Scala developers. 

Freestyle tries to reduce the boilerplate needed to create programs using free algebras and provides ready to use effect algebras and integrations with other libraries, making it easier to work and get started with `Free` and consorts.
