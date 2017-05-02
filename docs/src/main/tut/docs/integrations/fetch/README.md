---
layout: docs
title: Fetch
permalink: /docs/integrations/fetch/
---

# Fetch

Running data fetches optimized by the [Fetch](https://github.com/47deg/fetch) library in Freestyle programs can be achieved through the algebra and interpreter that are provided by the `freestyle-fetch` package. `freestyle-fetch` allows you to run Fetch computations when interpreting a free program, using the target runtime monad as their runtime type.

Familiarity with fetch is assumed, take a look at the [Fetch documentation](http://47deg.github.io/fetch/) if you haven't before. We'll use a trivial data source for the examples:

```tut:book
import _root_.fetch._
import _root_.fetch.implicits._

import cats.data.NonEmptyList

object OneSource extends DataSource[Int, Int]{
 def name = "One"
 def fetchOne(id: Int): Query[Option[Int]] = Query.sync({
    println(s"Fetching ${id}")
    Option(1)
 })
 def fetchMany(ids: NonEmptyList[Int]): Query[Map[Int, Int]] = batchingNotSupported(ids)
}

def fetchOne(x: Int): Fetch[Int] = Fetch(x)(OneSource)
```

Let's start by creating a simple algebra for our application for printing messages on the screen:

```tut:book
import freestyle._
import freestyle.implicits._

@free trait Interact {
  def tell(msg: String): FS[Unit]
}
```

Then, make sure to include the Fetch algebra `FetchM` in your application:

```tut:book
import freestyle.fetch._
import freestyle.fetch.implicits._

@module trait App {
  val interact: Interact
  val fetches: FetchM
}
```

Now that we've got our `Interact` algebra and `FetchM` in our app, we're ready to write the first program:

```tut:book
def program[F[_]](
  implicit app: App[F]
): FreeS[F, Int] =  for {
    _ <- app.interact.tell("Hello")
    x <- app.fetches.runA(fetchOne(1))
    _ <- app.interact.tell(s"Result: ${x}")
  } yield x
```

To run this, we need to create an implicit interpreter for our `Interact` algebra:

```tut:book
import cats.Monad

implicit def interactInterp[F[_]](
  implicit ME: Monad[F]
): Interact.Handler[F] = new Interact.Handler[F] {
  def tell(msg: String): F[Unit] = {
    println(msg)
    ME.pure(())
  }
}
```

Now we can run the program to a `Future`. Check how the result from the fetch is printed to the console:

```tut:book
import scala.concurrent._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

Await.result(program[App.Op].interpret[Future], Duration.Inf)
```

## Running fetches

A handful of operations for running fetches are exposed in the `FetchM` algebra.

### runA

We've already seen `FetchM#runA`, which runs a fetch that returns the final result:

```tut:book
def program[F[_]](
  implicit app: App[F]
): FreeS[F, Int] =  for {
    _ <- app.interact.tell("Hello")
    x <- app.fetches.runA(fetchOne(1))
    _ <- app.interact.tell(s"Result: ${x}")
  } yield x

Await.result(program[App.Op].interpret[Future], Duration.Inf)
```

There is a variant of `runA` where a cache can be specified: `FetchM#runAWithCache`. In the following example, we simply pass an empty in-memory cache, but you can pass your own cache or a resulting cache from a previous fetch:

```tut:book
def program[F[_]](
  implicit app: App[F]
): FreeS[F, Int] =  for {
    _ <- app.interact.tell("Hello")
    x <- app.fetches.runAWithCache(fetchOne(1), InMemoryCache.empty)
    _ <- app.interact.tell(s"Result: ${x}")
  } yield x

Await.result(program[App.Op].interpret[Future], Duration.Inf)
```

### runE

Fetch tracks its internal state with an environment of type `FetchEnv`. The `FetchM#runE` allows us to run a fetch and get its final environment out, ignoring its result:

```tut:book
def program[F[_]](
  implicit app: App[F]
): FreeS[F, FetchEnv] =  for {
    _ <- app.interact.tell("Hello")
    env <- app.fetches.runE(fetchOne(1))
    _ <- app.interact.tell(s"Result: ${env}")
  } yield env

Await.result(program[App.Op].interpret[Future], Duration.Inf)
```

There is a variant of `runE` where a cache can be specified: `FetchM#runEWithCache`. In the following example, we simply pass an empty in-memory cache, but you can pass your own cache or a resulting cache from a previous fetch.

```tut:book
def program[F[_]](
  implicit app: App[F]
): FreeS[F, FetchEnv] =  for {
    _ <- app.interact.tell("Hello")
    env <- app.fetches.runEWithCache(fetchOne(1), InMemoryCache.empty)
    _ <- app.interact.tell(s"Result: ${env}")
  } yield env

Await.result(program[App.Op].interpret[Future], Duration.Inf)
```

### runF

If we're interested in both the final environment and the fetch result, we can use `FetchM#runF` to get a `(FetchEnv, A)` pair.

```tut:book
def program[F[_]](
  implicit app: App[F]
): FreeS[F, FetchEnv] =  for {
    _ <- app.interact.tell("Hello")
    r <- app.fetches.runF(fetchOne(1))
    (env, x) = r
    _ <- app.interact.tell(s"Result: ${env}")
  } yield env

Await.result(program[App.Op].interpret[Future], Duration.Inf)
```

There is a variant of `runF` where a cache can be specified: `FetchM#runFWithCache`. In the following example, we simply pass an empty in-memory cache, but you can pass your own cache or a resulting cache from a previous fetch:

```tut:book
def program[F[_]](
  implicit app: App[F]
): FreeS[F, FetchEnv] =  for {
    _ <- app.interact.tell("Hello")
	r <- app.fetches.runFWithCache(fetchOne(1), InMemoryCache.empty)
	(env, x) = r
	_ <- app.interact.tell(s"Result: ${env}")
  } yield env

Await.result(program[App.Op].interpret[Future], Duration.Inf)
```

## Tips

### Reuse cache between fetches

One of the things to consider when interleaving fetches in free programs is that the default in-memory cache won't be shared between fetch executions on different steps of the program. Notice that the message from `OneSource` that says `"Fetching 1"` is printed twice:

```tut:book
def program[F[_]](
  implicit app: App[F]
): FreeS[F, Int] =  for {
    _ <- app.interact.tell("Hello")
	x <- app.fetches.runA(fetchOne(1))
	_ <- app.interact.tell(s"Result: ${x}")
	y <- app.fetches.runA(fetchOne(1))
  } yield x + y

Await.result(program[App.Op].interpret[Future], Duration.Inf)
```

However, we can run a fetch getting both the environment and the result with `runF`, and pass the resulting cache to subsequent fetch runs. Note how the `"Fetching 1"` message is only printed once:

```tut:book
def program[F[_]](
  implicit app: App[F]
): FreeS[F, Int] =  for {
    _ <- app.interact.tell("Hello")
    r <- app.fetches.runF(fetchOne(1))
	(env, x) = r
	_ <- app.interact.tell(s"Result: ${x}")
	y <- app.fetches.runAWithCache(fetchOne(1), env.cache)
  } yield x + y

Await.result(program[App.Op].interpret[Future], Duration.Inf)
```
