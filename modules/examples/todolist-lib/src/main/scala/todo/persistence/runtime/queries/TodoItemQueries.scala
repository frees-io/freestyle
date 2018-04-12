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

package examples.todolist.persistence.runtime.queries

import doobie.implicits.toSqlInterpolator
import doobie.util.query.Query0
import doobie.util.update.Update0
import examples.todolist.TodoItem

object TodoItemQueries {

  def insertQuery(input: TodoItem): Update0 =
    sql"""
          INSERT INTO todo_items(item, todo_list_id, completed)
          VALUES (${input.item}, ${input.todoListId}, ${input.completed})
       """.update

  def getQuery(id: Int): Query0[TodoItem] =
    sql"""SELECT item, todo_list_id, completed, id FROM todo_items WHERE id = $id"""
      .query[TodoItem]

  def updateQuery(input: TodoItem): Update0 =
    sql"""
          UPDATE todo_items
          SET item = ${input.item}, todo_list_id = ${input.todoListId}, completed = ${input.completed}
          WHERE id = ${input.id}
       """.update

  def deleteQuery(id: Int): Update0 =
    sql"""DELETE FROM todo_items WHERE id = $id""".update

  val listQuery: Query0[TodoItem] =
    sql"""SELECT item, todo_list_id, completed, id FROM todo_items ORDER BY id ASC"""
      .query[TodoItem]

  val dropQuery: Update0 =
    sql"""DROP TABLE todo_items IF EXISTS""".update

  val createQuery: Update0 =
    sql"""
          CREATE TABLE todo_items (
            id INT AUTO_INCREMENT PRIMARY KEY,
            item VARCHAR,
            todo_list_id INT NOT NULL,
            completed BOOLEAN NOT NULL,
            FOREIGN KEY (todo_list_id) REFERENCES todo_lists(id)
          )
       """.update
}
