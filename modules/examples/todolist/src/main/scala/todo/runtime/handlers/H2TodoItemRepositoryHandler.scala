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
package runtime.handlers

import doobie.imports._
import todo.model.TodoItem
import todo.persistence.TodoItemRepository

class H2TodoItemRepositoryHandler extends TodoItemRepository.Handler[ConnectionIO] {

  val drop: ConnectionIO[Int] =
    sql"""DROP TABLE todo_items IF EXISTS""".update.run

  val create: ConnectionIO[Int] =
    sql"""
          CREATE TABLE todo_items (
            id INT AUTO_INCREMENT PRIMARY KEY,
            item VARCHAR,
            todo_list_id INT NOT NULL,
            completed BOOLEAN NOT NULL,
            FOREIGN KEY (todo_list_id) REFERENCES todo_lists(id)
          )
       """.update.run

  def get(id: Int): ConnectionIO[Option[TodoItem]] =
    sql"""SELECT item, todo_list_id, completed, id FROM todo_items WHERE id = $id"""
      .query[TodoItem]
      .option

  def insert(input: TodoItem): ConnectionIO[Option[TodoItem]] =
    for {
      id <- sql"""INSERT INTO todo_items (item, todo_list_id, completed) VALUES (${input.item}, ${input.todoListId}, ${input.completed})""".update
        .withUniqueGeneratedKeys[Int]("id")
      item <- get(id)
    } yield item

  def list: ConnectionIO[List[TodoItem]] =
    sql"""SELECT item, todo_list_id, completed, id FROM todo_items ORDER BY id ASC"""
      .query[TodoItem]
      .list

  def update(input: TodoItem): ConnectionIO[Option[TodoItem]] =
    for {
      _    <- sql"""UPDATE todo_items SET item = ${input.item}, todo_list_id = ${input.todoListId}, completed = ${input.completed} WHERE id = ${input.id}""".update.run
      item <- get(input.id.get)
    } yield item

  def delete(id: Int): ConnectionIO[Int] =
    sql"""DELETE FROM todo_items WHERE id = $id""".update.run
}
