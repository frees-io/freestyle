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

import cats.implicits._
import cats.effect.Effect
import examples.todolist.service.TodoListService
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl

class TodoListApi[F[_]: Effect](implicit service: TodoListService[F]) extends Http4sDsl[F] {

  private val prefix = "lists"

  implicit val todoListEncoder: EntityEncoder[F, TodoList] = jsonEncoderOf[F, TodoList]
  implicit val todoListDecoder: EntityDecoder[F, TodoList] = jsonOf[F, TodoList]

  val endpoints = HttpService[F] {
    case POST -> Root / prefix =>
      service.reset.flatMap(e => Ok(e.asJson))

    case GET -> Root / prefix / IntVar(id) =>
      service.retrieve(id) flatMap { item =>
        item.fold(NotFound(s"Could not find ${service.model} with $id"))(todoList =>
          Ok(todoList.asJson))
      }

    case GET -> Root / prefix =>
      service.list.flatMap(l => Ok(l.asJson))

    case req @ POST -> Root / prefix =>
      for {
        todoList         <- req.as[TodoList]
        insertedTodoList <- service.insert(todoList)
        response         <- Ok(insertedTodoList.asJson)
      } yield response

    case req @ PUT -> Root / prefix / IntVar(id) =>
      for {
        todoList        <- req.as[TodoList]
        updatedTodoList <- service.update(todoList.copy(id = Some(id)))
        reponse         <- Ok(updatedTodoList.asJson)
      } yield reponse

    case DELETE -> Root / prefix / IntVar(id) =>
      service.destroy(id).flatMap(_ => Ok())
  }
}

object TodoListApi {
  implicit def instance[F[_]: Effect](implicit service: TodoListService[F]): TodoListApi[F] =
    new TodoListApi[F]
}
