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

import cats.{Eval, MonadError}

object error {

  @tagless(true) sealed trait ErrorM {
    def either[A](fa: Either[Throwable, A]): FS[A]
    def error[A](e: Throwable): FS[A]
    def catchNonFatal[A](a: Eval[A]): FS[A]
  }

  trait Implicits {

    implicit def freeStyleErrorMHandler[M[_]](
        implicit ME: MonadError[M, Throwable]): ErrorM.Handler[M] = new ErrorM.Handler[M] {
      def either[A](fa: Either[Throwable, A]): M[A] = ME.fromEither(fa)
      def error[A](e: Throwable): M[A]              = ME.raiseError[A](e)
      def catchNonFatal[A](a: Eval[A]): M[A]        = ME.catchNonFatal[A](a.value)
    }

    implicit class ErrorFSLift[A](fa: Either[Throwable, A])  {
      def liftF[F[_]: ErrorM]: F[A] = ErrorM[F].either(fa)
    }

  }

  object implicits extends Implicits
}
