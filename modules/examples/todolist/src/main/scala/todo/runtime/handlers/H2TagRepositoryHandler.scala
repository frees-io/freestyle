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
package runtime.handlers

import doobie._
import doobie.implicits._
import todo.model.Tag
import todo.persistence.TagRepository

class H2TagRepositoryHandler extends TagRepository.Handler[ConnectionIO] {

  val drop: ConnectionIO[Int] =
    sql"""DROP TABLE tags IF EXISTS""".update.run

  val create: ConnectionIO[Int] =
    sql"""CREATE TABLE tags
          (
            id INT AUTO_INCREMENT PRIMARY KEY,
            name VARCHAR
          )
       """.update.run

  def get(id: Int): ConnectionIO[Option[Tag]] =
    sql"""SELECT name, id FROM tags WHERE id = $id"""
      .query[Tag]
      .option

  def insert(input: Tag): ConnectionIO[Option[Tag]] =
    for {
      id <- sql"""INSERT INTO tags (name) VALUES (${input.name})""".update
        .withUniqueGeneratedKeys[Int]("id")
      item <- get(id)
    } yield item

  def list: ConnectionIO[List[Tag]] =
    sql"""SELECT name, id FROM tags ORDER BY id ASC"""
      .query[Tag]
      .list

  def update(input: Tag): ConnectionIO[Option[Tag]] =
    for {
      _    <- sql"""UPDATE tags SET name = ${input.name} WHERE id = ${input.id}""".update.run
      item <- get(input.id.get)
    } yield item

  def delete(id: Int): ConnectionIO[Int] =
    sql"""DELETE FROM tags WHERE id = $id""".update.run

}
