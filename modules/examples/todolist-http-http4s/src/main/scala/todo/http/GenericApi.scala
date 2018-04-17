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

import cats.effect.Effect
import cats.implicits._
import examples.todolist.model.Pong
import freestyle.tagless.logging.LoggingM
import io.circe.Json
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.HttpService

class GenericApi[F[_]: Effect](implicit log: LoggingM[F]) extends Http4sDsl[F] {
  val endpoints =
    HttpService[F] {
      case GET -> Root / "ping" =>
        for {
          _        <- log.error("Not really an error")
          _        <- log.warn("Not really a warn")
          _        <- log.debug("GET /ping")
          response <- Ok(Json.fromLong(Pong.current.time))
        } yield response

      case GET -> Root / "hello" =>
        for {
          _        <- log.error("Not really an error")
          _        <- log.warn("Not really a warn")
          _        <- log.debug("GET /Hello")
          response <- Ok("Hello World")
        } yield response
    }
}

object GenericApi {
  implicit def instance[F[_]: Effect](implicit log: LoggingM[F]): GenericApi[F] =
    new GenericApi[F]
}
