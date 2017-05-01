---
layout: docs
title: Akka HTTP
permalink: /docs/integrations/akkahttp/
---

### Akka HTTP

[Akka HTTP](http://doc.akka.io/docs/akka-http/10.0.5/java/http/introduction.html) (formerly known as `spray`) is an Akka-based library for implementing HTTP services.

Akka HTTP is frequently used to write server-side (a.k.a back-end) REST APIs; for instance, at 47 Degrees, we have use it to write the back-end
of the [9 Cards](https://github.com/47deg/nine-cards-backend) and [Scala Days](http://scaladays.org/) mobile applications.
Akka HTTP makes it easy to write these types of APIs due to its [routing DSL](http://doc.akka.io/docs/akka-http/10.0.5/java/http/introduction.html).
In this DSL, routes are written using directives that filter incoming requests and end in a `complete` directive.
The `complete` directive can take just a message, or just the status code to give back; but more frequently, it takes an expression, or program call, whose result is the _response body_ that is given back to the client as an argument.

The goal of `freestyle` integration with Akka HTTP is to allow programmers to pass into a `complete` directive an expression of type `fs : FreeS[F, A]`, 
where `F[_]` is the type constructor, and  `A` is the type of the result. In respect to this goal,
the [Akka HTTP docs](http://doc.akka.io/docs/akka-http/10.0.5/java/http/introduction.html#routing-dsl-for-http-servers) say that:

> Transforming request [...] bodies between over-the-wire formats and objects to be used in your application
> is done separately from the route declarations, in marshallers, which are pulled in implicitly using the “magnet” pattern.
> This means that you can complete a request with any kind of object as long as there is an implicit marshaller available in scope.

Thus, the `freestyle` integration for Akka HTTP provides an extension of that _magnet_ pattern, which allows us to generate
response [marshallers](http://doc.akka.io/docs/akka-http/current/scala/http/common/marshalling.html).
To be precise, what the integration gives us is a couple of  `implicit` methods to generate an instance of
 `ToEntityMarshaller[  FreeS[ F, A ] ] `, or `ToEntityMarshaller[ FreeS.Par[F, A]]`, where the parameter `F[_]` is the target of the algebra, `A` is the type of the base result. 
Note that the method has to be parameterized both on the `F` and on `A`, to make it as generic as possible.

```tut:book
import freestyle._
import freestyle.implicits._
import cats.{ ~>, Monad }
import _root_.akka.http.scaladsl.marshalling.ToEntityMarshaller

implicit def seqToEntityMarshaller[F[_], G[_], A](
    implicit NT: F ~> G,
    MonG: Monad[G],
    gem: ToEntityMarshaller[G[A]]): ToEntityMarshaller[FreeS[F, A]] =
  gem.compose((fs: FreeS[F, A]) => fs.exec[G])

implicit def parToEntityMarshaller[F[_], G[_], A](
    implicit NT: F ~> G,
    MonG: Monad[G],
    gem: ToEntityMarshaller[G[A]]): ToEntityMarshaller[FreeS.Par[F, A]] =
  gem.compose((fp: FreeS.Par[F, A]) => FreeS.liftPar(fp).exec[G])
```

To build an object `marsh`, our method needs to find in scope:

* A _base_ entity marshaller, `base: ToEntityMarshaller[ G[A]]`, where `G[_]` is a constructor type, which may be different for each application; 
  and `A` refers to the same type of the result in the response.
* A way to interpret an expression of type `FreeS[F, A]` into a `G[A]`. This is built by the implicit methods in
  `freestyle.implicits`, from a natural transformation `F ~> G` and an instance of `cats.Monad[G]`.

In essence, the generated marshaller first _interprets_ the value of type `FreeS[F,A]` into a `G[A]`, and then it passes the generated
value to the base marshaller. To build an Akka HTTP route using a `FreeS` program, one only needs to bring this method into the implicit context, which can be done using an
`import freestyle.http.akka._` statement.

#### Small Example

As an example, here is a small API program mixing `freestyle` and Akka HTTP. The integration is not part of the `freestyle` main module.
You need to import it separately, by adding the following dependency: 

```scala
libraryDependencies += "io.freestyle" %% "freestyle-http-akka" % "0.1.0"
```

First, we (1) define a domain class `User`, (2) write
a `@free` algebra that returns values of a type `FreeS[F, User]`, and then (3) define an interpreter for that algebra to a type for 
which a `Marshaller` _magnet_ exists. To keep things simple, we just interpret to `Id`:

```tut:book
import freestyle._
import freestyle.implicits._

case class User(name: String)

@free
trait UserApp {
  def get(id: Int): FS[User]
}

val app = UserApp.to[UserApp.Op]
```

To use this `@free` algebra in a route, we need (1) an `EntityMarshaller` for our domain object,
and (2) an interpreter of the algebra to a suitable domain, which for this example will be `Id`:

```tut:book
import _root_.akka.http.scaladsl.marshalling.{ Marshaller, ToEntityMarshaller }
import cats.Id

implicit val userMarshaller: ToEntityMarshaller[User] =
  Marshaller.StringMarshaller.compose((user: User) => s"User(${user.name})")

implicit val handler: UserApp.Handler[Id] = new UserApp.Handler[Id] {
  private[this] val users: Map[Int, User] = Map( 1 -> User("foo") )
    override def get(id: Int): User = users.get(id).getOrElse(User("default"))
}
```

With these in scope, one can now write the route by using the marshaller generators from Akka HTTP:

```tut:book
import _root_.akka.http.scaladsl.server.Directives._
import _root_.akka.http.scaladsl.server.Route
import _root_.akka.http.scaladsl.marshalling.{Marshaller, ToEntityMarshaller}
import _root_.akka.http.scaladsl.server.PathMatchers.IntNumber

val route: Route =  (get & path("user" / IntNumber)) { id =>
  complete(app.get(id))
}
```

More complex routes can be created as usual by chaining and grouping simple routes.
Each route can use a different algebra, or can be interpreted to a different target type `G[_]`.
