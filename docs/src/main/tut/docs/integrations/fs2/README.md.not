---
layout: docs
title: FS2
permalink: /docs/integrations/fs2/
---

# FS2

Interleaving FS2 streams in Freestyle programs can be achieved through the algebra and interpreter provided by the `freestyle-fs2` package. `freestyle-fs2` allows you to run streams when interpreting free programs, using the target runtime monad as their effect type.

Familiarity with fs2 is assumed, take a look at the [fs2 documentation](https://github.com/functional-streams-for-scala/fs2/blob/series/1.0/docs/guide.md) if you haven't before.

We'll start by creating a simple algebra for our application for printing messages on the screen:

```tut:book
import freestyle._

@free trait Interact {
  def tell(msg: String): FS[Unit]
}
```

Then, make sure to include the streams algebra `StreamM` in your application:

```tut:book
import freestyle._
import freestyle.implicits._
import freestyle.fs2._
import freestyle.fs2.implicits._

@module trait App {
  val interact: Interact
  val streams: StreamM
}
```

Now that we've got our `Interact` algebra and `StreamM` in our app, we're ready to write the first program:

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

To run it, we need to create an implicit interpreter for our `Interact` algebra:

```tut:book
import cats._

implicit def interactInterp[F[_]](
  implicit ME: MonadError[F, Throwable]
): Interact.Handler[F] = new Interact.Handler[F] {
  def tell(msg: String): F[Unit] = {
    println(msg)
    ME.pure(())
  }
}
```

And now we can run the program to a `Future`. Check how the stream's value is printed to the console:

```tut:book
import cats.instances.future._

import scala.concurrent._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

Await.result(program[App.Op].interpret[Future], Duration.Inf)
```

## Stream operations

A handful of operations for running streams are exposed in the `StreamM` algebra.

### runLog

We've already seen `StreamM#runLog`, that runs a stream accumulating its result in a `Vector`. In the following example, we use it to pass a Stream that emits the number 42 and ends:

```tut:book
def program[F[_]](
  implicit app: App[F]
): FreeS[F, Vector[Int]] = app.streams.runLog(Stream.emit(42))

Await.result(program[App.Op].interpret[Future], Duration.Inf)
```

### runFold

We can run a fold over a stream too; let's create a stream with a series of numbers. We use a type provided by `freestyle-fs2` as the effect type for the stream (`Eff`), this allows the final stream effect type to be decided when running the program:

```tut:book
val aStream: Stream[Eff, Int] = Stream.emits(0 until 10)
```

Now we can fold over the above stream by adding all of its numbers:

```tut:book
def program[F[_]](
  implicit app: App[F]
): FreeS[F, Int] = app.streams.runFold(0, (x: Int, y: Int) => x + y)(aStream)

Await.result(program[App.Op].interpret[Future], Duration.Inf)
```

### runLast

Another option is to run a stream discarding all the results but the last one. Since the stream can be empty, the result of `runLast` is an `Option`:

```tut:book
def program[F[_]](
  implicit app: App[F]
): FreeS[F, Option[Int]] = app.streams.runLast(aStream)

Await.result(program[App.Op].interpret[Future], Duration.Inf)
```

## Streaming IO

The fs2 library comes with support for streaming IO through its `fs2-io` package, and it's straightforward to integrate in freestyle programs.

Here is an example borrowed from the `fs2` README of a fahrenheit to celsius converter process. In order for an IO stream to be able to run in a free program, we need to use `Eff` as the effect type for the stream:

```tut:book
import _root_.fs2.{io, text}
import java.nio.file.Paths

def fahrenheitToCelsius(f: Double): Double =
  (f - 32.0) * (5.0/9.0)

val converter: Stream[Eff, Unit] = {
  io.file.readAll[Eff](Paths.get("testdata/fahrenheit.txt"), 4096)
    .through(text.utf8Decode)
    .through(text.lines)
    .filter(s => !s.trim.isEmpty && !s.startsWith("//"))
    .map(line => fahrenheitToCelsius(line.toDouble).toString)
    .intersperse("\n")
    .through(text.utf8Encode)
    .through(io.file.writeAll(Paths.get("testdata/celsius.txt")))
}
```

We can now interleave this stream inside our free programs, choosing the streams' effect type when running the whole program:

```tut:book
def program[F[_]](
  implicit app: App[F]
): FreeS[F, Unit] = for {
 _ <- app.interact.tell("Converting from farenheit to celsius")
 _ <- app.streams.run(converter)
} yield ()

Await.result(program[App.Op].interpret[Future], Duration.Inf)
```

