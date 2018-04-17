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
import examples.todolist.service.TodoItemService
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl

class TodoItemApi[F[_]: Effect](implicit service: TodoItemService[F]) extends Http4sDsl[F] {

  import codecs._

  private val prefix = "items"

  val endpoints = HttpService[F] {
    case POST -> Root / prefix =>
      service.reset.flatMap(_ => Ok())

    case GET -> Root / prefix / IntVar(id) =>
      service.retrieve(id) flatMap { item =>
        item.fold(NotFound(s"Could not find ${service.model} with $id"))(todoItem =>
          Ok(todoItem.asJson))
      }

    case GET -> Root / prefix =>
      service.list.flatMap(l => Ok(l.asJson))

    case req @ POST -> Root / prefix =>
      for {
        todoItem     <- req.as[TodoItem]
        insertedItem <- service.insert(todoItem)
        response     <- Ok(insertedItem.asJson)
      } yield response

    case req @ PUT -> Root / prefix / IntVar(id) =>
      for {
        todoItem    <- req.as[TodoItem]
        updatedItem <- service.update(todoItem.copy(id = Some(id)))
        reponse     <- Ok(updatedItem.asJson)
      } yield reponse

    case DELETE -> Root / prefix / IntVar(id) =>
      service.destroy(id).flatMap(_ => Ok())
  }
}

object TodoItemApi {
  implicit def instance[F[_]: Effect](implicit service: TodoItemService[F]): TodoItemApi[F] =
    new TodoItemApi[F]
}
