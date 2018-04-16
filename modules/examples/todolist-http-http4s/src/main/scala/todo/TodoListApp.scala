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

import cats.Monad
import cats.effect.IO
import cats.Monad.ops._

import examples.todolist.http.GenericApi

import org.http4s.server.Server
import org.http4s.server.blaze.BlazeBuilder

import org.http4s.HttpService
import org.http4s.implicits._

import freestyle.tagless.config.ConfigM
import freestyle.tagless.config.implicits._

object TodoListApp {

  import examples.todolist.runtime.implicits._

  def bootstrap[F[_]: Monad](
      implicit config: ConfigM[F]
  ): F[Server[F]] = {
    val services: HttpService[F] = GenericApi().service

    for {
      cfg <- config.load
      host: String = cfg.string("http.host").getOrElse("localhost")
      port: Int    = cfg.int("http.port").getOrElse(8080)
    } yield {
      BlazeBuilder[F]
        .bindHttp(port, host)
        .mountService(services)
        .start
    }
  }

  def main() =
    bootstrap[IO].unsafeRunAsync {
      case Left(error)   => println(s"Error executing server. ${error.getMessage}")
      case Right(server) => server.shutdown
    }
}
