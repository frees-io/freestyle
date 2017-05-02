---
layout: docs
title: Async Callbacks
permalink: /docs/effects/async/
---

# Async Callbacks

The _freestyle-async_ module makes it possible to use asynchronous callback-based APIs with freestyle.

## Example

Let’s imagine an API that allows us to find books written by a certain author. We need to supply a callback function handling both a successful case where the books have been found, and a case where something went wrong. 

```tut:book
case class Author(name: String) extends AnyVal
case class Book(title: String, year: Int)

trait LibraryAPI {
  def findBooks(author: Author)(withBooks: List[Book] => Unit, error: Throwable => Unit): Unit
}
```

We can use this type of callback-based (asynchronous) API in freestyle by using the _freestyle-async_ module.

To use the _freestyle-async_ module, you need to include one of the following dependencies:

[comment]: # (Start Replace)

```scala
libraryDependencies += "com.47deg" %% "freestyle-async" % "0.1.0"

// and if you want to use Monix' Task:
libraryDependencies += "com.47deg" %% "freestyle-async-monix" % "0.1.0"

// and if you want to use FS2's Task:
libraryDependencies += "com.47deg" %% "freestyle-async-fs2" % "0.1.0"
```

[comment]: # (End Replace)

The standard freestyle imports:

```tut:silent
import freestyle._
import freestyle.implicits._
```

The imports for the _freestyle-async_ module:

```tut:silent
import freestyle.async._
import freestyle.async.implicits._
```

Now if we want to create a freestyle program which uses the `findBooks` function and returns all the books sorted by the year they were written, we can use the `AsyncM` effect:

```tut:book
def sortedBooks[F[_]: AsyncM](lib: LibraryAPI)(author: Author): FreeS[F, List[Book]] =
  AsyncM[F].async[List[Book]] { cb =>
    lib.findBooks(author)({ books => cb(Right(books)) }, { error => cb(Left(error)) })
  }
```

A simple demo implementation for the `LibararyAPI`:

```tut:book
case class NoBooksFound(author: Author) extends Exception(s"No books found from ${author.name}")

object Library extends LibraryAPI {
  def findBooks(author: Author)(withBooks: List[Book] => Unit, error: Throwable => Unit): Unit =
    if (author.name.toLowerCase == "dickens") error(NoBooksFound(author))
    else withBooks(Book("The Old Man and the Sea", 1951) :: Book("1984", 1948) :: Book("On the Road", 1957) :: Nil)
}
```

Now we can create simple programs requesting the books for certain authors:

```tut:book
val getSorted: Author => FreeS[AsyncM.Op, List[Book]] =
  sortedBooks[AsyncM.Op](Library) _

val dickensBooks = getSorted(Author("Dickens"))
val otherBooks   = getSorted(Author("HemingwayOrwellKerouac"))
```

We can run these programs using:

- FS2's `Task` as target (we need to have an `fs2.Strategy` in implicit scope):

```tut:book
import freestyle.asyncFs2.implicits._

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration.Duration

import _root_.fs2.{Strategy, Task => Fs2Task}
import _root_.fs2.interop.cats._

implicit val strategy = Strategy.fromExecutionContext(ExecutionContext.Implicits.global)

val fs2Task1 = dickensBooks.interpret[Fs2Task]
val fs2Task2 = otherBooks.interpret[Fs2Task]
```

```tut:book:fail
Await.result(fs2Task1.unsafeRunAsyncFuture, Duration.Inf)
```

```tut:book
Await.result(fs2Task2.unsafeRunAsyncFuture, Duration.Inf)
```

- Monix' `Task` as target (we need to have an `ExecutionContext` in implicit scope):

```tut:book
import freestyle.asyncMonix.implicits._

import monix.eval.{Task => MonixTask}
import monix.cats._
import monix.execution.Scheduler

implicit val executionContext = Scheduler.Implicits.global

val monixTask1 = dickensBooks.interpret[MonixTask]
val monixTask2 = otherBooks.interpret[MonixTask]
```

```tut:book:fail
Await.result(monixTask1.runAsync, Duration.Inf)
```

```tut:book
Await.result(monixTask2.runAsync, Duration.Inf)
```

- `Future` as target (we need to have an `ExecutionContext` in implicit scope):

```tut:book
import scala.concurrent.Future
// import scala.concurrent.ExecutionContext.Implicits.global

import cats.implicits._

val fut1 = dickensBooks.interpret[Future]
val fut2 = otherBooks.interpret[Future]
```

```tut:fail:book
Await.result(fut1, Duration.Inf)
```

```tut:book
Await.result(fut2, Duration.Inf)
```