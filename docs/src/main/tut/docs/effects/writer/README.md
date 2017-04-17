---
layout: docs
title: Writer
permalink: /docs/effects/writer/
---

## WriterM

The writer effect allows to accumulate values which can be obtained once the program is interpreted. 

The `writer` effect supports parametrization to any type that supports monoidal accumulation while remaining type safe throughout the program declaration. 

The constrains placed by this effect is that there needs to be an implicit evidence of `MonadWriter[M[_], W]` 
for any runtime `M[_]` used in its interpretation. 

The writer effect comes with two operations `writer` and `tell`.

### writer

`writer` sets a tuple with the current accumulator value and returning value

```tut:book
import freestyle._
import freestyle.implicits._
import freestyle.effects.writer
import cats.data.Writer
import cats.implicits._

val wr = writer[List[Int]]

import wr.implicits._

type Logger[A] = Writer[List[Int], A]

def programWriter[F[_]: wr.WriterM] =
  for {
    _ <- 1.pure[FreeS[F, ?]]
    b <- wr.WriterM[F].writer((Nil, 1))
    _ <- 1.pure[FreeS[F, ?]]
  } yield b
  
programWriter[wr.WriterM.Op].exec[Logger].run
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
      
programTell[wr.WriterM.Op].exec[Logger].run
```
