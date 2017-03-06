package freestyle.effects

import freestyle._
import cats.{Monad, MonadState, ~>}
import cats.data.{State, StateT, Validated, ValidatedNel, NonEmptyList}
import cats.arrow.{FunctionK}

object validation {
  trait ValidationException {
    def explanation: String
  }
  case class NotValid(explanation: String) extends ValidationException

  object ValidationException {
    def apply(msg: String): ValidationException = NotValid(msg)
  }

  type Errors = List[ValidationException]

  @free sealed trait ValidationM[F[_]] {
    def valid[A](x: A): FreeS.Par[F, A]
    def invalid(err: ValidationException): FreeS.Par[F, Unit]
    def errors: FreeS.Par[F, Errors]

    def fromEither[A](x: Either[ValidationException, A]): FreeS.Par[F, Either[ValidationException, A]]
    def fromValidatedNel[A](x: ValidatedNel[ValidationException, A]): FreeS.Par[F, ValidatedNel[ValidationException, A]]
  }

  object implicits {
    implicit def freeStyleValidationMStateInterpreter[M[_]](
      implicit
        MS: MonadState[M, Errors]
    ): ValidationM.Interpreter[M] = new ValidationM.Interpreter[M] {
      def validImpl[A](x: A): M[A] = MS.pure(x)

      def errorsImpl: M[Errors] = MS.get

      def invalidImpl(err: ValidationException): M[Unit] = MS.modify((s: Errors) => s :+ err)

      def fromEitherImpl[A](x: Either[ValidationException, A]): M[Either[ValidationException, A]] =
      x match {
        case Left(err) => MS.flatMap(invalidImpl(err))((unit) => MS.pure(x))
        case Right(_) => MS.pure(x)
      }

      def fromValidatedNelImpl[A](x: ValidatedNel[ValidationException, A]): M[ValidatedNel[ValidationException, A]] =
        x match {
          case Validated.Invalid(errs: NonEmptyList[ValidationException]) => MS.pure(x)
            MS.flatMap(MS.modify(
              (s: Errors) => s ++ errs.toList
            ))((unit) => MS.pure(x))
          case Validated.Valid(_) =>  MS.pure(x)
        }
    }
  }
}
