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
package services

import freestyle.free.effects.error.ErrorM
import freestyle.free._
import todo.model.{Tag, TodoForm, TodoList}
import todo.persistence.AppRepository
import cats.implicits._

@module
trait AppServices {

  val repo: AppRepository
  val tagServices: TagService
  val todoItemServices: TodoItemService
  val todoListServices: TodoListService
  val error: ErrorM

  val reset: FS.Seq[Int] =
    for {
      tags  <- tagServices.reset
      lists <- todoListServices.reset
      items <- todoItemServices.reset
    } yield tags + lists + items

  val list: FS.Seq[List[TodoForm]] = repo.list.map(_.groupBy(x => (x._1, x._2)).map {
    case ((todoList, tag), list) =>
      TodoForm(todoList, tag, list.flatMap(_._3))
  }.toList)

  def insert(form: TodoForm): FS.Seq[TodoForm] =
    for {
      tag <- tagServices.insert(form.tag)
      t   <- error.either[Tag](tag.toRight(new NoSuchElementException("Could not create Tag")))

      list <- todoListServices.insert(form.list.copy(tagId = t.id))
      l <- error.either[TodoList](
        list.toRight(new NoSuchElementException("Could not create TodoList")))

      i <- todoItemServices.batchedInsert(form.items.map(_.copy(todoListId = l.id)))
    } yield {
      form.copy(list = l, tag = t, items = i.sequence getOrElse form.items)
    }

  def update(form: TodoForm): FS.Seq[TodoForm] =
    for {
      tag <- tagServices.update(form.tag)
      t   <- error.either[Tag](tag.toRight(new NoSuchElementException("Could not update Tag")))

      list <- todoListServices.update(form.list.copy(tagId = t.id))
      l <- error.either[TodoList](
        list.toRight(new NoSuchElementException("Could not update TodoList")))

      i <- todoItemServices.batchedUpdate(form.items.map(_.copy(todoListId = l.id)))
    } yield {
      form.copy(list = l, tag = t, items = i.sequence getOrElse form.items)
    }

  def destroy(form: TodoForm): FS.Seq[Int] = {
    val todoItemIds: Option[List[Int]] = form.items.map(_.id).sequence
    val todoListId: Option[Int]        = form.list.id
    val todoTagId: Option[Int]         = form.tag.id

    val program = for {
      a <- todoItemIds.map(todoItemServices.batchedDestroy)
      b <- todoListId.map(todoItemServices.destroy)
      c <- todoTagId.map(tagServices.destroy)
    } yield List(a, b, c)

    program.map { x =>
      x.sequence.map(_.sum)
    } getOrElse (throw new NoSuchElementException("Could not delete"))
  }
}
