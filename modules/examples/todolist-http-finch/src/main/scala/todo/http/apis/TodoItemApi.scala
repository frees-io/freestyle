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
package apis

import cats.~>
import cats.Monad
import cats.Monad.ops._
import com.twitter.util.Future
import io.finch._
import io.finch.circe._
import io.circe.generic.auto._
import examples.todolist.TodoItem
import examples.todolist.service.TodoItemService

class TodoItemApi[F[_]: Monad](implicit service: TodoItemService[F], handler: F ~> Future)
    extends CRUDApi[TodoItem] {

  import io.finch.syntax._

  private val prefix = "items"

  val reset = post(prefix :: "reset") {
    handler(service.reset.map(Ok))
  }

  val retrieve = get(prefix :: path[Int]) { id: Int =>
    handler(
      service.retrieve(id) map (item =>
        item.fold[Output[TodoItem]](
          NotFound(new NoSuchElementException(s"Could not find ${service.model} with $id")))(Ok)))
  } handle {
    case nse: NoSuchElementException => NotFound(nse)
  }

  val list = get(prefix) {
    handler(service.list.map(Ok))
  }

  val insert = post(prefix :: jsonBody[TodoItem]) { item: TodoItem =>
    handler(service.insert(item).map(Ok))
  }

  val update = put(prefix :: path[Int] :: jsonBody[TodoItem]) { (id: Int, item: TodoItem) =>
    handler(service.update(item.copy(id = Some(id))).map(Ok))
  }

  val destroy = delete(prefix :: path[Int]) { id: Int =>
    handler(service.destroy(id).map(Ok))
  }
}

object TodoItemApi {
  implicit def instance[F[_]: Monad](
      implicit service: TodoItemService[F],
      handler: F ~> Future): TodoItemApi[F] =
    new TodoItemApi[F]
}
