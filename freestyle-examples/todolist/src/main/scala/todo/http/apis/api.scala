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
package http
package apis

class Api[F[_]](implicit genericApi: GenericApi, todoItemApi: TodoItemApi[F]) {
  val endpoints = genericApi.endpoints :+: todoItemApi.endpoints
}

object Api {
  implicit def instance[F[_]](implicit genericApi: GenericApi, todoItemApi: TodoItemApi[F]) =
    new Api[F]
}
