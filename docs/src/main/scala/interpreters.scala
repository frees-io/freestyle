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

package freestyle.docs.core.interpreters

import cats.syntax.applicative._
import freestyle._

@free
trait KVStore {
  def put[A](key: String, value: A): FS[Unit]
  def get[A](key: String): FS[Option[A]]
  def delete(key: String): FS[Unit]
  def update[A](key: String, f: A => A): FS.Seq[Unit] =
    get[A](key).freeS flatMap {
      case Some(a) => put[A](key, f(a)).freeS
      case None    => ().pure[FS.Seq]
    }
}

@free
trait Log {
  def info(msg: String): FS[Unit]
  def warn(msg: String): FS[Unit]
}
