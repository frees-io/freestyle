/*
 * Copyright 2017-2018 47 Degrees, LLC. <http://www.47deg.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package examples.todolist
package http

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.{Http, ListeningServer, Service}
import com.twitter.server.TwitterServer
import com.twitter.util.{Await, Future}
import io.circe.generic.auto._
import io.finch.circe._
import cats.effect.IO
import cats.{~>, Monad}
import cats.implicits._
import doobie.util.transactor.Transactor
import freestyle.tagless.module
import freestyle.tagless.logging.LoggingM
import freestyle.tagless.loggingJVM.log4s.implicits._
import freestyle.tagless.config.ConfigM
import freestyle.tagless.config.implicits._
import freestyle.tagless.effects.error.ErrorM
import freestyle.tagless.effects.error.implicits._
import examples.todolist.http.apis.Api
import examples.todolist.persistence.Persistence
import examples.todolist.services.Services

@module
trait App[F[_]] {
  val persistence: Persistence[F]
  val services: Services[F]
}

object TodoListApp extends TwitterServer {

  import examples.todolist.runtime.implicits._

  def bootstrap[F[_]: Monad](
      implicit app: App[F],
      handler: F ~> Future,
      T: Transactor[F],
      api: Api[F]): F[ListeningServer] = {

    val service: Service[Request, Response] = api.endpoints.toService
    val log: LoggingM[F]                    = app.services.log
    val cfg: ConfigM[F]                     = app.services.config

    for {
      _      <- log.warn("Trying to load application.conf")
      config <- cfg.load
      host = config.string("http.host").getOrElse("localhost")
      port = config.int("http.port").getOrElse("8080")
      _ <- log.debug(s"Host: $host")
      _ <- log.debug(s"Port $port")
    } yield
      Await.ready(
        Http.server.withAdmissionControl
          .concurrencyLimit(maxConcurrentRequests = 10, maxWaiters = 10)
          .serve(s"$host:$port", service)
      )
  }

  def main() =
    bootstrap[IO].unsafeRunAsync {
      case Left(error)   => println(s"Error executing server. ${error.getMessage}")
      case Right(server) => server.close()
    }

}
