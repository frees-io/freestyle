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

package apis

import cats._
import cats.data._
import cats.implicits._

import doobie.imports._

import fs2.Task
import fs2.interop.cats._

import io.finch._
import models.TodoItem
import persistence.TodoItemDao

import freestyle._
import freestyle.implicits._

import freestyle.doobie._
import freestyle.doobie.implicits._

import freestyle.http.finch._

import modules._

trait TodoItemApi {
  implicit val xa = DriverManagerTransactor[Task](
    "org.h2.Driver",
    "jdbc:h2:mem:freestyle-todo;DB_CLOSE_DELAY=-1",
    "sa",
    ""
  )

  private val todoItemDao = TodoItemDao.instance

  val resetTodoItem: Endpoint[Int] =
    post("items" :: "reset") {
      Ok(todoItemDao.init[Persistence.Op].exec[Task].unsafeRunSync.toOption.get)
//      todoItemDao.init[Persistence.Op].exec[Task].map(Ok(_))
    }

  val getTodoItem: Endpoint[Option[TodoItem]] =
    get("items" :: int) { id: Int =>
      Ok(todoItemDao.get[Persistence.Op](id).exec[Task].unsafeRunSync.toOption.get)
    }

  val getTodoItems: Endpoint[List[TodoItem]] =
    get("items") {
      Ok(todoItemDao.list[Persistence.Op].exec[Task].unsafeRunSync.toOption.get)
    }

  val insertTodoItem: Endpoint[Int] =
    post("items" :: param("item")) { item: String =>
      Ok(
        todoItemDao
          .insert[Persistence.Op](TodoItem(0, item))
          .exec[Task]
          .unsafeRunSync
          .toOption
          .get)
    }

  val updateTodoItem: Endpoint[Int] =
    put("items" :: int :: param("item")) { (id: Int, item: String) =>
      Ok(
        todoItemDao
          .update[Persistence.Op](TodoItem(id, item))
          .exec[Task]
          .unsafeRunSync
          .toOption
          .get)
    }

  val deleteTodoItem: Endpoint[Int] =
    delete("items" :: int) { id: Int =>
      Ok(todoItemDao.delete[Persistence.Op](id).exec[Task].unsafeRunSync.toOption.get)
    }

  val todoItemApi = (resetTodoItem :+: getTodoItem :+: getTodoItems :+: insertTodoItem :+: updateTodoItem :+: deleteTodoItem)
}
