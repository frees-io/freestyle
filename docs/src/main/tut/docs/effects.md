---
layout: docs
title: Effects
---

# Effects

Freestyle comes builtin with a list of ready to use effects modeled as `@free` algebras contained in the `freestyle-effects` module.
The current release of `freestyle-effects` supports Scala.jvm and Scala.js.

For Scala.jvm

```scala
libraryDependencies += "io.freestyle" %% "freestyle-effects" % "0.1.0"
```

For Scala.js

```scala
libraryDependencies += "io.freestyle" %%% "freestyle-effects" % "0.1.0"
```

If you are missing an effect from the following list please [raise an issue](https://github.com/47deg/freestyle/issues/new)
so it can be considered in future releases.

- [error]()
- [option]()
- [reader]()
- [writer]()
- [state]()
- [traverse]()

## Error

The error effect allows short circuiting of programs and handling invocations which can potentially result in runtime exceptions.
It includes three basic operations `either`, `error` and `catchNonFatal`.

The constrains placed by this effect is that there needs to be an implicit evidence of `MonadError[M[_], Throwable] 
for `Target` or any other runtime `M[_]` used in interpretation. In the example below this constrain is satisfied by
`import cats.implicits._` which provides a `MonadError` instance for `Either[Throwable, ?]`. 
Multiple types such as `Future`, `monix.eval.Task` and even more complex transformers stacks are capable of satisfying these constrains.

### either

`either` allows us to lift values of `Either[Throwable, ?]` into the context of `FreeS` raising an error short circuiting 
the program if the value is a `Left(throwable)` or continuing with the computation in the case of a `Right(a)` 

```tut:silent:book
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

shortCircuit[ErrorM.T].exec[Target]
```

```tut:silent:book

def continueWithRightValue[F[_]: ErrorM] =
  for {
    a <- 1.pure[FreeS[F, ?]]
    b <- ErrorM[F].either[Int](Right(1))
    c <- 1.pure[FreeS[F, ?]]
  } yield a + b + c

continueWithRightValue[ErrorM.T].exec[Target]
```

### error

If you want so simply raise an error without throwing an exception you may use the `error` operation which short circuits
the program. 

```tut:silent:book
def shortCircuitWithError[F[_]: ErrorM] =
  for {
    a <- 1.pure[FreeS[F, ?]]
    b <- ErrorM[F].error[Int](boom)
    c <- 1.pure[FreeS[F, ?]]
  } yield a + b + c

shortCircuitWithError[ErrorM.T].exec[Target]
```

### catchNonFatal

`catchNonFatal` allows capturing of exception in computations that are not guaranteed to succeed and may potentially throw
a runtime exception when interacting with unprincipled APIs which signal errors as thrown exceptions.
Not all subclass of `java.lang.Throwable` are captured by `catchNonFatal`, as its name implies just those that are considered
in `scala.util.control.NonFatal`.

`catchNonFatal` expects a `cats.Eval` value which holds a lazy computation.

```tut:silent:book
import cats.Eval

def catchingExceptions[F[_]: ErrorM] =
  for {
    a <- 1.pure[FreeS[F, ?]]
    b <- ErrorM[F].catchNonFatal[Int](Eval.later(throw new RuntimeException))
    c <- 1.pure[FreeS[F, ?]]
  } yield a + b + c
  
catchingExceptions[ErrorM.T].exec[Target]
```

## Option

The option effect allows short circuiting of programs for optional values.
It includes two basic operations: `option` and `non`.

The constrains placed by this effect is that there needs to be an implicit evidence of `MonadFilter[M[_]] 
for any runtime `M[_]` used in its interpretation. Short-circuiting with `none` does not mean that you'd end up with a
`None` value at some point. The final value in case of short-circuit is determined by the `MonadFilter[M[_]]#empty` for 
your target runtime `M[_]`.

### option

`option` allows a value of type `Option[_]` to be lifted into the context of `FreeS`. If a `None` it's found the program
will short circuit. 

```tut:silent:book
import freestyle.effects.option._
import freestyle.effects.option.implicits._

def programNone[F[_]: OptionM] =
  for {
    a <- 1.pure[FreeS[F, ?]]
    b <- OptionM[F].option[Int](None)
    c <- 1.pure[FreeS[F, ?]]
  } yield a + b + c
  
programNone[OptionM.T].exec[Option]
```

If a `Some(_)` is found the value is extracted and lifted into the context and the programs resumes
normally.

```tut:silent:book
def programSome[F[_]: OptionM] =
  for {
    a <- 1.pure[FreeS[F, ?]]
    b <- OptionM[F].option(Some(1))
    c <- 1.pure[FreeS[F, ?]]
  } yield a + b + c
  
programSome[OptionM.T].exec[Option]
```

### none

`none` immediately short circuits the program without providing further information as to what the reason is. Handle with
care. 

```tut:silent:book
def programNone2[F[_]: OptionM] =
  for {
    a <- 1.pure[FreeS[F, ?]]
    b <- OptionM[F].none[Int]
    c <- 1.pure[FreeS[F, ?]]
  } yield a + b + c
  
programNone2[OptionM.T].exec[Option]
```

## Reader

The reader effect allows obtaining values from the environment. The initial seed for the environment value is provided
at runtime interpretation.

The `reader` effect supports parametrization to any seed value type while remaining type safe throughout the program declaration. 

The constrains placed by this effect is that there needs to be an implicit evidence of `MonadReader[M[_], R] 
for any runtime `M[_]` used in its interpretation. `R` represents the seed value type. 

The reader effect comes with two operations `ask` and `reader`.

### ask

`ask` simply returns the entire environment in its current state.

```tut:silent:book
import freestyle.effects.reader
import cats.data.Reader

case class Config(n: Int)

type ConfigEnv[A] = Reader[Config, A]

val rd = reader[Config]

import rd.implicits._

def programAsk[F[_]: rd.ReaderM] =
  for {
    _ <- 1.pure[FreeS[F, ?]]
    c <- rd.ReaderM[F].ask
    _ <- 1.pure[FreeS[F, ?]]
  } yield c
    
programAsk[rd.ReaderM.T].exec[ConfigEnv].run(Config(n = 10))
```

`reader` allows extracting values of the environment and lifting them into the context of `FreeS`

```tut:silent:book
def programReader[F[_]: rd.ReaderM] =
  for {
    a <- 1.pure[FreeS[F, ?]]
    b <- rd.ReaderM[F].reader(_.n)
    c <- 1.pure[FreeS[F, ?]]
  } yield a + b + c
    
programReader[rd.ReaderM.T].exec[ConfigEnv].run(Config(n = 1))
```

## Writer

## State

## Traverse