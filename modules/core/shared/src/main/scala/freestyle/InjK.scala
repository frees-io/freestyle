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

import cats._
import cats.arrow.FunctionK
import cats.free.Inject
import iota._

/** A generalized injection typeclass to abstract over support for
  * various coproduct implementations
  */
sealed trait InjK[F[_], G[_]] {
  def inj: FunctionK[F, G]
  def prj: FunctionK[G, λ[α => Option[F[α]]]]
  final def apply[A](fa: F[A]): G[A] = inj(fa)
  final def unapply[A](ga: G[A]): Option[F[A]] = prj(ga)
}

object InjK extends InjKInstances0

private[freestyle] sealed trait InjKInstances0 extends InjKInstances1 {
  implicit def injKReflexive[F[_]]: InjK[F, F] = new InjK[F, F] {
    def inj = FunctionK.id
    def prj = λ[F ~> λ[α => Option[F[α]]]](Some(_))
  }
}

private[freestyle] sealed trait InjKInstances1 {
  implicit def injKFromCatsInject[F[_], G[_]](
    implicit ev: Inject[F, G]
  ): InjK[F, G] = new InjK[F, G] {
    def inj = λ[F ~> G](ev.inj(_))
    def prj = λ[G ~> λ[α => Option[F[α]]]](ev.prj(_))
  }

  implicit def injKfromIotaCopKInjectL[F[_], L <: KList](
    implicit ev: CopK.InjectL[F, L]
  ): InjK[F, CopK[L, ?]] = new InjK[F, CopK[L, ?]] {
    def inj = λ[F ~> CopK[L, ?]](ev.inj(_))
    def prj = λ[CopK[L, ?] ~> λ[α => Option[F[α]]]](ev.proj(_))
  }
}
