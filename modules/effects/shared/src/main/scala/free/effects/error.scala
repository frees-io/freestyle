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

package freestyle.free
package effects

object error {

  type ErrorM[F[_]] = freestyle.tagless.effects.error.ErrorM.StackSafe[F]

  val ErrorM = freestyle.tagless.effects.error.ErrorM.StackSafe

  trait FreeImplicits extends freestyle.tagless.effects.error.Implicits {

    class ErrorFreeSLift[F[_]: ErrorM] extends FreeSLift[F, Either[Throwable, ?]] {
      def liftFSPar[A](fa: Either[Throwable, A]): FreeS.Par[F, A] = ErrorM[F].either(fa)
    }

    implicit def freeSLiftError[F[_]: ErrorM]: FreeSLift[F, Either[Throwable, ?]] =
      new ErrorFreeSLift[F]

  }

  object implicits extends FreeImplicits
}
