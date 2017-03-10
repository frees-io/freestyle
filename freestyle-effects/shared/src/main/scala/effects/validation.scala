package freestyle.effects

import freestyle._
import cats.{MonadState}
import cats.data.{State, Validated, ValidatedNel, NonEmptyList}


object validation {
  final class ValidationProvider[E]{
    type Errors = List[E]

    /** An algebra for introducing validation semantics in a program. **/
    @free sealed trait ValidationM[F[_]] {
      def valid[A](x: A): FreeS.Par[F, A]

      def invalid(err: E): FreeS.Par[F, Unit]

      def errors: FreeS.Par[F, Errors]

      def fromEither[A](x: Either[E, A]): FreeS.Par[F, Either[E, A]]

      def fromValidatedNel[A](x: ValidatedNel[E, A]): FreeS.Par[F, ValidatedNel[E, A]]
    }

    object implicits {
      implicit def freeStyleValidationMStateInterpreter[M[_]](
        implicit
          MS: MonadState[M, Errors]
      ): ValidationM.Handler[M] = new ValidationM.Handler[M] {
        def valid[A](x: A): M[A] = MS.pure(x)

        def errors: M[Errors] = MS.get

        def invalid(err: E): M[Unit] = MS.modify((s: Errors) => s :+ err)

        def fromEither[A](x: Either[E, A]): M[Either[E, A]] =
          x match {
            case Left(err) => MS.flatMap(invalid(err))((unit) => MS.pure(x))
            case Right(_) => MS.pure(x)
          }

        def fromValidatedNel[A](x: ValidatedNel[E, A]): M[ValidatedNel[E, A]] =
          x match {
            case Validated.Invalid(errs: NonEmptyList[E]) =>
              MS.flatMap(MS.modify(
                (s: Errors) => s ++ errs.toList
              ))((unit) => MS.pure(x))
            case Validated.Valid(_) =>  MS.pure(x)
          }
      }

      implicit class ValidSyntax[A](private val s: A){
        def valid[F[_] : ValidationM]: FreeS[F, A] = ValidationM[F].valid(s)
      }
      implicit class InvalidSyntax[A](private val e: E){
        def invalid[F[_] : ValidationM]: FreeS[F, Unit] = ValidationM[F].invalid(e)
      }
    }
  }

  def apply[E] = new ValidationProvider[E]
}
