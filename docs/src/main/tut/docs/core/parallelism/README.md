---
layout: docs
title: Parallelism
permalink: /docs/core/parallelism/
---

# Parallelism and non-determinism

Freestyle supports building programs that support both sequential and parallel computations.

# Rationale

One of the biggest issues with architectures based on Free monads is that, by default ,`cats.free.Free` does not support parallelism since your actions as described as a series of sequential monadic steps. Each computation step is built with `Free#flatMap` and evaluated via `Free#foldMap`.
This is not an issue when your operations produce an output value and that value is used as the input in the next step in the monadic sequence, but it's rather hard to express computations that may be performed in parallel.

# Hinting Parallelization

As you may have noticed by now, Freestyle uses a type alias as a return type for operations called `FreeS`.
FreeS is an alias for a Free monad that represents a sequential fragment of potentially multiple parallel steps.

```scala
type FreeS[F[_], A] = Free[FreeApplicative[F, ?], A]
```

We use `FreeS.Par` as an alias for `FreeApplicative` to denote functions that represent a potential parallel step.

Independent operations that can potentially be executed in parallel can be placed inside `@free` algebras as abstract definitions like in the example below:

```tut:book
import freestyle._

@free trait Validation {
  def minSize(n: Int): FS[Boolean]
  def hasNumber: FS[Boolean]
}
```

# Parallel interpretation

Handlers for operations that are executed in parallel should target a type for which there is a monad instance that supports parallelism.
Freestyle ships with ready to use instances for `scala.concurrent.Future` and contains extension modules for [`monix.eval.Task`](https://monix.io/docs/2x/eval/task.html) and [`akka actors`](http://akka.io/). (WIP)

To enable these instances and support parallelism you need to explicitly import:

```tut:book
import freestyle.nondeterminism._
```

The code below illustrates a handler that will allow parallel executions thanks to the unsafe nature of `scala.concurrent.Future#apply` which runs immediately:

```tut:book
import cats.data.Kleisli
import cats.syntax.cartesian._
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

type ParValidator[A] = Kleisli[Future, String, A]

implicit val interpreter = new Validation.Handler[ParValidator] {
  override def minSize(n: Int): ParValidator[Boolean] =
    Kleisli(s => Future(s.size >= n))

  override def hasNumber: ParValidator[Boolean] =
    Kleisli(s => Future(s.exists(c => "0123456789".contains(c))))
}

val validation = Validation[Validation.Op]
import validation._

val parValidation = (minSize(3) |@| hasNumber).map(_ :: _ :: Nil)

val validator = parValidation.interpret[ParValidator]

//validator.run("abc1") runs each op in parallel and returns the result in a Future
```

# Mixing sequential and parallel computations

Sequential and parallel actions can be easily intermixed in `@free` algebras:

```tut:book
@free trait MixedFreeS {
  def x: FS[Int]
  def y: FS[Int]
  def z: FS[Int]
}
```

Using the [cats cartesian builder operator \|@\|](http://eed3si9n.com/herding-cats/Cartesian.html#The+Applicative+Style) we can easily describe steps that run in parallel:

```tut:book
def program[F[_]](implicit M: MixedFreeS[F]) = {
  import M._
  for {
    a <- z //3
    bc <- (x |@| y).tupled.freeS
    (b, c) = bc
    d <- z //3
  } yield List(a,b,c,d)
}
```

Once our operations run in parallel, we can join the results back into the monadic flow lifting it with `.freeS`.
In practice, you may not need to use `.freeS` since Freestyle supports an implicit conversion to lift those automatically so, if the return type is properly inferred you may omit `.freeS` altogether.

`FreeS.Par#freeS` is a function enriched into the `FreeApplicative` syntax that joins the result of both operations back
into a free monad step whose result can be used in further monadic computation.

Now that we've covered how to build modular programs that support both sequential and parallel style computations, let's explore some of the [extra freestyle integrations and effects](../../effects).
