---
layout: docs
title: Handlers
permalink: /docs/core/handlers/
---

# Handlers

Freestyle empowers programs whose runtime can easily be overridden via implicit evidence. 

As part of its design, Freestyle is compatible with `Free` and the traditional patterns around it. Apps built with Freestyle give developers the freedom to choose automatic or manual algebras, modules, and interpreters, and intermix them as you see fit in applications based on the desired encoding.

## Implementation

Freestyle automatically generates an abstract definition of an interpreter for each one of the
algebras annotated with `@free`.
This allows you to build the proper runtime definitions for your algebras by simply extending the `Handler[M[_]]`
member in your algebras companion.

Consider the following algebra adapted to Freestyle from the [Typelevel Cats Free monads examples](http://typelevel.org/cats/datatypes/freemonad.html):

```tut:book
import freestyle.free._
import cats.implicits._

@free trait KVStore {
  def put[A](key: String, value: A): FS[Unit]
  def get[A](key: String): FS[Option[A]]
  def delete(key: String): FS[Unit]
  def update[A](key: String, f: A => A): FS.Seq[Unit] =
    get[A](key).freeS flatMap {
      case Some(a) => put[A](key, f(a)).freeS
      case None => ().pure[FS.Seq]
    }
}
```

To define a runtime interpreter for this, we simply extend `KVStore.Handler[M[_]]` and implement its abstract members:

```tut:book
import cats.data.State

type KVStoreState[A] = State[Map[String, Any], A]

implicit val kvStoreHandler: KVStore.Handler[KVStoreState] = new KVStore.Handler[KVStoreState] {
  def put[A](key: String, value: A): KVStoreState[Unit] =
    State.modify(_.updated(key, value))
  def get[A](key: String): KVStoreState[Option[A]] =
    State.inspect(_.get(key).map(_.asInstanceOf[A]))
  def delete(key: String): KVStoreState[Unit] =
    State.modify(_ - key)
}
```

As you may have noticed, instead of implementing a Natural transformation `F ~> M`, we implement methods that closely resemble each one of the smart constructors in our `@free` algebras in Freestyle. This is not an imposition but rather a convenience as the resulting instances are still Natural Transformations.

In the example above, `KVStore.Handler[M[_]]` is already a Natural transformation of type `KVStore.Op ~> KVStoreState` in which its
`apply` function automatically delegates each step to the abstract method that you are implementing as part of the Handler.

Alternatively, if you would rather implement a natural transformation by hand, you can still do that by choosing not to implement
`KVStore.Handler[M[_]]` and providing one like so:

```tut:book
import cats.~>

implicit def manualKvStoreHandler: KVStore.Op ~> KVStoreState = 
  new (KVStore.Op ~> KVStoreState) {
    def apply[A](fa: KVStore.Op[A]): KVStoreState[A] =
      fa match {
        case KVStore.PutOp(key, value) =>
          State.modify(_.updated(key, value))
        case KVStore.GetOp(key) =>
          State.inspect(_.get(key).map(_.asInstanceOf[A]))
        case KVStore.DeleteOp(key) =>
          State.modify(_ - key)
      }
}
```

## Composition

Freestyle performs automatic composition of interpreters by providing the implicit machinery necessary to derive a Module interpreter
by the evidence of it's algebras' interpreters.
To illustrate interpreter composition, let's define a new algebra `Log` which we will compose with our `KVStore` operations:

```tut:book
@free trait Log {
  def info(msg: String): FS[Unit]
  def warn(msg: String): FS[Unit]
}
```

Once our algebra is defined we can easily write an interpreter for it:

```tut:book
import cats.implicits._

implicit def logHandler: Log.Handler[KVStoreState] = 
  new Log.Handler[KVStoreState] {
    def info(msg: String): KVStoreState[Unit] = println(s"INFO: $msg").pure[KVStoreState]
    def warn(msg: String): KVStoreState[Unit] = println(s"WARN: $msg").pure[KVStoreState]
  }
```

Before we create a program combining all operations, let’s consider both `KVStore` and `Log` as part of a module in our application:

```tut:book
@module trait Backend {
  val store: KVStore
  val log: Log
}
```

When `@module` is materialized, it will automatically create the `Coproduct` that matches the interpreters necessary to run the `Free` structure
below:

```tut:book
def program[F[_]](implicit B: Backend[F]): FreeS[F, Option[Int]] = {
  import B.store._, B.log._
  for {
    _ <- put("wild-cats", 2)
    _ <- info("Added wild-cats")
    _ <- update[Int]("wild-cats", (_ + 12))
    _ <- info("Updated wild-cats")
    _ <- put("tame-cats", 5)
    n <- get[Int]("wild-cats")
    _ <- delete("tame-cats")
    _ <- warn("Deleted tame-cats")
  } yield n
}
```

Once we have combined our algebras, we can evaluate them by providing implicit evidence of the Coproduct interpreters. `import freestyle.free.implicits._` brings into scope, among others, the necessary implicit definitions to derive a unified interpreter given implicit evidence of each one of the individual algebra's interpreters:

```tut:book
import freestyle.free.implicits._
program[Backend.Op].interpret[KVStoreState]
```

Alternatively, you can build your interpreters by hand if you choose not to use Freestyle’s implicit machinery. This can quickly grow unruly as the number of algebras increase in an application, but it’s also possible, in the spirit of providing two-way compatibility in all areas between manually built ADTs and Natural Transformations, and the ones automatically derived by Freestyle.

## Tagless Interpretation

Some imports:

```tut:silent
import cats._
import cats.implicits._

import freestyle.tagless._
```

Tagless final algebras are declared using the `@tagless` macro annotation.

```tut:book

@tagless trait Validation {
  def minSize(s: String, n: Int): FS[Boolean]
  def hasNumber(s: String): FS[Boolean]
}

@tagless trait Interaction {
  def tell(msg: String): FS[Unit]
  def ask(prompt: String): FS[String]
}
```

Once your `@tagless` algebras are defined, you can start building programs that rely upon implicit evidence of those algebras
being present, for the target runtime monad you are planning to interpret to.

```tut:book

def taglessProgram[F[_]: Monad](implicit validation : Validation[F], interaction: Interaction[F]) =
  for {
    userInput <- interaction.ask("Give me something with at least 3 chars and a number on it")
    valid     <- (validation.minSize(userInput, 3), validation.hasNumber(userInput)).mapN(_ && _)
    _         <- if (valid)
                    interaction.tell("awesomesauce!") 
                 else
                    interaction.tell(s"$userInput is not valid")
  } yield ()
```

Note that unlike in `@free`, `F[_]` here it refers to the target runtime monad. This is to provide an allocation free model where your
ops are not being reified and then interpreted. This allocation step in Free monads is what allows them to be stack-safe.
The tagless final encoding with direct style syntax is as stack-safe as the target `F[_]` you are interpreting to.

Once our `@tagless` algebras are defined, we can provide `Handler` instances in the same way we do with `@free`.

```tut:book
import scala.util.Try

implicit val validationHandler = new Validation.Handler[Try] {
  override def minSize(s: String, n: Int): Try[Boolean] = Try(s.size >= n)
  override def hasNumber(s: String): Try[Boolean] = Try(s.exists(c => "0123456789".contains(c)))
}

implicit val interactionHandler = new Interaction.Handler[Try] {
  override def tell(s: String): Try[Unit] = Try(println(s))
  override def ask(s: String): Try[String] = Try("This could have been user input 1")
}
```

At this point, we can run our pure programs at the edge of the world.

```tut:book
taglessProgram[Try]
```

## Stack Safety

Freestyle provides two strategies to make `@tagless` encoded algebras stack safe.

### Interpreting to a stack safe monad

The handlers above are not stack safe because `Try` is not stack-safe. Luckily, we can still execute 
our program stack safe with Freestyle by interpreting to `Free[Try, ?]` instead of `Try` directly. 
This small penalty and a few extra allocations will make our programs stack safe.

We can safely invoke our program in a stack safe way, running it to `Free[Try, ?]` first then to `Try` with `Free#runTailRec`:

```tut.book
import cats.free.Free

taglessProgram[Free[Try, ?]].runTailRec
```

### Interpreting combined `@tagless` and `@free` algebras

When combining `@tagless` and `@free` algebras, we need all algebras to be considered in the final Coproduct we are interpreting to.
We can simply use tagless's `.StackSafe` representation in modules so they are considered for the final Coproduct.

```tut:silent
import freestyle.free._
import freestyle.free.implicits._

import freestyle.free.logging._
import freestyle.free.loggingJVM.implicits._
```

```tut:book
def taglessProgram[F[_]]
   (implicit log: LoggingM[F], 
             validation : Validation.StackSafe[F], 
             interaction: Interaction.StackSafe[F]) = {

  import cats.implicits._

  for {
    userInput <- interaction.ask("Give me something with at least 3 chars and a number on it")
    valid     <- (validation.minSize(userInput, 3), validation.hasNumber(userInput)).mapN(_ && _)
    _         <- if (valid)
                    interaction.tell("awesomesauce!") 
                 else
                    interaction.tell(s"$userInput is not valid")
    _         <- log.debug("Program finished")
  } yield ()
}
```

```tut:book
@module trait App {
  val interaction: Interaction.StackSafe
  val validation: Validation.StackSafe
  val log: LoggingM
}
```

Once all of our algebras are considered, we can execute our programs

```tut:book
taglessProgram[App.Op].interpret[Try]
```

Now that we've learned to define our own interpreters, let's jump into [Parallelism](../parallelism/).
