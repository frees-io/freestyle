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

import cats.{~>, Foldable, MonadCombine}

object traverse {

  final class TraverseProvider[G[_]] {

    /** Acts as a generator providing traversable semantics to programs
     */
    @free sealed abstract class TraverseM {
      def empty[A]: OpPar[A]
      def fromTraversable[A](ta: G[A]): OpPar[A]
    }

    /** Interpretable as long as Foldable instance for G[_] and a MonadCombine for M[_] exists
     * in scope
     */
    object implicits {
      implicit def freestyleTraverseMHandler[F[_], M[_]](
          implicit MC: MonadCombine[M],
          FT: Foldable[G]): TraverseM.Handler[M] =
        new TraverseM.Handler[M] {
          def empty[A]: M[A]                     = MC.empty[A]
          def fromTraversable[A](ta: G[A]): M[A] = FT.foldMap(ta)(MC.pure)(MC.algebra[A])
        }
    }

    class TraverseFreeSLift[F[_]: TraverseM] extends FreeSLift[F, G] {
      def liftFSPar[A](fa: G[A]): FreeS.Par[F, A] = TraverseM[F].fromTraversable(fa)
    }

    implicit def freeSLiftTraverse[F[_]: TraverseM]: FreeSLift[F, G] =
      new TraverseFreeSLift[F]

  }

  def apply[T[_]] = new TraverseProvider[T]

  def list = apply[List]

}
