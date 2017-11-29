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

We will use an example similar to the one used in the [parallelism](/docs/core/parallelism/) section. We will create a small algebra to validate numbers:

```tut:book
@free trait Validator {
  def isPositive: FS[Boolean]
  def isEven: FS[Boolean]
}
```

We can assert that something should be positive and even by combining both operations:

```tut:book
import cats.syntax.apply._

def isPositiveEven[F[_]](implicit V: Validator[F]): FreeS.Par[F, Boolean] =
  (V.isPositive, V.isEven).mapN(_ && _)
```

Our algebra didn't specify exactly what will be validated, so that gives us the liberty to do that in a handler.

We will create a handler that validates integers and can potentially do so asynchronously.

We can encode that we will validate integers by using `Reader[Int, ?]` and the (potential) asynchronicity with Monix `Task`, combining the two we end up with `ReaderT[Task, Int, ?]` or `Kleisli[Task, Int, ?]`.

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

val check = isPositiveEven[Validator.Op].interpret[ValidateInt]
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

We can see that `isPositive` is always printed before `isEven` even though `isEven` doesn't take as long as `isPositive`.

This happens because, in most `Monad` instances, the `Applicative` operations are implemented using `flatMap`, which means that the operations are sequential.

Monix however, allows parallel execution in batches, that does deterministic (ordered) signaling of results with the help of `Task`.
              
The following example uses `Task.gather`, which does parallel processing while preserving result ordering, but in order to ensure that parallel processing actually happens, 
the tasks need to be effectively asynchronous, which for simple functions need to fork threads, hence the usage of `Task.apply`, although remember that you can apply `Task.fork` to any task.

```tut:book
val check2 = isPositiveEven[Validator.Op].interpret[ValidateInt]

val items = 1 :: 2 :: -1 :: Nil

// The list of all tasks needed for execution
val tasks = items.map(check2.run(_))
// Processing in parallel
val aggregate = Task.gather(tasks).map(_.toList)

// Evaluation:
aggregate.foreach(println)
```              
If ordering of results does not matter, you can also use Task.gatherUnordered instead of gather, which might yield better results, given its non-blocking execution.

### Async

There is also the _frees-async-cats-effect_ module mentioned in the [async callback section](/docs/effects/async/), which allows you to work with callback-based APIs in freestyle and translate your `FreeS` program in the end to a type like Monix' `Task`.
