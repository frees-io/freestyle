---
layout: docs
title: Play Framework
permalink: /docs/play/
---

# Play Framework integration

It is easy to use a freestyle program as a result of a Play Framework Action with the _freestyle-http-play_ module. This module provides an implicit conversion `FreeS[F, A] => Future[A]` which allows a user to define a Free program as the result of any Play Action that expects a Future as a response.

In order to enable this integration you may depend on _freestyle-http-play_

```scala
libraryDependencies += "com.47deg" %% "freestyle-http-play" % "0.1.0"
```

The regular imports for working with Freestyle and Cats

```tut:silent
import freestyle._
import freestyle.implicits._
import cats._
import cats.implicits._
```

And some imports for the _freestyle-http-play_ module and Play itself:

```tut:silent
import freestyle.http.play.implicits._

import play.api.mvc._
import play.api.http._
import play.api.libs.concurrent.Execution.Implicits._

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
```

For demonstration purposes we will create here a very simple program that returns an `OK` http status response.

```tut:book
object algebras {
  @free
  trait Noop[F[_]] {
    def ok: FreeS[F, String]
  }
}

object handlers {
  import algebras._

  implicit def noopHandler[M[_]](
      implicit MM: Monad[M]
  ): Noop.Handler[M] = new Noop.Handler[M] {
    def ok: M[String] = MM.pure("success!")
  }
}

import algebras._
import handlers._

def program[F[_]: Noop]: FreeS[F, Result] =
  for {
    msg <- Noop[F].ok
  } yield Results.Ok(msg)
```

Once our program is defined we may simply return it as a result of any `Action` without the need to explicitly interpret it to `Future`

```tut:silent
object AppController extends Controller {
  def endpoint = Action.async { _ => program[Noop.Op] }
}
```
