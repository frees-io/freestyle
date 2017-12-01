---
layout: docs
title: Play Framework
permalink: /docs/integrations/play/
---

# Play Framework integration

Freestyle programs are easy to use as a result of a Play Framework Action with the  _freestyle-http-play_ module. This module provides an implicit conversion `FreeS[F, A] => Future[A]` which allows a user to define a Free program as the result of any Play Action that expects a Future as a response.

To enable this integration you can depend on _freestyle-http-play_ with [Play framework 2.6](https://playframework.com/documentation/2.6.x/Migration26):

[comment]: # (Start Replace)

```scala
libraryDependencies += "io.frees" %% "frees-play" % "0.4.6"
```

[comment]: # (End Replace)

The regular imports for working with Freestyle and Cats:

```tut:silent
import freestyle._
import freestyle.implicits._
import cats.implicits._
```

And some imports for the _frees-http-play_ module and Play itself:

```tut:silent
import freestyle.http.play.implicits._

import javax.inject._

import play.api.mvc._
import play.api.http._

import scala.concurrent.ExecutionContext

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
```

For demonstration purposes, we will create a very simple program that returns an `OK` http status response:

```tut:book
import freestyle._

object algebras {
  @free
  trait Noop {
    def ok: FS[String]
  }
}
```

```tut:book
import cats.Monad

object handlers {
  import algebras._

  implicit def noopHandler[M[_]](
      implicit MM: Monad[M]
  ): Noop.Handler[M] = new Noop.Handler[M] {
    def ok: M[String] = MM.pure("success!")
  }
}
```

```tut:book
import algebras._
import handlers._

def program[F[_]: Noop]: FreeS[F, Result] =
  for {
    msg <- Noop[F].ok
  } yield Results.Ok(msg)
```

Once our program is defined we may simply return it as a result of any `Action` without the need to explicitly interpret it to `Future`:

```tut:silent
class AppController @Inject()(val controllerComponents: ControllerComponents)(implicit ec: ExecutionContext) extends BaseController {
  def endpoint = Action.async { _ => program[Noop.Op] }
}
```