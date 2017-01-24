---
layout: docs
title: Effects
permalink: /docs/effects/
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

- [error](#error)
- [option](#option)
- [reader](#reader)
- [writer](#writer)
- [state](#state)
- [traverse](#traverse)

## Error

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

shortCircuit[ErrorM.T].exec[Target]
```

```tut:book

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

```tut:book
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

```tut:book
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

```tut:book
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

```tut:book
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

```tut:book
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

The constrains placed by this effect is that there needs to be an implicit evidence of `MonadReader[M[_], R]` 
for any runtime `M[_]` used in its interpretation. `R` represents the seed value type. 

The reader effect comes with two operations `ask` and `reader`.

### ask

`ask` simply returns the entire environment in its current state.

```tut:book
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

### reader

`reader` allows extracting values of the environment and lifting them into the context of `FreeS`

```tut:book
def programReader[F[_]: rd.ReaderM] =
  for {
    a <- 1.pure[FreeS[F, ?]]
    b <- rd.ReaderM[F].reader(_.n)
    c <- 1.pure[FreeS[F, ?]]
  } yield a + b + c
    
programReader[rd.ReaderM.T].exec[ConfigEnv].run(Config(n = 1))
```

## Writer

The writer effect allows to accumulate values which can be obtained once the program is interpreted. 

The `writer` effect supports parametrization to any type that supports monoidal accumulation while remaining type safe throughout the program declaration. 

The constrains placed by this effect is that there needs to be an implicit evidence of `MonadWriter[M[_], W]` 
for any runtime `M[_]` used in its interpretation. 

The writer effect comes with two operations `writer` and `tell`.

### writer

`writer` sets a tuple with the current accumulator value and returning value

```tut:book
import freestyle.effects.writer
import cats.data.Writer

val wr = writer[List[Int]]

import wr.implicits._

type Logger[A] = Writer[List[Int], A]

def programWriter[F[_]: wr.WriterM] =
  for {
    _ <- 1.pure[FreeS[F, ?]]
    b <- wr.WriterM[F].writer((Nil, 1))
    _ <- 1.pure[FreeS[F, ?]]
  } yield b
  
programWriter[wr.WriterM.T].exec[Logger].run
```

### tell

`tell` appends a value for monoidal accumulation

```tut:book
def programTell[F[_]: wr.WriterM] =
  for {
    _ <- 1.pure[FreeS[F, ?]]
    b <- wr.WriterM[F].writer((List(1), 1))
    c <- wr.WriterM[F].tell(List(1))
    _ <- 1.pure[FreeS[F, ?]]
  } yield b
      
programTell[wr.WriterM.T].exec[Logger].run
```

## State

The state effect enables purely functional state throughout programs.

The `state` effect supports parametrization to any type remaining type safe throughout the program declaration. 

The constrains placed by this effect is that there needs to be an implicit evidence of `MonadState[M[_], S]` 
for any runtime `M[_]` where `S` is the type of state.

The state effect comes with four basic operations `get`, `set`, `modify` and `inspect`.

## get

`get` retrieves the current state

```tut:book
import freestyle.effects.state
import cats.data.State

val st = state[Int]

type TargetState[A] = State[Int, A]

import st.implicits._

def programGet[F[_]: st.StateM] =
  for {
    a <- 1.pure[FreeS[F, ?]]
    b <- st.StateM[F].get
    c <- 1.pure[FreeS[F, ?]]
  } yield a + b + c
      
programGet[st.StateM.T].exec[TargetState].run(1).value
```

### set

`set` replaces the current state

```tut:book
def programSet[F[_]: st.StateM] =
  for {
    _ <- st.StateM[F].set(1)
    a <- st.StateM[F].get
  } yield a

programSet[st.StateM.T].exec[TargetState].run(0).value
```

### modify

`modify` modifies the current state

```tut:book
def programModify[F[_]: st.StateM] =
  for {
    a <- st.StateM[F].get
    _ <- st.StateM[F].modify(_ + a)
    b <- st.StateM[F].get
  } yield b

programModify[st.StateM.T].exec[TargetState].run(1).value
```

### inspect

`inspect` runs a function over the current state and returns the resulting value

```tut:book
def programInspect[F[_]: st.StateM] =
  for {
    a <- st.StateM[F].get
    b <- st.StateM[F].inspect(_ + a)
  } yield b

programInspect[st.StateM.T].exec[TargetState].run(1).value
```

## Traverse

Traverse acts as a generator and works with any `G[_]` for which a `cats.Foldable` instance is available.
The target runtime `M[_]` requires a `MonadCombine[M]` instance.

Traverse includes two basic operations `fromTraversable` and `empty`

### fromTraversable

`fromTraversable` allows lifting of any `G[_]: Foldable` into the context of `FreeS`

```tut:book
import freestyle.effects._

val list = traverse[List]
import list._, list.implicits._

def programTraverse[F[_]: TraverseM] =
  for {
    a <- TraverseM[F].fromTraversable(1 :: 2 :: 3 :: Nil)
    b <- (a + 1).pure[FreeS[F, ?]]
  } yield b

programTraverse[TraverseM.T].exec[List]
```

### empty

`empty` allows short circuiting of programs providing the empty value for the `G[_]` through the final `MonadCombine`.
In the same way as `OptionM#none`, the empty value is determined by how the `MonadCombine` instance for the final `M[_]`
is implemented.

```tut:book
def programEmpty[F[_]: TraverseM] =
  for {
    _ <- TraverseM[F].empty[Int]
    a <- TraverseM[F].fromTraversable(1 :: 2 :: 3 :: Nil)
    b <- (a + 1).pure[FreeS[F, ?]]
    c <- (b + 1).pure[FreeS[F, ?]]
  } yield c

programEmpty[TraverseM.T].exec[List]
```
