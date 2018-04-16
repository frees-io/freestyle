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

package todo

import cats.effect.{Effect, IO}
import cats.syntax.either._
import cats.syntax.functor._
import cats.syntax.flatMap._
import examples.todolist.http.GenericApi
import org.http4s.server.blaze.BlazeBuilder
import org.http4s.HttpService
import org.http4s.implicits._
import freestyle.tagless.config.ConfigM
import freestyle.tagless.config.implicits._
import freestyle.tagless.logging.LoggingM
import freestyle.tagless.loggingJVM.log4s.implicits._
import fs2.StreamApp

object TodoListApp extends StreamApp[IO] {

  import examples.todolist.runtime.implicits._

  override def stream(
      args: List[String],
      requestShutdown: IO[Unit]): fs2.Stream[IO, StreamApp.ExitCode] =
    bootstrap[IO].unsafeRunSync()

  def bootstrap[F[_]: Effect](
      implicit config: ConfigM[F],
      log: LoggingM[F]): F[fs2.Stream[F, StreamApp.ExitCode]] = {

    val services: HttpService[F] = GenericApi().service

    for {
      _   <- log.warn("Trying to load application.conf")
      cfg <- config.load
      host: String = cfg.string("http.host").getOrElse("localhost")
      port: Int    = cfg.int("http.port").getOrElse(8080)
      _ <- log.debug(s"Host: $host")
      _ <- log.debug(s"Port: $port")
    } yield
      BlazeBuilder[F]
        .bindHttp(port, host)
        .mountService(services)
        .serve
  }
}
