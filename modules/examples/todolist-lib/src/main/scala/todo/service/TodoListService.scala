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

package examples.todolist.service

import cats.Monad
import cats.implicits._
import freestyle.tagless._
import freestyle.tagless.logging.LoggingM
import examples.todolist.TodoList
import examples.todolist.persistence.TodoListRepository

@module
trait TodoListService[F[_]] {

  implicit val M: Monad[F]
  implicit val L: LoggingM[F]

  val repo: TodoListRepository[F]

  val model: String = classOf[TodoList].getSimpleName

  def insert(item: TodoList): F[Option[TodoList]] =
    for {
      _            <- L.debug(s"Trying to insert a $model")
      insertedItem <- repo.insert(item)
      _            <- L.info(s"Tried to add a $model")
    } yield insertedItem

  def retrieve(id: Int): F[Option[TodoList]] =
    for {
      _    <- L.debug(s"Trying to retrieve a $model")
      item <- repo.get(id)
      _    <- L.info(s"Found ${item}")
    } yield item

  def update(item: TodoList): F[Option[TodoList]] =
    for {
      _           <- L.debug(s"Trying to update a $model")
      updatedItem <- repo.update(item)
      _           <- L.info(s"Tried to update a $model")
    } yield updatedItem

  def destroy(id: Int): F[Int] =
    for {
      _           <- L.debug(s"Trying to destroy a $model")
      deletedItem <- repo.delete(id)
      _           <- L.info(s"Tried to delete $model")
    } yield deletedItem

  def batchedInsert(items: List[TodoList]): F[List[Option[TodoList]]] =
    for {
      _             <- L.debug(s"Trying to insert batch $model")
      insertedItems <- items.traverse(repo.insert)
    } yield insertedItems

  def batchedUpdate(items: List[TodoList]): F[List[Option[TodoList]]] =
    for {
      _            <- L.debug(s"Trying to update batch $model")
      updatedItems <- items.traverse(repo.update)
    } yield updatedItems

  def batchedDestroy(ids: List[Int]): F[Int] =
    for {
      _            <- L.debug(s"Trying to delete batch $model")
      deletedItems <- ids.traverse(repo.delete)
    } yield deletedItems.sum

  val reset: F[Int] =
    for {
      _   <- L.debug(s"Trying to reset $model in repository")
      ops <- repo.init
      _   <- L.warn(s"Reset $model table in repository")
    } yield ops

  val list: F[List[TodoList]] =
    for {
      _     <- L.debug(s"Trying to get all $model models")
      items <- repo.list
      _     <- L.info(s"Found all $model models")
    } yield items
}
