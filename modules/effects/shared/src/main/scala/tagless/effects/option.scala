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

import cats.Applicative
import cats.mtl.FunctorEmpty

object option {

  @tagless @stacksafe sealed trait OptionM {
    def option[A](fa: Option[A]): FS[A]
    def none[A]: FS[A]
  }

  trait Implicits {
    implicit def freeStyleOptionMHandler[M[_]](
        implicit FE: FunctorEmpty[M], A: Applicative[M]): OptionM.Handler[M] =
      new OptionM.Handler[M] {
        def option[A](fa: Option[A]): M[A] = FE.flattenOption(A.pure(fa))
        def none[A]: M[A]                  = option(Option.empty[A])
      }

    implicit class OptionFSLift[F[_]: OptionM, A](fa: Option[A])  {
      def liftF: F[A] = OptionM[F].option(fa)
    }

  }

  object implicits extends Implicits
}