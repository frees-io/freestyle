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

package examples.todolist.service

import cats.Monad
import cats.implicits._
import freestyle.tagless._
import freestyle.tagless.effects.error.ErrorM
import examples.todolist.{Tag, TodoForm, TodoList}
import examples.todolist.persistence.AppRepository

@module
trait AppService[F[_]] {

  implicit val M: Monad[F]

  val repo: AppRepository[F]
  val tagService: TagService[F]
  val todoItemService: TodoItemService[F]
  val todoListService: TodoListService[F]

  val error: ErrorM

  def insert(form: TodoForm): F[TodoForm] = {
    for {
      tag <- tagService.insert(form.tag)
      t   <- error.either[Tag](tag.toRight(new NoSuchElementException("Could not create Tag")))

      list <- todoListService.insert(form.list.copy(tagId = t.id))
      l <- error.either[TodoList](
        list.toRight(new NoSuchElementException("Could not create TodoList")))

      item <- todoItemService.batchedInsert(form.items.map(_.copy(todoListId = l.id)))
    } yield {
      form.copy(list = l, tag = t, items = item.sequence getOrElse form.items)
    }
  }

  def update(form: TodoForm): F[TodoForm] =
    for {
      tag <- tagService.update(form.tag)
      t   <- error.either[Tag](tag.toRight(new NoSuchElementException("Could not create Tag")))

      list <- todoListService.update(form.list.copy(tagId = t.id))
      l <- error.either[TodoList](
        list.toRight(new NoSuchElementException("Could not create TodoList")))

      item <- todoItemService.batchedUpdate(form.items.map(_.copy(todoListId = l.id)))
    } yield {
      form.copy(list = l, tag = t, items = item.sequence getOrElse form.items)
    }

  def destroy(form: TodoForm): F[Int] = {
    val itemIds: Option[List[Int]] = form.items.map(_.id).sequence
    val listId: Option[Int]        = form.list.id
    val tagId: Option[Int]         = form.tag.id

    val program = for {
      a <- itemIds.map(todoItemService.batchedDestroy)
      b <- listId.map(todoListService.destroy)
      c <- tagId.map(tagService.destroy)
    } yield List(a, b, c)

    program
      .map(_.sequence.map(_.sum))
      .getOrElse(throw new NoSuchElementException("Could not delete"))
  }

  val reset: F[Int] =
    for {
      tags  <- tagService.reset
      lists <- todoListService.reset
      items <- todoItemService.reset
    } yield tags + lists + items

  val list: F[List[TodoForm]] =
    repo.list.map(_.groupBy(x => (x._1, x._2)).map {
      case ((todoList, tag), list) =>
        TodoForm(todoList, tag, list.flatMap(_._3))
    }.toList)
}
