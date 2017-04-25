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
package definitions
package persistence

import models.TodoItem
import doobie.imports._
import freestyle._

@free
trait TodoItemRepository {
  def drop: FS[Int]
  def create: FS[Int]
  def get(id: Int): FS[Option[TodoItem]]
  def insert(input: TodoItem): FS[Int]
  def list: FS[List[TodoItem]]
  def update(input: TodoItem): FS[Int]
  def delete(id: Int): FS[Int]
  def init: FS.Seq[Int] =
    for {
      drops   <- drop
      creates <- create
    } yield drops + creates
}

class H2TodoItemRepositoryHandler extends TodoItemRepository.Handler[ConnectionIO] {
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
