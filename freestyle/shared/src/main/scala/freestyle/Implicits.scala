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

import cats.{ Applicative, Monad }
import cats.data.Coproduct
import cats.free.{Free, FreeApplicative, Inject}
import shapeless.Lazy

trait Interpreters {

  implicit def interpretCoproduct[F[_], G[_], M[_]](
      implicit fm: FSHandler[F, M],
      gm: Lazy[FSHandler[G, M]]): FSHandler[Coproduct[F, G, ?], M] =
    fm or gm.value

  // workaround for https://github.com/typelevel/cats/issues/1505
  implicit def catsFreeRightInjectInstanceLazy[F[_], G[_], H[_]](
      implicit I: Lazy[Inject[F, G]]): Inject[F, Coproduct[H, G, ?]] =
    Inject.catsFreeRightInjectInstance(I.value)

}

trait FreeSInstances {
  implicit def freestyleApplicativeForFreeS[F[_]]: Applicative[FreeS.Par[F, ?]] =
    FreeApplicative.freeApplicative[F]

  implicit def freestyleMonadForFreeS[F[_]]: Monad[FreeS[F, ?]] =
    Free.catsFreeMonadForFree[FreeApplicative[F, ?]]
}

object implicits extends Interpreters with FreeSInstances
