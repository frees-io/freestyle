---
layout: docs
title: Algebras
permalink: /docs/core/algebras/
---

# Algebras

Algebraic Data Types are the foundation used to define `Free` based applications and libraries that express their operations as algebras. At the core of Freestyle algebras is the `@free` macro annotation. `@free` expands abstract traits and classes automatically deriving Algebraic Data types and all the machinery needed to compose them from abstract method definitions.

When you build an algebra with Freestyle, you only need to concentrate on the API that you want to be exposed as abstract smart constructors, without worrying how they will be implemented.

In Freestyle, an **algebra** is a trait or abstract class annotated with `@free` or `@tagless`:

```tut:book
import freestyle.free._

case class User(id: Long, name: String)

@free trait Users {
  def get(id: Long): FS[User]
  def save(user: User): FS[User]
  def list: FS[List[User]]
}
```

The `Users` trait declares three smart constructors, named `get`, `save`, and `list`, which generate the basic operations in the algebra. A **smart constructor** is an abstract method declaration with a return type of the form `FS[Ret]`, where `Ret` is the type of the data computed by the operation, and `FS[_]`  marks the method as an operation of the algebra. Intuitively, `FS` means that the method gives a computation within some (generic) context or effect `FS`. 
For example, the `save` smart constructor has a return type `FS[User]`. Intuitively, this declares `save` as a computation in a context (or effect) `FS`, whose result will be a `User` object. 

