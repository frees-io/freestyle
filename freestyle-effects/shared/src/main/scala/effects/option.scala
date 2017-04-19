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
package effects

import cats.MonadFilter

object option {

  @free sealed trait OptionM {
    def option[A](fa: Option[A]): OpPar[A]
    def none[A]: OpPar[A]
  }

  trait OptionImplicits {
    implicit def freeStyleOptionMHandler[M[_]](
        implicit MF: MonadFilter[M]): OptionM.Handler[M] = new OptionM.Handler[M] {
      def option[A](fa: Option[A]): M[A] = fa.map(MF.pure[A]).getOrElse(MF.empty[A])
      def none[A]: M[A]                  = MF.empty[A]
    }

    class OptionFreeSLift[F[_]: OptionM] extends FreeSLift[F, Option] {
      def liftFSPar[A](fa: Option[A]): FreeS.Par[F, A] = OptionM[F].option(fa)
    }

    implicit def freeSLiftOption[F[_]: OptionM]: FreeSLift[F, Option] = new OptionFreeSLift[F]
  }

  object implicits extends OptionImplicits
}
