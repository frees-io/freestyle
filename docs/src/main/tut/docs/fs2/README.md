---
layout: docs
title: FS2
permalink: /docs/fs2/
---

# FS2

Interleaving FS2 streams in freestyle programs can be achieved through the algebra and interpreter provided by the `freestyle-fs2` package. `freestyle-fs2` allows you to run streams when interpreting free programs, using the target runtime monad as their effect type.

Familiarity with fs2 is assumed, take a look at [its documentation](https://github.com/functional-streams-for-scala/fs2/blob/series/1.0/docs/guide.md) if you haven't before.

We'll start by creating a simple algebra for our application for printing messages in the screen:

```tut:book
import freestyle._

@free trait Interact[F[_]] {
  def tell(msg: String): FreeS[F, Unit]
}
```

Then, make sure to include the streams algebra `StreamM` in your application:

```tut:book
import freestyle._
import freestyle.implicits._
import freestyle.fs2._
import freestyle.fs2.implicits._

@module trait App[F[_]] {
  val interact: Interact[F]
  val streams: StreamM[F]
}
```

Now that we've got our `Interact` algebra and `StreamM` in our app, we're ready to write a first program:

```tut:book
import _root_.fs2.Stream

def program[F[_]](
    implicit app: App[F]
  ): FreeS[F, Vector[Int]] =  for {
    _ <- app.interact.tell("Hello")
	x <- app.streams.runLog(Stream.emits(List(1, 2, 3)))
	_ <- app.interact.tell(s"Result: ${x}")
  } yield x
```

For running it, we need to create an implicit interpreter for our `Interact` algebra:

```tut:book
import cats._

implicit def interactInterp[F[_]](
  implicit ME: MonadError[F, Throwable]
): Interact.Interpreter[F] = new Interact.Interpreter[F] {
	def tellImpl(msg: String): F[Unit] = {
      println(msg)
	  ME.pure(())
	}
}
```

And now we can run the program to a `Future`, check how the stream's value is printed to the console:

```tut:book
import cats.instances.future._

import scala.concurrent._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

Await.result(
  program[App.T].exec[Future],
  Duration.Inf
)
```


