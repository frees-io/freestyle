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

package examples.todolist.persistence.runtime

import cats.Monad
import doobie.implicits._
import doobie.util.transactor.Transactor
import examples.todolist.Tag
import examples.todolist.persistence.TagRepository

class TagRepositoryHandler[F[_]: Monad](implicit T: Transactor[F])
    extends TagRepository.Handler[F] {

  import examples.todolist.persistence.runtime.queries.TagQueries._

  def insert(input: Tag): F[Option[Tag]] =
    insertQuery(input)
      .withUniqueGeneratedKeys[Int]("id")
      .flatMap(getQuery(_).option)
      .transact(T)

  def get(id: Int): F[Option[Tag]] =
    getQuery(id).option.transact(T)

  def update(tag: Tag): F[Option[Tag]] =
    updateQuery(tag).run
      .flatMap(_ => getQuery(tag.id.get).option)
      .transact(T)

  def delete(id: Int): F[Int] =
    deleteQuery(id).run.transact(T)

  def list: F[List[Tag]] =
    listQuery
      .to[List]
      .transact(T)

  def drop: F[Int] =
    dropQuery.run.transact(T)

  def create: F[Int] =
    createQuery.run.transact(T)

  def init: F[Int] =
    dropQuery.run
      .flatMap(
        drops =>
          createQuery.run
            .map(_ + drops))
      .transact(T)
}
