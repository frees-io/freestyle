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

package persistence

import cats._
import cats.data._
import cats.implicits._

import doobie.imports._

import fs2.Task
import fs2.interop.cats._

import freestyle._
import freestyle.implicits._

import freestyle.doobie._
import freestyle.doobie.implicits._
import models.TodoItem

trait Repository[A, ID] {
  def drop: ConnectionIO[Int]
  def create: ConnectionIO[Int]

  def get(id: Int): ConnectionIO[Option[A]]
  def insert(input: A): ConnectionIO[Int]
  def list: ConnectionIO[List[A]]
  def update(input: A): ConnectionIO[Int]
  def delete(id: Int): ConnectionIO[Int]
}

trait TodoItemRepository extends Repository[TodoItem, Int]

class TodoItemH2Repository extends TodoItemRepository {
  val drop = sql"""DROP TABLE todo_items IF EXISTS""".update.run

  val create =
    sql"""CREATE TABLE todo_items (id INT AUTO_INCREMENT PRIMARY KEY, item VARCHAR)""".update.run

  def get(id: Int) =
    sql"""SELECT id, item FROM todo_items WHERE id = $id"""
      .query[TodoItem]
      .option

  def insert(input: TodoItem) =
    sql"""INSERT INTO todo_items (item) VALUES (${input.item})""".update.run

  def list =
    sql"""SELECT id, item FROM todo_items ORDER BY id ASC"""
      .query[TodoItem]
      .list

  def update(input: TodoItem) =
    sql"""UPDATE todo_items SET item = ${input.item} WHERE id = ${input.id}""".update.run

  def delete(id: Int) = sql"""DELETE FROM todo_items WHERE id = $id""".update.run
}

object TodoItemRepository {
  implicit def instance: TodoItemRepository = new TodoItemH2Repository
}

class TodoItemDao {
  import modules._

  val todoItemRepository = TodoItemRepository.instance

  def drop[F[_]: DoobieM](implicit persistence: Persistence[F]): FreeS[F, Int] =
    for {
      rows <- todoItemRepository.drop.liftFS[F]
    } yield rows

  def create[F[_]: DoobieM](implicit persistence: Persistence[F]): FreeS[F, Int] =
    for {
      rows <- todoItemRepository.create.liftFS[F]
    } yield rows

  def init[F[_]: DoobieM](implicit persistence: Persistence[F]): FreeS[F, Int] =
    for {
      drops   <- drop
      creates <- create
    } yield drops + creates

  def get[F[_]: DoobieM](id: Int)(
      implicit persistence: Persistence[F]): FreeS[F, Option[TodoItem]] =
    for {
      todoItem <- todoItemRepository.get(id).liftFS[F]
    } yield todoItem

  def insert[F[_]: DoobieM](input: TodoItem)(implicit persistence: Persistence[F]): FreeS[F, Int] =
    for {
      rows <- todoItemRepository.insert(input).liftFS[F]
    } yield rows

  def list[F[_]: DoobieM](implicit persistence: Persistence[F]): FreeS[F, List[TodoItem]] =
    for {
      todoItems <- todoItemRepository.list.liftFS[F]
    } yield todoItems

  def update[F[_]: DoobieM](input: TodoItem)(implicit persistence: Persistence[F]): FreeS[F, Int] =
    for {
      rows <- todoItemRepository.update(input).liftFS[F]
    } yield rows

  def delete[F[_]: DoobieM](id: Int)(implicit persistence: Persistence[F]): FreeS[F, Int] =
    for {
      rows <- todoItemRepository.delete(id).liftFS[F]
    } yield rows
}

object TodoItemDao {
  implicit def instance: TodoItemDao = new TodoItemDao
}
