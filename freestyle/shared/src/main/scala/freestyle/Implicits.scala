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

import cats.Monad
import cats.arrow.FunctionK
import cats.data.Coproduct
import cats.free.{Free, FreeApplicative, Inject}
import shapeless.Lazy


trait Interpreters {

  implicit def interpretCoproduct[F[_], G[_], M[_]](
      implicit fm: FunctionK[F, M],
      gm: Lazy[FunctionK[G, M]]): FunctionK[Coproduct[F, G, ?], M] =
    fm or gm.value

  implicit def interpretAp[F[_], M[_]: Monad](
      implicit fInterpreter: FunctionK[F, M]): FunctionK[FreeApplicative[F, ?], M] =
    λ[FunctionK[FreeApplicative[F, ?], M]](_.foldMap(fInterpreter))

  // workaround for https://github.com/typelevel/cats/issues/1505
  implicit def catsFreeRightInjectInstanceLazy[F[_], G[_], H[_]](
      implicit I: Lazy[Inject[F, G]]): Inject[F, Coproduct[H, G, ?]] =
    Inject.catsFreeRightInjectInstance(I.value)
}

trait FreeSInstances {
  implicit def freestyleMonadForFreeS[F[_]]: Monad[FreeS[F, ?]] =
    Free.catsFreeMonadForFree[FreeApplicative[F, ?]]
}

object implicits extends Interpreters with FreeSInstances
