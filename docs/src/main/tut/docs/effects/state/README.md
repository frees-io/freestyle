---
layout: docs
title: State
permalink: /docs/effects/state/
---

## StateM

The state effect enables purely functional state throughout programs.

The `state` effect supports parametrization to any type remaining type safe throughout the program declaration. 

There needs to be implicit evidence of `MonadState[M[_], S]` for any runtime `M[_]` where `S` is the type of state due to the constraints placed by this effect.

The state effect comes with four basic operations `get`, `set`, `modify`, and `inspect`.

## get

`get` retrieves the current state:

```tut:book
import freestyle._
import freestyle.implicits._
import freestyle.effects.state
import cats.data.State
import cats.implicits._

val st = state[Int]

type TargetState[A] = State[Int, A]

import st.implicits._

def programGet[F[_]: st.StateM] =
  for {
    a <- 1.pure[FreeS[F, ?]]
    b <- st.StateM[F].get
    c <- 1.pure[FreeS[F, ?]]
  } yield a + b + c
      
programGet[st.StateM.Op].exec[TargetState].run(1).value
```

### set

`set` replaces the current state:

```tut:book
def programSet[F[_]: st.StateM] =
  for {
    _ <- st.StateM[F].set(1)
    a <- st.StateM[F].get
  } yield a

programSet[st.StateM.Op].exec[TargetState].run(0).value
```

### modify

`modify` modifies the current state:

```tut:book
def programModify[F[_]: st.StateM] =
  for {
    a <- st.StateM[F].get
    _ <- st.StateM[F].modify(_ + a)
    b <- st.StateM[F].get
  } yield b

programModify[st.StateM.Op].exec[TargetState].run(1).value
```

### inspect

`inspect` runs a function over the current state and returns the resulting value:

```tut:book
def programInspect[F[_]: st.StateM] =
  for {
    a <- st.StateM[F].get
    b <- st.StateM[F].inspect(_ + a)
  } yield b

programInspect[st.StateM.Op].exec[TargetState].run(1).value
```
