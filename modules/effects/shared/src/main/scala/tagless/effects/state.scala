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

package freestyle.tagless
package effects

import cats.mtl.MonadState

object state {

  final class StateSeedProvider[S] {

    @tagless(true) sealed abstract class StateM {
      def get: FS[S]
      def set(s: S): FS[Unit]
      def modify(f: S => S): FS[Unit]
      def inspect[A](f: S => A): FS[A]
    }

    trait Implicits {

      implicit def freestyleStateMHandler[M[_]](implicit MS: MonadState[M, S]): StateM.Handler[M] =
        new StateM.Handler[M] {
          def get: M[S]                   = MS.get
          def set(s: S): M[Unit]          = MS.set(s)
          def modify(f: S => S): M[Unit]  = MS.modify(f)
          def inspect[A](f: S => A): M[A] = MS.inspect(f)
        }

      implicit class StateFSLift[A](fa: Function1[S, A])  {
        def liftF[F[_]: StateM]: F[A] = StateM[F].inspect(fa)
      }

    }

    object implicits extends Implicits
  }

  def apply[S] = new StateSeedProvider[S]

}
