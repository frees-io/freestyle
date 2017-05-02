---
layout: docs
title: Traverse
permalink: /docs/effects/traverse/
---

## TraverseM

Traverse acts as a generator and works with any `G[_]` for which a `cats.Foldable` instance is available.
The target runtime `M[_]` requires a `MonadCombine[M]` instance.

Traverse includes two basic operations `fromTraversable` and `empty`.

### fromTraversable

`fromTraversable` allows the lifting of any `G[_]: Foldable` into the context of `FreeS`:

```tut:book
import freestyle._
import freestyle.implicits._
import freestyle.effects._
import cats.implicits._

val list = traverse[List]
import list._, list.implicits._

def programTraverse[F[_]: TraverseM] =
  for {
    a <- TraverseM[F].fromTraversable(1 :: 2 :: 3 :: Nil)
    b <- (a + 1).pure[FreeS[F, ?]]
  } yield b

programTraverse[TraverseM.Op].exec[List]
```

### empty

`empty` allows the short circuiting of programs providing the empty value for the `G[_]` through the final `MonadCombine`.
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

programEmpty[TraverseM.Op].exec[List]
```
