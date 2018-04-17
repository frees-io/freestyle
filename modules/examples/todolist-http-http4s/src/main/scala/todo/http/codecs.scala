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

package examples.todolist
package http

import cats.effect.Effect
import io.circe.generic.auto._
import org.http4s._
import org.http4s.circe._

object codecs {

  implicit def tagEncoder[F[_]: Effect]: EntityEncoder[F, Tag] = jsonEncoderOf[F, Tag]
  implicit def tagDecoder[F[_]: Effect]: EntityDecoder[F, Tag] = jsonOf[F, Tag]

  implicit def todoItemEncoder[F[_]: Effect]: EntityEncoder[F, TodoItem] =
    jsonEncoderOf[F, TodoItem]
  implicit def todoItemDecoder[F[_]: Effect]: EntityDecoder[F, TodoItem] = jsonOf[F, TodoItem]

  implicit def todoListEncoder[F[_]: Effect]: EntityEncoder[F, TodoList] =
    jsonEncoderOf[F, TodoList]
  implicit def todoListDecoder[F[_]: Effect]: EntityDecoder[F, TodoList] = jsonOf[F, TodoList]

  implicit def todoFormEncoder[F[_]: Effect]: EntityEncoder[F, TodoForm] =
    jsonEncoderOf[F, TodoForm]
  implicit def todoFormDecoder[F[_]: Effect]: EntityDecoder[F, TodoForm] =
    jsonOf[F, TodoForm]
}
