---
layout: docs
title: Reader
permalink: /docs/effects/reader/
---

## ReaderM

The reader effect allows obtaining values from the environment. The initial seed for the environment value is provided
at runtime interpretation.

The `reader` effect supports parametrization to any seed value type while remaining type safe throughout the program declaration. 

There needs to be implicit evidence of `MonadReader[M[_], R]` 
for any runtime `M[_]` used in its interpretation due to the constraints placed by this effect. `R` represents the seed value type. 

The reader effect comes with two operations `ask` and `reader`.

### ask

`ask` simply returns the entire environment in its current state.

```tut:book
import freestyle._
import freestyle.implicits._
import freestyle.effects.reader
import cats.data.Reader
import cats.implicits._

case class Config(n: Int)

type ConfigEnv[A] = Reader[Config, A]

val rd = reader[Config]

import rd.implicits._

def programAsk[F[_]: rd.ReaderM] =
  for {
    _ <- FreeS.pure(1)
    c <- rd.ReaderM[F].ask
    _ <- FreeS.pure(1)
  } yield c
    
programAsk[rd.ReaderM.Op].interpret[ConfigEnv].run(Config(n = 10))
```

### reader

`reader` allows extracting values of the environment and lifting them into the context of `FreeS`

```tut:book
def programReader[F[_]: rd.ReaderM] =
  for {
    a <- FreeS.pure(1)
    b <- rd.ReaderM[F].reader(_.n)
    c <- FreeS.pure(1)
  } yield a + b + c
    
programReader[rd.ReaderM.Op].interpret[ConfigEnv].run(Config(n = 1))
```
