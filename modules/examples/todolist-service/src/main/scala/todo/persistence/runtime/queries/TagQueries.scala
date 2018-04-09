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
import examples.todolist.Tag

object TagQueries {
  def insertQuery(input: Tag): Update0 =
    sql"""
          INSERT INTO tags (name)
          VALUES (${input.name})
       """.update

  def getQuery(id: Int): Query0[Tag] =
    sql"""SELECT name, id FROM tags WHERE id = $id"""
      .query[Tag]

  def updateQuery(input: Tag): Update0 =
    sql"""
          UPDATE tags
          SET name = ${input.name}
          WHERE id = ${input.id}
       """.update

  def deleteQuery(id: Int): Update0 =
    sql"""DELETE FROM tags WHERE id = $id""".update

  val listQuery: Query0[Tag] =
    sql"""SELECT name, id FROM tags ORDER BY id ASC"""
      .query[Tag]

  val createQuery: Update0 =
    sql"""
          CREATE TABLE tags (
            id INT AUTO_INCREMENT PRIMARY KEY,
            name VARCHAR
          )
       """.update

  val dropQuery: Update0 =
    sql"""DROP TABLE tags IF EXISTS""".update
}
