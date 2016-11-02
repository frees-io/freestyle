# Algebras

Algebraic Data Types are at the core of `Free` based applications.
At the core of Freestyle algebras it's the `@free` macro annotation.
`@free` expands abstract traits and classes deriving Algebraic Data types and all the machinery to compose them from
abstract method definitions.

When you build an algebra with Freestyle you only need to concentrate in the API that you want exposed as smart constructors
at a high level.

A trait or abstract class annotated with `@free` is all you need to create your first algebra with Freestyle.

```tut:silent
import io.freestyle._

case class User(id: Long, name: String)

@free trait UserRepository[F[_]] {
  def get(id: Long): Free[F, User]
  def save(user: User): Free[F, User]
  def list: Free[F, List[User]]
}
```

This is equivalent to

```tut:silent
import io.freestyle._

case class User(id: Long, name: String)

trait UserRepository[F[_]] {
  def get(id: Long): Free[F, User]
  def save(user: User): Free[F, User]
  def getAll(filter: String): Free[F, List[User]]
}

object UserRepository {
  trait UserRepositoryOP[A] extends Product with Serializable
  final case class Get(id: Long) extends UserRepositoryOP[User]
  final case class Save(user: User) extends UserRepositoryOP[User]
  final case class GetAll(filter: String) extends UserRepositoryOP[List[User]]
  
  type T[A] = UserRepositoryOP[A]
  
  class UserRepositoryImpl[F[_]](implicit I: Inject[UserRepositoryOp, F]) {
	def get(id: Long): Free[F, User] = Free.inject[UserRepositoryOp, F](Get(id))
    def save(user: User): Free[F, User] = Free.inject[UserRepositoryOp, F](Save(user))
    def getAll(filter: String): Free[F, List[User]] = Free.inject[UserRepositoryOp, F](GetAll(filter))
  }
  
  implicit def instance[F[_]](implicit I: Inject[UserRepositoryOP, F]): UserRepository[F] =
    new UserRepositoryImpl[F]
  
  def apply[F[_]](implicit ev: UserRepository[F]): UserRepository[F] = ev
}
```

Let's examine the code above to understand what Freestyle is doing for you.

## Automatic method implementations

From the abstract smart constructors Freestyle generates an Algebraic data types available through a companion object.
This Algebraic datatype contains the shape needed to implement the abstract methods.

Freestyle autmatically implements those abstract methods through `Free.inject[UserRepositoryOP, F]` where `F[_]` represents
the final `Coproduct` used to composed different algebras.

## Implicit machinery

As you may have noticed when defining algebras with `@free` there is no need to provide implicit evidences for the necessary
`Inject` typeclasses that otherwise you need to manually provide to further evaluate your free monads when they are interleaved with other `Free` programs.

Beside providing the apropriate `Inject` evidences Freestyle creates an implicit method that will enable implicit summoning of the smart
constructors class implementation and a `apply` methods that allows you to summon instances of your smart constructors class at any point
in the application in a convenient way. This effectively enables implicits based Dependency Injection where you may choose to override implementations
using the implicits scoping rules to place different implementations where appropriate.

```tut:silent
val userRepository = UserRepository[UserRepository.T]
```

```tut:silent
def myService[F[_]](implicit userRepository: UserRepository[F]) = ???
```

## Convenient type aliases

All companions generated with `@free` contain a convenient type alias `T` that you can refer to and that points to the root
ADT node. referring to the Root ADT node it's also possible but discouraged as naming conventions may change in the future.
You can use this to further build `Coproduct` types which will serve in the parameterization of your applications and code.

```tut:silent
@free trait Service1[F[_]]{
	def x(n: Int): Free[F, Int]
}
@free trait Service2[F[_]]{
	def y(n: Int): Free[F, Int]
}
@free trait Service3[F[_]]{
	def z(n: Int): Free[F, Int]
}
type C1[A] = Coproduct[Service1.T, Service2.T, A]
type Module[A] = Coproduct[Service3.T, C1, A]
```

This is obviously far from ideal as building `Coproduct` types by hand often times result in bizarre compile errors
when the types don't align properly arising from placing them in the wrong order when building the `Coproduct`
Fear not. Freestyle provides a [modular system]() to remove all the complexity from building `Coproduct` types by hand also
compose arbitrarly nested Modules containing Algebras where all their Coproduct types are aligned automatically.

[Continue to Modules]()
