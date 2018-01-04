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

package freestyle.free.cache.redis.rediscala

import cats.{~>}
import cats.data.Kleisli
import _root_.redis.commands.Transactions

/* An important challenge when executing operations in FreeStyle is to ensure that
 * parallel fragments execute their operations as _parallel_ as possible.
 * Since Redis is a single-threaded server, _parallel_ means sending operations together
 * in a single batch, which is possible if there are no data dependencies between them. */
class Interpret[F[_]](client: Transactions) extends (Kleisli[F, Commands, ?] ~> F) {

  override def apply[A](fa: Kleisli[F, Commands, A]): F[A] = {
    val transaction = client.transaction()
    val result      = fa(transaction)
    transaction.exec()
    result
  }

}
