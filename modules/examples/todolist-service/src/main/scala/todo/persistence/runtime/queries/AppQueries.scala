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
import examples.todolist.{Tag, TodoItem, TodoList}

object AppQueries {
  val listQuery: Query0[(TodoList, Tag, Option[TodoItem])] =
    sql"""
          SELECT lists.title, lists.tag_id, lists.id, tags.name, tags.id, items.item, items.todo_list_id, items.completed, items.id
          FROM todo_lists AS lists
          INNER JOIN tags ON lists.tag_id = tags.id
          LEFT JOIN todo_items AS items
          ON lists.id = items.todo_list_id
       """
      .query[(TodoList, Tag, Option[TodoItem])]
}
