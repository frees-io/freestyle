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

import cats.{Eval, MonadError}

object error {

  @free sealed trait ErrorM {
    def either[A](fa: Either[Throwable, A]): OpPar[A]
    def error[A](e: Throwable): OpPar[A]
    def catchNonFatal[A](a: Eval[A]): OpPar[A]
  }

  trait ErrorImplicits {

    implicit def freeStyleErrorMHandler[M[_]](
        implicit ME: MonadError[M, Throwable]): ErrorM.Handler[M] = new ErrorM.Handler[M] {
      def either[A](fa: Either[Throwable, A]): M[A] = fa.fold(ME.raiseError[A], ME.pure[A])
      def error[A](e: Throwable): M[A]              = ME.raiseError[A](e)
      def catchNonFatal[A](a: Eval[A]): M[A]        = ME.catchNonFatal[A](a.value)
    }

    class ErrorFreeSLift[F[_]: ErrorM] extends FreeSLift[F, Either[Throwable, ?]] {
      def liftFSPar[A](fa: Either[Throwable, A]): FreeS.Par[F, A] = ErrorM[F].either(fa)
    }

    implicit def freeSLiftError[F[_]: ErrorM]: FreeSLift[F, Either[Throwable, ?]] =
      new ErrorFreeSLift[F]

  }

  object implicits extends ErrorImplicits
}
