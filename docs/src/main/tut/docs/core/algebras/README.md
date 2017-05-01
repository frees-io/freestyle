---
layout: docs
title: Algebras
permalink: /docs/core/algebras/
---

# Algebras

Algebraic Data Types are the foundation used to define `Free` based applications and libraries that express their operations as algebras. At the core of Freestyle algebras is the `@free` macro annotation. `@free` expands abstract traits and classes automatically deriving Algebraic Data types and all the machinery needed to compose them from abstract method definitions.

When you build an algebra with Freestyle, you only need to concentrate on the API that you want to be exposed as abstract smart constructors, without worrying how they will be implemented.

A trait or abstract class annotated with `@free` or [`@tagless`](../tagless/) is all you need to create your first algebra with Freestyle:

```tut:book
import freestyle._

case class User(id: Long, name: String)

@free trait UserRepository {
  def get(id: Long): FS[User]
  def save(user: User): FS[User]
  def list: FS[List[User]]
}
```

This is similar to the simplified manual encoding below:

```tut:book
import freestyle.{FreeS, EffectLike}

case class User(id: Long, name: String)

trait UserRepository[F[_]] extends freestyle.EffectLike[F] {
  def get(id: Long): FS[User]
  def save(user: User): FS[User]
  def getAll(filter: String): FS[List[User]]
}

object UserRepository {
  import _root_.cats.arrow.FunctionK
  import _root_.freestyle.InjK
  import _root_.freestyle.FreeS

  sealed trait Op[A] extends Product with Serializable
  final case class Get(id: Long) extends Op[User]
  final case class Save(user: User) extends Op[User]
  final case class GetAll(filter: String) extends Op[List[User]]

  class To[L[_]](implicit ii: InjK[Op, L]) extends UserRepository[L] {

    private[this] val inj = FreeS.inject[Op, L](ii)

    def get(id: Long): FS[User] = inj( Get(id) )

    def save(user: User): FS[User] = inj( Save(user) )

    def getAll(filter: String): FS[List[User]] = inj( GetAll(filter) )
  }

  implicit def to[L[_]](implicit I: InjK[Op, L]): UserRepository[L] =
    new To[L]

  def apply[L[_]](implicit c: UserRepository[L]): UserRepository[L] = c

  trait Handler[M[_]] extends FSHandler[Op, M] {

    protected[this] def get(id: Long): M[User]
    protected[this] def save(user: User): M[User]
    protected[this] def getAll(filter: String): M[List[User]]

    override def apply[A](fa: Op[A]): M[A] = fa match {
      case l @ Get(_) => get(l.id)
      case l @ Save(_) => save(l.user)
      case l @ GetAll(_) => getAll(l.filter)
    }
  }

}
```

Let's examine the two fragments above to understand what Freestyle is doing for you.

## Automatic method implementations

From the abstract smart constructors, Freestyle generates an Algebraic data type available through a companion object.
This Algebraic data type contains the shape needed to implement the abstract methods.

Freestyle automatically implements those abstract methods using the `Inject` strategy for composing unrelated ADTs through a Coproduct as described
in [Data types a la Carte](http://www.cs.ru.nl/~W.Swierstra/Publications/DataTypesALaCarte.pdf) by Wouter Swierstra.

## Dependency Injection

As you may have noticed when defining algebras with `@free`, there is no need to provide implicit evidence for the necessary `Inject` typeclasses that otherwise need to be manually provided to further evaluate your free monads when they are interleaved with other `Free` programs.

Beside providing the appropriate `Inject` evidences,  Freestyle creates an implicit method that will enable implicit summoning of the smart
constructors class implementation and an `apply` method that allows summoning instances of your smart constructors where needed.
This effectively enables implicits based Dependency Injection where you may choose to override implementations
using the implicits scoping rules to place different implementations where appropriate.

```tut:book
val userRepository = UserRepository[UserRepository.Op]
```

```tut:book
def myService[F[_]](implicit userRepository: UserRepository[F]) = ???
```

```tut:book
def myService2[F[_]: UserRepository] = ???
```

## Convenient type aliases

All companions generated with `@free` define a `sealed trait Op[A]` as the root node of the requests ADT.
You may use this to manually build `Coproduct` types which will serve in the parametrization of your application and code as in the example below:

```tut:book
import cats.data.Coproduct

@free trait Service1{
  def x(n: Int): FS[Int]
}
@free trait Service2{
  def y(n: Int): FS[Int]
}
@free trait Service3{
  def z(n: Int): FS[Int]
}
type C1[A] = Coproduct[Service1.Op, Service2.Op, A]
type Module[A] = Coproduct[Service3.Op, C1, A]
```

This is obviously far from ideal, as building `Coproduct` types by hand often results in bizarre compile errors
when the types don't align properly from being placed in the wrong order.

Fear not. Freestyle provides a [modular system](../modules/) to achieve Onion-style architectures
and removes all the complexity from building `Coproduct` types by hand and compose arbitrarily nested Modules containing Algebras.

[Continue to Modules](../modules/).
