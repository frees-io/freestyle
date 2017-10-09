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
import cats.free.{Free, FreeApplicative }

import iota._

trait Interpreters {
  implicit def interpretIotaCopK[F[a] <: CopK[_, a], G[_]]: FSHandler[F, G] =
    macro _root_.iota.internal.CopKFunctionKMacros.summon[F, G]
}

trait FreeSInstances {
  implicit def freestyleApplicativeForFreeS[F[_]]: Applicative[FreeS.Par[F, ?]] =
    FreeApplicative.freeApplicative[F]

  implicit def freestyleMonadForFreeS[F[_]]: Monad[FreeS[F, ?]] =
    Free.catsFreeMonadForFree[FreeApplicative[F, ?]]
}

object implicits extends Interpreters with FreeSInstances
