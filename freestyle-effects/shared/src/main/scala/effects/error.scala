package io.freestyle.effects

import io.freestyle._
import cats.{Eval, MonadError}

object error {

  @free sealed trait ErrorM[F[_]] {
    def either[A](fa: Either[Throwable, A]): FreeS.Par[F, A]
    def error[A](e: Throwable): FreeS.Par[F, A]
    def catchNonFatal[A](a: Eval[A]): FreeS.Par[F, A]
  }

  object implicits {

    implicit def freeStyleErrorMInterpreter[M[_]](
        implicit ME: MonadError[M, Throwable]): ErrorM.Interpreter[M] = new ErrorM.Interpreter[M] {
      def eitherImpl[A](fa: Either[Throwable, A]): M[A] = fa.fold(ME.raiseError[A], ME.pure[A])
      def errorImpl[A](e: Throwable): M[A]              = ME.raiseError[A](e)
      def catchNonFatalImpl[A](a: Eval[A]): M[A]        = ME.catchNonFatal[A](a.value)
    }

    class ErrorFreeSLift[F[_]: ErrorM] extends FreeSLift[F, Either[Throwable, ?]] {
      def liftFSPar[A](fa: Either[Throwable, A]): FreeS.Par[F, A] = ErrorM[F].either(fa)
    }

    implicit def freeSLiftError[F[_]: ErrorM]: FreeSLift[F, Either[Throwable, ?]] =
      new ErrorFreeSLift[F]

  }

}