The `@free` is a Scala [macro annotation](https://www.scala-lang.org/blog/2017/10/09/scalamacros.html), that replaces the annotated trait `Users` by a modified trait, and generates a companion object, as in the code below: 

```tut:silent
import freestyle.free.FreeS
import freestyle.free.internal.EffectLike

case class User(id: Long, name: String)

trait Users[F[_]] extends EffectLike[F] {
  def get(id: Long): FS[User]
  def save(user: User): FS[User]
  def getAll(filter: String): FS[List[User]]
}

object Users {
  import _root_.cats.arrow.FunctionK
  import _root_.freestyle.free.InjK
  import _root_.freestyle.free.FreeS

  sealed trait Op[A] extends Product with Serializable
  final case class GetOp(id: Long) extends Op[User]
  final case class SaveOp(user: User) extends Op[User]
  final case class GetAllOp(filter: String) extends Op[List[User]]

  class To[L[_]](implicit ii: InjK[Op, L]) extends Users[L] {
    private[this] val inj = FreeS.inject[Op, L](ii)

    def get(id: Long): FS[User] = inj( GetOp(id) )
    def save(user: User): FS[User] = inj( SaveOp(user) )
    def getAll(filter: String): FS[List[User]] = inj( GetAllOp(filter) )
  }

  implicit def to[L[_]](implicit I: InjK[Op, L]): Users[L] =
    new To[L]

  def apply[L[_]](implicit c: Users[L]): Users[L] = c

  trait Handler[M[_]] extends FSHandler[Op, M] {
    protected[this] def get(id: Long): M[User]
    protected[this] def save(user: User): M[User]
    protected[this] def getAll(filter: String): M[List[User]]

    override def apply[A](fa: Op[A]): M[A] = fa match {
      case l @ GetOp(_) => get(l.id)
      case l @ SaveOp(_) => save(l.user)
      case l @ GetAllOp(_) => getAll(l.filter)
    }
  }

}
```

Let us examine how the `@free`-annotated trait relates to the code generated from it. 

### Generalise Trait

The `@free` macro makes two changes to the trait `Users`.

1. It adds a type parameter `F[_]`, of kind `* -> *`, which is a general type in which operations are constructed. This makes `Users[F[_]]` resemble a [_Generalised Abstract Data Type (GADT)_](https://pchiusano.github.io/2014-05-20/scala-gadts.html), an interface to construct the algebra's operations into a type `F[_]`.

2. It makes `Users` to extend `EffectLike[F[_]]`. This trait defines the type `FS[_]`, used in the smart constructors, as `FS[_] = FreeS.Par[F]`, which in turn is an alias for `Free[FreeApplicative[F, ?], ?]`.

The `@free` annotation works as a syntactic sugar for writing a GADT in Scala. Without the `EffectLike` trait, the algebra GADT would be written as follows:
```Scala
trait Users[F[_]] {
  def get(id: Long): FreeS.Par[F, User]
  def save(user: User): FreeS.Par[F, User]
  def getAll(filter: String): FreeS.Par[F, List[User]]
}
```

### Algebraic Data Type

From the abstract smart constructors, `@free` generates an algebraic data type (ADT) of operations inside the companion object.
This Algebraic data type contains the shape needed to implement the abstract methods.

```Scala
  sealed trait Op[A] extends Product with Serializable

  final case class GetOp(id: Long) extends Op[User]
  final case class SaveOp(user: User) extends Op[User]
  final case class GetAllOp(filter: String) extends Op[List[User]]
```
Some important features of this ADT are the following ones:
* The root of the ADT is a sealed trait `Op[A]`. Note that this name `Op` is the same in _every_ `@free`-generated algebra.
* For each smart constructor `def foo(x: X, y: Y, ...): FS[Z]`, the `@free` generates a case class `Foo`, whose fields are the parameters of the constructor. For those with a background in object-oriented design, this is similar to the [Command Pattern](http://wiki.c2.com/?CommandPattern).
* The parameter `A` in the root trait `Op[A]` describes the type of the expected result of that operation.

### Injection

If the modified trait `Users[F[_]]` declares an interface for the algebra, the companion object `Users` provides an implementation for that interface.

The `@free` annotation adds into the companion object a class `To[L[_]]`, which implements the `Users` GADT for the target type `L[_]` by using an `InjK[Op, L]` object. An `InjK[F[_], G[_]]` is a special type of tranformation, much like the `FunctionK` in `cats`, that allows to transform a `F[A]` into a `G[A]`. In the case of the `To` class, we need an `InjK[Op, L]`, that allows transforming an object `Req[A]` in the `Op` ADT above, to a value `L[A]` in the target type `L`. The class `To[L[_]]` implements each operation of the algebra by just applying that `InjK` object to the instance of the `Op` ADT.

The `InjK` is based the `Inject` strategy from the [Data types à-la-Carte](http://www.cs.ru.nl/~W.Swierstra/Publications/DataTypesALaCarte.pdf) article, which describes how to compose unrelated ADTs using the Coproduct (or `EitherK` in `cats`).

### Dependency Injection

As you may have noticed when defining algebras with `@free`, there is no need to provide implicit evidence for the necessary `Inject` typeclasses that otherwise need to be manually provided to further evaluate your free monads when they are interleaved with other `Free` programs.

Beside providing the appropriate `Inject` evidences,  Freestyle creates an implicit method that will enable implicit summoning of the smart constructors class implementation and an `apply` method that allows summoning instances of your smart constructors where needed.
This effectively enables implicits based Dependency Injection where you may choose to override implementations using the implicits scoping rules to place different implementations where appropriate.

```tut:book
val users = Users[Users.Op]

def myService[F[_]](implicit users: Users[F]) = ???

def myService2[F[_]: Users] = ???
```

### Composed Operations

The trait `EffectLike[F[_]]` mentioned above, apart from the type alias `FS[_]`, also defines two type aliases `FS.Seq[_]` and `FS.Par[]`.

```Scala
trait EffectLike[F[_]] {
  final type FS[A] = FreeS.Par[F, A]
  final object FS {
    final type Seq[A] = FreeS[F, A]
    final type Par[A] = FreeS.Par[F, A]
  }
}
```

These type aliases can be used to define some `FS` operations that are derived from other operations. , by combining other operations:

* `FS.Par[A]` is an alias for `FreeApplicative[F, A]`. You can declare a derived operation of type `FS.Par` by applying the methods of the `Functor` and `Applicative` type classes.
* `FS.Seq[A]` is an alias for `Free[FreeApplicative[F, ?], A]`, that can be combined using the methods of the `Monad` type class.

```tut:book
import cats.syntax.apply._
import cats.syntax.monad._
@free trait X {
  def a: FS[Int]
  def b(i: Int): FS.Par[Int] = a.map( x => x + i)
  def c: FS.Par[Int] = (a,b(42)).mapN(_ + _)
  def d: FS.Seq[Int] = c.freeS.flatMap(x => b(x).freeS)
}
```

We use the type alias `FS.Seq` and `FS.Par` to indicate complex operations, which combine the algebra's basic requests with the operations from the `Monad` (or `Applicative`) type class. The names follow the intuition that `Applicative` operations combine data-independent computations that can be run in parallel, and `Monad` operations combine data-dependent computations that need to be run in sequence.

Note that, although  `FS[_]` and `FS.Par[_]` are equivalent, `@free` only allows using the former for abstract methods. 

## Convenient type aliases

As described above, `@free` always uses the name `Op` for the sealed trait at the root of the requests ADT. This allows you to access it uniformly, for instance to build a `Coproduct` type that helps you to parametrize you application code. Here is an example for this:

```tut:book
import cats.data.EitherK

@free trait Service1{
  def x(n: Int): FS[Int]
}
@free trait Service2{
  def y(n: Int): FS[Int]
}
@free trait Service3{
  def z(n: Int): FS[Int]
}
type C1[A] = EitherK[Service1.Op, Service2.Op, A]
type Module[A] = EitherK[Service3.Op, C1, A]
```

This is obviously far from ideal, as building `EitherK` types by hand often results in bizarre compile errors when the types don't align properly from being placed in the wrong order.

### Combining `@tagless` and `@free` algebras

Freestyle allows us to compose `@free` and `@tagless` algebras. Let us consider the following `@tagless` algebras:

```tut:book
import freestyle.tagless._

@tagless(true) trait Validation {
  def minSize(s: String, n: Int): FS[Boolean]
  def hasNumber(s: String): FS[Boolean]
}

@tagless(true) trait Interaction {
  def tell(msg: String): FS[Unit]
  def ask(prompt: String): FS[String]
}
```

For every `@tagless` algebra, there is also a free-based representation that is stack-safe by nature, and that can be used
to lift `@tagless` algebras to a context mixing `@free` and `@tagless` algebras.

Let's redefine `program` to support `LoggingM` which is a `@free` defined algebra of logging operations:

```tut:silent
import freestyle.free._
import freestyle.free.implicits._

import freestyle.free.logging._
import freestyle.free.loggingJVM.implicits._
```

```tut:book
def program[F[_]]
   (implicit log: LoggingM[F], 
             validation : Validation.StackSafe[F], 
             interaction: Interaction.StackSafe[F]) = {

  import cats.implicits._

  for {
    userInput <- interaction.ask("Give me something with at least 3 chars and a number on it")
    valid <- (validation.minSize(userInput, 3), validation.hasNumber(userInput)).mapN(_ && _)
    _ <- if (valid)
            interaction.tell("awesomesauce!") 
         else
            interaction.tell(s"$userInput is not valid")
    _ <- log.debug("Program finished")
  } yield ()
}
```

Since `Validation` and `Interaction` were `@tagless` algebras, we need their `StackSafe` representation in order to combine
them with `@free` algebras.

Fear not. Freestyle provides a [modular system](../modules/) to achieve Onion-style architectures
and removes all the complexity from building `EitherK` types by hand and compose arbitrarily nested Modules containing Algebras.

[Continue to Modules](../modules/).
