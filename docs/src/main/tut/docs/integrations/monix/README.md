---
layout: docs
title: Monix
permalink: /docs/integrations/monix/
---

## Monix

Monix' `Task` can be used as a target of your `FreeS` programs.

```tut:silent
import freestyle._
```

We will take a similar example as in the [parallelism](/docs/core/parallelism/) section. Here we will create a small algebra to validate numbers.

```tut:book
@free trait Validator {
  def isPositive: FS[Boolean]
  def isEven: FS[Boolean]
}
```

We can assert that something should be positive and even by combining both operations:

```tut:book
import cats.syntax.cartesian._

def isPositiveEven[F[_]](implicit V: Validator[F]): FreeS.Par[F, Boolean] =
  (V.isPositive |@| V.isEven).map(_ && _)
```

Our algebra didn't specify what exactly will be validated, so that gives us the liberty to do that in a handler.

We will create a handler that validates integers and that can potentially validate these integers asynchronously.

We could encode the fact that we will validate integers by using `Reader[Int, ?]` and the (potential) asynchronicity with Monix `Task`, combining the two we end up with `ReaderT[Task, Int, ?]` or `Kleisli[Task, Int, ?]`.

```tut:book
import cats.data.Kleisli
import monix.eval.Task

type ValidateInt[A] = Kleisli[Task, Int, A]

def blockAndPrint(millis: Long, msg: String): Unit = { 
  Thread.sleep(Math.abs(millis))
  println(msg)
}

implicit val validateIntTaskHandler: Validator.Handler[ValidateInt] = 
  new Validator.Handler[ValidateInt] {
    def isPositive: ValidateInt[Boolean] =
      Kleisli(i => Task.eval { blockAndPrint(i * 500L, s"isPositive($i)"); i > 0 })

    def isEven: ValidateInt[Boolean] =
      Kleisli(i => Task.eval { blockAndPrint(i * 250L, s"isEven($i)"); i % 2 == 0 })
  }
```

With the Freestyle implicits and the Monix to Cats conversions in scope, we can use our handler to interpret the `isPositiveEven` program:

```tut:book
import freestyle.implicits._
import monix.cats._

val check = isPositiveEven[Validator.Op].exec[ValidateInt]
```

We can pass some integers to the `check` program with the `Kleisli#run` method and run the `Task` as a `Future` using `Task#runAsync`:

```tut:book
import monix.execution.Scheduler.Implicits.global
import scala.concurrent.Await
import scala.concurrent.duration.Duration

Await.result(check.run(1).runAsync, Duration.Inf)
Await.result(check.run(2).runAsync, Duration.Inf)
Await.result(check.run(-1).runAsync, Duration.Inf)
```

We can see that `isPositive` is always printed before `isEven` eventhough `isEven` doesn't take as long as `isPositive`.

This happens because in most `Monad` instances the `Applicative` operations are implemented using `flatMap`, which means that the operations are sequential.

Monix however also has a nondeterministic `Monad` instance, that will execute `Task`s in parallel:

```tut:book
import Task.nondeterminism

val check2 = isPositiveEven[Validator.Op].exec[ValidateInt]

Await.result(check2.run(1).runAsync, Duration.Inf)
Await.result(check2.run(2).runAsync, Duration.Inf)
Await.result(check2.run(-1).runAsync, Duration.Inf)
```

Note that if we lift our `isPostiveEven` program into `FreeS`, it will still execute sequentially. This is an issue in the `Monad` instance of `Kleisli` in the current Cats version, that means that it doesn't use (all) the `Applicative` methods of the nondeterministic instance of `Task`. In the next release of Cats this will execute in parallel as well.

```
val check3 = isPositiveEven[Validator.Op].freeS.exec[ValidateInt]

Await.result(check3.run(1).runAsync, Duration.Inf)
Await.result(check3.run(2).runAsync, Duration.Inf)
Await.result(check3.run(-1).runAsync, Duration.Inf)
```

### Async

There is also the _freestyle-async-monix_ module mentioned in the [async callback section](/docs/effects/async/), which allows you to work with callback based APIs in freestyle and translate your `FreeS` program in the end to a type like Monix' `Task`.
