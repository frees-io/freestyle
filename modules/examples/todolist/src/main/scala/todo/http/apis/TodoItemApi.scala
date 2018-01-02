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
package http
package apis

import cats.~>
import cats.instances.list._
import cats.syntax.traverse._
import com.twitter.util.Future
import io.finch._
import io.finch.circe._
import io.circe.generic.auto._
import freestyle.free._
import freestyle.free.http.finch._
import freestyle.free.logging._
import todo.model.TodoItem
import todo.services._

class TodoItemApi[F[_]](implicit service: TodoItemService[F], handler: F ~> Future)
    extends CRUDApi[TodoItem] {

  val reset: Endpoint[Int] =
    post(service.prefix :: "reset") {
      service.reset.map(Ok(_))
    }

  val retrieve: Endpoint[TodoItem] =
    get(service.prefix :: int) { id: Int =>
      service.retrieve(id) map (item =>
        item.fold[Output[TodoItem]](
          NotFound(new NoSuchElementException(s"Could not find ${service.model} with $id")))(Ok(_)))
    } handle {
      case nse: NoSuchElementException => NotFound(nse)
    }

  val list: Endpoint[List[TodoItem]] =
    get(service.prefix) {
      service.list.map(Ok(_))
    }

  val insert: Endpoint[Option[TodoItem]] =
    post(service.prefix :: jsonBody[TodoItem]) { item: TodoItem =>
      service.insert(item).map(Ok(_))
    }

  val update: Endpoint[Option[TodoItem]] =
    put(service.prefix :: int :: jsonBody[TodoItem]) { (id: Int, item: TodoItem) =>
      service.update(item.copy(id = Some(id))).map(Ok(_))
    }

  val destroy: Endpoint[Int] =
    delete(service.prefix :: int) { id: Int =>
      service.destroy(id).map(Ok(_))
    }
}

object TodoItemApi {
  implicit def instance[F[_]](
      implicit service: TodoItemService[F],
      handler: F ~> Future): TodoItemApi[F] =
    new TodoItemApi[F]
}
