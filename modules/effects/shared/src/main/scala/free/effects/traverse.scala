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

object traverse {

  final class TraverseProvider[G[_]] {

    val taglessV: freestyle.tagless.effects.traverse.TraverseProvider[G] =
      freestyle.tagless.effects.traverse[G]

    type TraverseM[F[_]] = taglessV.TraverseM.StackSafe[F]

    val TraverseM = taglessV.TraverseM.StackSafe

    trait FreeImplicits extends taglessV.Implicits {

      class TraverseFreeSLift[F[_]: TraverseM] extends FreeSLift[F, G] {
        def liftFSPar[A](fa: G[A]): FreeS.Par[F, A] = TraverseM[F].fromTraversable(fa)
      }

      implicit def freeSLiftTraverse[F[_]: TraverseM]: FreeSLift[F, G] =
        new TraverseFreeSLift[F]

    }

    object implicits extends FreeImplicits

  }

  def apply[T[_]] = new TraverseProvider[T]

  def list = apply[List]

}
