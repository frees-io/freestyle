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

object option {

  type OptionM[F[_]] = freestyle.tagless.effects.option.OptionM.StackSafe[F]

  val OptionM = freestyle.tagless.effects.option.OptionM.StackSafe

  trait FreeImplicits extends freestyle.tagless.effects.option.Implicits {

    class OptionFreeSLift[F[_]: OptionM] extends FreeSLift[F, Option] {
      def liftFSPar[A](fa: Option[A]): FreeS.Par[F, A] = OptionM[F].option(fa)
    }

    implicit def freeSLiftOption[F[_]: OptionM]: FreeSLift[F, Option] = new OptionFreeSLift[F]
  }

  object implicits extends FreeImplicits
}