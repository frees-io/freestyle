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

import cats.MonadState

object state {

  final class StateSeedProvider[S] {

    @free sealed abstract class StateM {
      def get: OpPar[S]
      def set(s: S): OpPar[Unit]
      def modify(f: S => S): OpPar[Unit]
      def inspect[A](f: S => A): OpPar[A]
    }

    object implicits {

      implicit def freestyleStateMHandler[M[_]](
          implicit MS: MonadState[M, S]): StateM.Handler[M] = new StateM.Handler[M] {
        def get: M[S]                   = MS.get
        def set(s: S): M[Unit]          = MS.set(s)
        def modify(f: S => S): M[Unit]  = MS.modify(f)
        def inspect[A](f: S => A): M[A] = MS.inspect(f)
      }

      class StateInspectFreeSLift[F[_]: StateM] extends FreeSLift[F, Function1[S, ?]] {
        def liftFSPar[A](fa: S => A): FreeS.Par[F, A] = StateM[F].inspect(fa)
      }

      implicit def freeSLiftStateInspect[F[_]: StateM]: FreeSLift[F, Function1[S, ?]] =
        new StateInspectFreeSLift[F]

    }

  }

  def apply[S] = new StateSeedProvider[S]

}
