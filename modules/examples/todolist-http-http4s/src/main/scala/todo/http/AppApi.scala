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
import examples.todolist.service.AppService
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl

class AppApi[F[_]: Effect](implicit service: AppService[F]) extends Http4sDsl[F] {

  implicit private val todoFormEncoder: EntityEncoder[F, TodoForm] = jsonEncoderOf[F, TodoForm]
  implicit private val todoFormDecoder: EntityDecoder[F, TodoForm] = jsonOf[F, TodoForm]

  val endpoints = HttpService[F] {
    case POST -> Root / "reset" =>
      service.reset.flatMap(e => Ok(e.asJson))

    case GET -> Root / "list" =>
      service.list.flatMap(l => Ok(l.asJson))

    case req @ POST -> Root / "insert" =>
      for {
        form         <- req.as[TodoForm]
        insertedForm <- service.insert(form)
        response     <- Ok(insertedForm.asJson)
      } yield response

    case req @ PUT -> Root / "update" =>
      for {
        form        <- req.as[TodoForm]
        updatedForm <- service.update(form)
        response    <- Ok(updatedForm.asJson)
      } yield response

    case req @ DELETE -> Root / "delete" =>
      for {
        form     <- req.as[TodoForm]
        deleted  <- service.destroy(form)
        response <- Ok(deleted.asJson)
      } yield response
  }
}

object AppApi {
  implicit def instance[F[_]: Effect](implicit service: AppService[F]): AppApi[F] = new AppApi[F]
}
