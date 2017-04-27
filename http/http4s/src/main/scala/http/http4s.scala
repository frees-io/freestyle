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

package freestyle
package http

import cats.{Applicative, Monad}
import org.http4s.EntityEncoder

object http4s {

  implicit def freeSEntityEncoder[F[_], G[_], A](
      implicit NT: FSHandler[F, G],
      G: Monad[G],
      EE: EntityEncoder[G[A]]): EntityEncoder[FreeS[F, A]] =
    EE.contramap((f: FreeS[F, A]) => f.exec[G])

  implicit def freeSParEntityEncoder[F[_], G[_], A](
      implicit NT: FSHandler[F, G],
      G: Applicative[G],
      EE: EntityEncoder[G[A]]): EntityEncoder[FreeS.Par[F, A]] =
    EE.contramap((f: FreeS.Par[F, A]) => f.exec[G])
}
