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

package freestyle.free
package effects

import cats.data.{NonEmptyList, State, Validated, ValidatedNel}
import cats.mtl.MonadState

object validation {
  final class ValidationProvider[E] {
    type Errors = List[E]

    /** An algebra for introducing validation semantics in a program. **/
    @free sealed trait ValidationM {
      def valid[A](x: A): FS[A]

      def invalid(err: E): FS[Unit]

      def errors: FS[Errors]

      def fromEither[A](x: Either[E, A]): FS[Either[E, A]]

      def fromValidatedNel[A](x: ValidatedNel[E, A]): FS[ValidatedNel[E, A]]
    }

    trait Implicits {
      implicit def freeStyleValidationMStateInterpreter[M[_]](
          implicit MS: MonadState[M, Errors]
      ): ValidationM.Handler[M] = new ValidationM.Handler[M] {
        def valid[A](x: A): M[A] = MS.monad.pure(x)

        def errors: M[Errors] = MS.get

        def invalid(err: E): M[Unit] = MS.modify((s: Errors) => s :+ err)

        def fromEither[A](x: Either[E, A]): M[Either[E, A]] =
          x match {
            case Left(err) => MS.monad.as(invalid(err), x)
            case Right(_)  => MS.monad.pure(x)
          }

        def fromValidatedNel[A](x: ValidatedNel[E, A]): M[ValidatedNel[E, A]] =
          x match {
            case Validated.Invalid(errs) =>
              MS.monad.as(MS.modify((s: Errors) => s ++ errs.toList), x)
            case Validated.Valid(_) => MS.monad.pure(x)
          }
      }

      implicit class ValidSyntax[A](private val s: A) {
        def liftValid[F[_]: ValidationM]: FreeS[F, A] = ValidationM[F].valid(s)
      }
      implicit class InvalidSyntax[A](private val e: E) {
        def liftInvalid[F[_]: ValidationM]: FreeS[F, Unit] = ValidationM[F].invalid(e)
      }
    }

    object implicits extends Implicits
  }

  def apply[E] = new ValidationProvider[E]

}
