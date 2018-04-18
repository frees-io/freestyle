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
import examples.todolist.TodoList

object TodoListQueries {

  def insertQuery(input: TodoList): Update0 =
    sql"""
          INSERT INTO todo_lists (title, tag_id)
          VALUES (${input.title}, ${input.tagId})
       """.update

  def getQuery(id: Int): Query0[TodoList] =
    sql"""SELECT title, tag_id, id FROM todo_lists WHERE id = $id"""
      .query[TodoList]

  def updateQuery(input: TodoList): Update0 =
    sql"""
          UPDATE todo_lists
          SET title = ${input.title}, tag_id = ${input.tagId}
          WHERE id = ${input.id}
       """.update

  def deleteQuery(id: Int): Update0 =
    sql"""DELETE FROM todo_lists WHERE id = $id""".update

  val listQuery: Query0[TodoList] =
    sql"""SELECT title, tag_id, id FROM todo_lists ORDER BY id ASC"""
      .query[TodoList]

  val dropQuery: Update0 =
    sql"""DROP TABLE todo_lists IF EXISTS""".update

  val createQuery: Update0 =
    sql"""
          CREATE TABLE todo_lists (
            id INT AUTO_INCREMENT PRIMARY KEY,
            title VARCHAR,
            tag_id INT,
            FOREIGN KEY (tag_id) REFERENCES tags(id)
          )
       """.update
}
