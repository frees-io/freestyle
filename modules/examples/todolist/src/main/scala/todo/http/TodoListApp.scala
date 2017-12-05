/*
 * Copyright 2017 47 Degrees, LLC. <http://www.47deg.com>
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

package todo
package http

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.{Http, ListeningServer, Service}
import com.twitter.server.TwitterServer
import com.twitter.util.{Await, Future}
import freestyle.free._
import freestyle.free.implicits._
import io.circe.generic.auto._
import io.finch.circe._
import todo.http.apis.{Api, AppApi}
import runtime.implicits._
import cats.implicits._
import cats.~>
import freestyle.free.config.ConfigM
import freestyle.free.effects.error.implicits._
import freestyle.free.config.implicits._
import freestyle.free.loggingJVM.implicits._
import freestyle.http.finch._
import freestyle.free.logging.LoggingM
import todo.persistence.{TagRepository, _}
import todo.services.{AppServices, Services, TagService}

/**
 * Top level module including all algebra dependencies nested in other contains modules.
 * Coproduct of these algebras are calculated recursively so the entire graph is considered
 * for implicit resolution when running .interpret.
 *
 * Freestyle looks up all implicit handlers and attempts to conform a Coproduct of algebras
 * provided individual evidences for each handler exist
 */
@module
trait App {
  val persistence: Persistence
  val services: Services
}

object TodoListApp extends TwitterServer {

  def bootstrap[F[_]](
      implicit app: App[F],
      handler: F ~> Future,
      api: Api[F]): FreeS[F, ListeningServer] = {
    val service: Service[Request, Response] = api.endpoints.toService
    val log                                 = app.services.log
    val cfg                                 = app.services.config
    for {
      _      <- log.warn("Trying to load application.conf")
      config <- cfg.load
      host = config.string("http.host").getOrElse("localhost")
      port = config.int("http.port").getOrElse("8080")
      _ <- log.debug(s"Host: $host")
      _ <- log.debug(s"Port: $port")
    } yield
      Await.ready(
        Http.server.withAdmissionControl
          .concurrencyLimit(maxConcurrentRequests = 10, maxWaiters = 10)
          .serve(s"$host:$port", service))
  }

  def main(): Unit = {

    /**
     * Single point of interpretation of the entire program that delays effects at the edge of the application.
     */
    val server = Await.result(bootstrap[App.Op].interpret[Future])
    onExit {
      server.close()
    }
  }

}
