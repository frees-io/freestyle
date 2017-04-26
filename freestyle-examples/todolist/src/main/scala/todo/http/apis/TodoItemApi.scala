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
package apis

import cats.~>
import com.twitter.util.Future
import io.finch._
import freestyle._
import freestyle.implicits._
import freestyle.http.finch._

import todo.definitions.models.TodoItem
import todo.definitions.persistence._
import todo.runtime.implicits._

class TodoItemApi[F[_]](implicit repo: TodoItemRepository[F], handler: F ~> Future) {

  val resetTodoItem: Endpoint[Int] =
    post("items" :: "reset") {
      repo.init.map(Ok(_))
    }

  // val getTodoItem: Endpoint[Option[TodoItem]] =
  val getTodoItem: Endpoint[TodoItem] =
    get("items" :: int) { id: Int =>
      repo.get(id).map(_.fold[Output[TodoItem]](NotFound(new NoSuchElementException))(Ok(_)))
    }

  val getTodoItems: Endpoint[List[TodoItem]] =
    get("items") {
      repo.list.map(Ok(_))
    }

  val insertTodoItem: Endpoint[Int] =
    post("items" :: param("item")) { item: String =>
      repo.insert(TodoItem(0, item)).map(Ok(_))
    }

  val updateTodoItem: Endpoint[Int] =
    put("items" :: int :: param("item")) { (id: Int, item: String) =>
      repo.update(TodoItem(id, item)).map(Ok(_))
    }

  val deleteTodoItem: Endpoint[Int] =
    delete("items" :: int) { id: Int =>
      repo.delete(id).map(Ok(_))
    }

  val endpoints = resetTodoItem :+: getTodoItem :+: getTodoItems :+: insertTodoItem :+: updateTodoItem :+: deleteTodoItem
}

object TodoItemApi {
  implicit def instance[F[_]](
      implicit repo: TodoItemRepository[F],
      handler: F ~> Future): TodoItemApi[F] =
    new TodoItemApi[F]
}
