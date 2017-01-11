---
layout: docs
title: Interpreters
---

# Interpreters

As part of its design Freestyle is compatible with Free and traditional patterns around it. Apps build with Freestyle give developers the freedom
to choose Automatic or manual algebras, modules and interpreters and intremix them as you see fit in applications based on the desired encoding.

## Implementation

Freestyle automatically generates an abstract definition of an interpreter for each one of the
algebras annotated with `@free`.
This allow you to build the proper runtime definitions for your algebras by simply extending the `Interpreter[M[_]]`
member in your algebras companion.

Consider the following Algebra adapted to Freestyle from the [Typelevel Cats Free monads examples]()

```tut:silent
import freestyle._
import cats.implicits._

@free trait KVStore[F[_]] {
  def put[A](key: String, value: A): FreeS[F, Unit]
  def get[A](key: String): FreeS[F, Option[A]]
  def delete(key: String): FreeS[F, Unit]
  def update[A](key: String, f: A => A): FreeS[F, Unit] = 
    get[A](key) flatMap {
      case Some(a) => put[A](key, f(a))
      case None => ().pure[FreeS[F, ?]]
    }
}
```

In order to define a runtime interpreter for it we simply extend `KVStore.Interpreter[M[_]]` and implement its
abstract members.

```tut:silent
import cats.data.State

type KVStoreState[A] = State[Map[String, Any], A]

implicit val kvStoreInterpreter: KVStore.Interpreter[KVStoreState] = new KVStore.Interpreter[KVStoreState] {
  def putImpl[A](key: String, value: A): KVStoreState[Unit] =
    State.modify(_.updated(key, value))
  def getImpl[A](key: String): KVStoreState[Option[A]] =
    State.inspect(_.get(key).map(_.asInstanceOf[A]))
  def deleteImpl(key: String): KVStoreState[Unit] =
    State.modify(_ - key)
}
```

As you may have noticed in Freestyle instead of implementing a Natural transformation from your Algebra to a target `M[_]` we
instead implement methods that closely resemble each one of the smart constructors in our @free algebras.
This is not an imposition but rather a comvinience as the resulting instances are still Natural Transformations.

In the example above `KVStore.Interpreter[M[_]]` it's actually already a Natural transformation of type `KVStore.T ~> KVStoreState` in which on its
`apply` function automatically delegates each step to the abstract method that you are implementing as part of the Interpreter.

Alternatively if you would rather implement a natural transformation by hand you can still do that by choosing not to implement
`KVStore.Interpreter[M[_]]` and providing one like so:

```tut:silent
import cats.~>

implicit def manualKvStoreInterpreter: KVStore.T ~> KVStoreState = 
  new (KVStore.T ~> KVStoreState) {
    def apply[A](fa: KVStore.T[A]): KVStoreState[A] =
      fa match {
        case KVStore.PutOP(key, value) =>
          State.modify(_.updated(key, value))
        case KVStore.GetOP(key) =>
          State.inspect(_.get(key).map(_.asInstanceOf[A]))
        case KVStore.DeleteOP(key) =>
          State.modify(_ - key)
      }
}
```

## Composition

Freestyle performs automatic composition of interpreters by providing the implicit machinery necessary to derive a Module interpreter
by the evidence of it's algebras's interpreters.
To ilustrate interpreter composition let's define a new algebra `Log` which we will compose with our `KVStore` operations

```tut:silent
@free trait Log[F[_]] {
  def info(msg: String): FreeS[F, Unit]
  def warn(msg: String): FreeS[F, Unit]
}
```

Once our algebra is defined we can easily write an interpreter for it

```tut:silent
import cats.implicits._

implicit def logInterpreter: Log.Interpreter[KVStoreState] = 
  new Log.Interpreter[KVStoreState] {
    def infoImpl(msg: String): KVStoreState[Unit] = println("INFO: $msg").pure[KVStoreState]
    def warnImpl(msg: String): KVStoreState[Unit] = println("WARN: $msg").pure[KVStoreState]
  }
```

Before we create a program where we combine all operations let's consider both `KVStore` and `Log` as part
of a module in our application

```tut:silent
@module trait Backend[F[_]] {
  val store: KVStore[F]
  val log: Log[F]
}
```

When `@module` is materialized it will automatically create the Coproduct that matches interpreters necessary to run the `Free` structure
below.

```tut:silent
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

Once we have combined our algebras we can simply evaluate them by providing implicit evidence of the Coproduct interpreters.
`import freestyle.implicits._` brings into scope among others the necessary implicit definitions to derive a unified interpreter given
implicit evidences of each one of the individual algebra's interpreters.

```tut:silent
import freestyle.implicits._
program[Backend.T].exec[KVStoreState]
```

Alternatively you can build your interpreters by hand if you wish not to use Freestyle implicit machinery.
This may quickly grow unwildly as the number of algebras increase in an application but it's also possible in the spirit of providing two way compatibility
in all areas between manually built ADTs and Natural Transformations and the ones automatically derived by Freestyle.

Now that we've learnt to define our own interpreters let's jump into [Parallelism](parallelism.html) 
