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
package services

import freestyle.free.logging.LoggingM
import freestyle.free._
import todo.model.Tag
import todo.persistence.TagRepository
import cats.implicits._

@module
trait TagService {

  val repo: TagRepository

  val log: LoggingM

  val prefix = "tags"

  val model = classOf[Tag].getSimpleName

  val reset: FS.Seq[Int] =
    for {
      _ <- log.debug(s"Trying to reset $model in repository")
      r <- repo.init
      _ <- log.warn(s"POST /$prefix/reset: Initialize the $model table")
    } yield r

  def retrieve(id: Int): FS.Seq[Option[Tag]] =
    for {
      _    <- log.debug(s"Trying to retrieve an $model")
      item <- repo.get(id)
      _    <- log.info(s"GET /$prefix/$id: Found $item")
    } yield item

  val list: FS.Seq[List[Tag]] =
    for {
      _     <- log.debug(s"Trying to get all $model models")
      items <- repo.list
      _     <- log.info(s"GET /$prefix: Found all the $model models")
    } yield items

  def insert(item: Tag): FS.Seq[Option[Tag]] =
    for {
      _ <- log.debug(s"Trying to insert a $model")
      r <- repo.insert(item)
      _ <- log.info(s"POST /$prefix with $item: Tried to add $model")
    } yield r

  def update(item: Tag): FS.Seq[Option[Tag]] =
    for {
      _ <- log.debug(s"Trying to update a $model")
      r <- repo.update(item)
      _ <- log.info(s"PUT /$prefix/$item.id with $item: Tried to update $model")
    } yield r

  def destroy(id: Int): FS.Seq[Int] =
    for {
      _ <- log.debug(s"Trying to delete a $model")
      r <- repo.delete(id)
      _ <- log.info(s"DELETE /$prefix/$id: Tried to delete $model")
    } yield r

  def batchedInsert(items: List[Tag]): FS.Seq[List[Option[Tag]]] =
    for {
      _ <- log.debug(s"Trying to insert batch $model")
      r <- items.traverse(repo.insert)
    } yield r

  def batchedUpdate(items: List[Tag]): FS.Seq[List[Option[Tag]]] =
    for {
      _ <- log.debug(s"Trying to update batch $model")
      r <- items.traverse(repo.update)
    } yield r

  def batchedDestroy(ids: List[Int]): FS.Seq[Int] =
    for {
      _ <- log.debug(s"Trying to destroy batch $model")
      r <- ids.traverse(repo.delete)
    } yield r.sum
}