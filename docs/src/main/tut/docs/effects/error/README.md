---
layout: docs
title: Error
permalink: /docs/effects/error/
---

## ErrorM

The error effect allows short circuiting of programs and handling invocations which can potentially result in runtime exceptions.
It includes three basic operations `either`, `error` and `catchNonFatal`.

The constrains placed by this effect is that there needs to be an implicit evidence of `MonadError[M[_], Throwable]` 
for `Target` or any other runtime `M[_]` used in interpretation. In the example below this constrain is satisfied by
`import cats.implicits._` which provides a `MonadError` instance for `Either[Throwable, ?]`. 
Multiple types such as `Future`, `monix.eval.Task` and even more complex transformers stacks are capable of satisfying these constrains.

### either

`either` allows us to lift values of `Either[Throwable, ?]` into the context of `FreeS` raising an error short circuiting 
the program if the value is a `Left(throwable)` or continuing with the computation in the case of a `Right(a)` 

```tut:book
import freestyle._
import freestyle.implicits._
import freestyle.effects.error._
import freestyle.effects.error.implicits._
import cats.implicits._

val boom = new RuntimeException("BOOM")

type Target[A] = Either[Throwable, A]

def shortCircuit[F[_]: ErrorM] =
  for {
    a <- 1.pure[FreeS[F, ?]]
    b <- ErrorM[F].either[Int](Left(boom))
    c <- 1.pure[FreeS[F, ?]]
  } yield a + b + c

shortCircuit[ErrorM.Op].exec[Target]
```

```tut:book

def continueWithRightValue[F[_]: ErrorM] =
  for {
    a <- 1.pure[FreeS[F, ?]]
    b <- ErrorM[F].either[Int](Right(1))
    c <- 1.pure[FreeS[F, ?]]
  } yield a + b + c

continueWithRightValue[ErrorM.Op].exec[Target]
```

### error

If you want so simply raise an error without throwing an exception you may use the `error` operation which short circuits
the program. 

```tut:book
def shortCircuitWithError[F[_]: ErrorM] =
  for {
    a <- 1.pure[FreeS[F, ?]]
    b <- ErrorM[F].error[Int](boom)
    c <- 1.pure[FreeS[F, ?]]
  } yield a + b + c

shortCircuitWithError[ErrorM.Op].exec[Target]
```

### catchNonFatal

`catchNonFatal` allows capturing of exception in computations that are not guaranteed to succeed and may potentially throw
a runtime exception when interacting with unprincipled APIs which signal errors as thrown exceptions.
Not all subclass of `java.lang.Throwable` are captured by `catchNonFatal`, as its name implies just those that are considered
in `scala.util.control.NonFatal`.

`catchNonFatal` expects a `cats.Eval` value which holds a lazy computation.

```tut:book
import cats.Eval

def catchingExceptions[F[_]: ErrorM] =
  for {
    a <- 1.pure[FreeS[F, ?]]
    b <- ErrorM[F].catchNonFatal[Int](Eval.later(throw new RuntimeException))
    c <- 1.pure[FreeS[F, ?]]
  } yield a + b + c
  
catchingExceptions[ErrorM.Op].exec[Target]
```
