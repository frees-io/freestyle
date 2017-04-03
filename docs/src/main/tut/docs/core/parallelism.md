---
layout: docs
title: Parallelism
permalink: /docs/core/parallelism/
---

# Parallelism and non-determinism

Freestyle supports building programs that support both sequential and parallel computations.

# Rationale

One of the biggest issue with architectures based on Free monads is that by default `cats.free.Free` does not support parallelism since your actions as described as a series of sequential monadic steps. Each computation step is built with `Free#flatMap` and evaluated via `Free#foldMap`.
This is not an issue when your operations produce and output value and that value is used as input in the next step in the monadic sequence but it's rather hard to express computations that may be performed in parallel.

# Hinting Parallelization

As you may have noticed by now Freestyle uses a type alias as return type of your operations called `FreeS`.
FreeS is an alias for a Free monad that represents a sequential fragment of potentially multiple parallel steps.

```scala
type FreeS[F[_], A] = Free[FreeApplicative[F, ?], A]
```

We use `FreeS.Par` as an alias for `FreeAplicative` to denote functions that represents a potential parallel step.

Independent operations that can be executed potentially in parallel may be placed inside `@free` algebras as abstract definitions like in the example below.

```tut:book
import freestyle._

@free trait Validation[F[_]] {
  def minSize(n: Int): FreeS.Par[F, Boolean]
  def hasNumber: FreeS.Par[F, Boolean]
}
```

# Parallel interpretation

Handlers for operations that are executed in parallel should target a type for which there is a monad instance that supports parallelism.
Freestyle ships with ready to use instances for `scala.concurrent.Future` and contains extension modules for [`monix.eval.Task`](https://monix.io/docs/2x/eval/task.html) and [`akka actors`](http://akka.io/) (WIP)

To enable these instances and support parallelism you need to explicitly import:

```tut:book
import freestyle.nondeterminism._
```

The code below illustrate a handler that will allow parallel executions thanks to the unsafe nature of `scala.concurrent.Future#apply` which runs immediately.

```tut:book
import cats.data.Kleisli
import cats.implicits._
import scala.concurrent._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

import freestyle.implicits._

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

val validator = parValidation.exec[ParValidator]

//validator.run("abc1") runs each op in parallel and returns the result in a Future
```

# Mixing sequential and parallel computations

Sequential and parallel actions can be easily intermixed in `@free` algebras.

```tut:book
@free trait MixedFreeS[F[_]] {
  def x: FreeS.Par[F, Int]
  def y: FreeS.Par[F, Int]
  def z: FreeS[F, Int]
}
```

Using the [cats cartesian builder operator \|@\|](http://eed3si9n.com/herding-cats/Cartesian.html#The+Applicative+Style) we can easily describe steps that run in parallel

```tut:book
import freestyle.implicits._
import cats.implicits._

def program[F[_]](implicit M: MixedFreeS[F]) = {
  import M._
  for {
    a <- z //3
    bc <- (x |@| y).tupled.freeS //(1,2) potentially x and y run in parallel
	(b, c) = bc
	d <- z //3
  } yield a :: b :: c :: d :: Nil // List(3,1,2,3)
}
```

Once our operations run in parallel we can join the results back into the monadic flow lifting it with `.freeS`.
In practice you may not need to use `.freeS` since Freestyle supports an implicit conversion to lift those automatically so if the return type is properly infered you may omit `.freeS` altogether.

`FreeS.Par#freeS` is a function enriched into the `FreeApplicative` syntax that joins the result of both operations back
into a free monad step whose result can be used in further monadic computation.

Now that we've covered how to build modular programs that support both sequential and parallel style computations let's explore some of the [extra freestyle integrations and effects](/docs/effects)
