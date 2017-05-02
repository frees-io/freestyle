---
layout: docs
title: Either
permalink: /docs/effects/either/
---

## EitherM

The either effect allows short circuiting of programs and handling invocations which can potentially result in runtime exceptions and that can be translated to a custom left value.
It includes three basic operations `either`, `error`, and `catchNonFatal`.

There needs to be implicit evidence of `MonadError[M[_], E]` 
for `Target` or any other runtime `M[_]` used in interpretation due to the constraints placed by this effect. In the example below, this constraint is satisfied by
`import cats.implicits._` which provides a `MonadError` instance for `Either[E, ?]`.

### either

`either` allows us to lift values of `Either[E, ?]` into the context of `FreeS` raising an error that short circuits the program if the value is `Left(e: E)` or continuing with the computation in the case of a `Right(a)`: 

```tut:book
import freestyle._
import freestyle.implicits._

import freestyle.effects.either

sealed trait BizException
case object Biz1 extends BizException

val e = either[BizException]

import e.implicits._
import cats.implicits._

type Target[A] = Either[BizException, A]

def shortCircuit[F[_]: e.EitherM] =
  for {
    a <- 1.pure[FreeS[F, ?]]
    b <- e.EitherM[F].either[Int](Left(Biz1))
    c <- 1.pure[FreeS[F, ?]]
  } yield a + b + c

shortCircuit[e.EitherM.Op].exec[Target]
```

```tut:book

def continueWithRightValue[F[_]: e.EitherM] =
  for {
    a <- 1.pure[FreeS[F, ?]]
    b <- e.EitherM[F].either[Int](Right(1))
    c <- 1.pure[FreeS[F, ?]]
  } yield a + b + c

continueWithRightValue[e.EitherM.Op].exec[Target]
```

### error

If you simply want to raise an error without throwing an exception, you may use the `error` operation which short circuits the program. 

```tut:book
def shortCircuitWithError[F[_]: e.EitherM] =
  for {
    a <- 1.pure[FreeS[F, ?]]
    b <- e.EitherM[F].error[Int](Biz1)
    c <- 1.pure[FreeS[F, ?]]
  } yield a + b + c

shortCircuitWithError[e.EitherM.Op].exec[Target]
```

### catchNonFatal

`catchNonFatal` allows the capturing of an exception in computations that are not guaranteed to succeed and may potentially throw
a runtime exception when interacting with unprincipled APIs which signal errors as thrown exceptions.
Not all subclass of `java.lang.Throwable` are captured by `catchNonFatal`, as its name implies just those that are considered
in `scala.util.control.NonFatal`.

`catchNonFatal` expects a `cats.Eval` value which holds a lazy computation and a function of `Throwable => E` that transforms the exception into the parametrized `E`:

```tut:book
import cats.Eval

def catchingExceptions[F[_]: e.EitherM] =
  for {
    a <- 1.pure[FreeS[F, ?]]
    b <- e.EitherM[F].catchNonFatal[Int](Eval.later(throw new RuntimeException), _ => Biz1)
    c <- 1.pure[FreeS[F, ?]]
  } yield a + b + c
  
catchingExceptions[e.EitherM.Op].exec[Target]
```
