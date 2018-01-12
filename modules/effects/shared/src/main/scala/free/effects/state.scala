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

object state {

  final class StateSeedProvider[S] {

    val taglessV: freestyle.tagless.effects.state.StateSeedProvider[S] =
      freestyle.tagless.effects.state[S]

    type StateM[F[_]] = taglessV.StateM.StackSafe[F]

    val StateM = taglessV.StateM.StackSafe

    trait FreeImplicits extends taglessV.Implicits {

      class StateInspectFreeSLift[F[_]: StateM] extends FreeSLift[F, Function1[S, ?]] {
        def liftFSPar[A](fa: S => A): FreeS.Par[F, A] = StateM[F].inspect(fa)
      }

      implicit def freeSLiftStateInspect[F[_]: StateM]: FreeSLift[F, Function1[S, ?]] =
        new StateInspectFreeSLift[F]

    }

    object implicits extends FreeImplicits

  }

  def apply[S] = new StateSeedProvider[S]

}
