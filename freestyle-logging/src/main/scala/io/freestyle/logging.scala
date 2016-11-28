package io.freestyle

import cats.Monad
import journal._

object logging {

  @free trait LoggingM[F[_]] {

    def debug(msg: String): FreeS[F, Unit]

    def debugWithCause(msg: String, cause: Throwable): FreeS[F, Unit]

    def error(msg: String): FreeS[F, Unit]

    def errorWithCause(msg: String, cause: Throwable): FreeS[F, Unit]

    def info(msg: String): FreeS[F, Unit]

    def infoWithCause(msg: String, cause: Throwable): FreeS[F, Unit]

    def warn(msg: String): FreeS[F, Unit]

    def warnWithCause(msg: String, cause: Throwable): FreeS[F, Unit]
  }

  object implicits {
    implicit def freeStyleLoggingInterpreter[M[_] : Monad]: LoggingM.Interpreter[M] =
      new LoggingM.Interpreter[M] {

        val log = Logger[this.type]

        def debugImpl(msg: String): M[Unit] = Monad[M].pure(log.debug(msg))

        def debugWithCauseImpl(msg: String, cause: Throwable): M[Unit] = Monad[M].pure(log.debug(msg, cause))

        def errorImpl(msg: String): M[Unit] = Monad[M].pure(log.error(msg))

        def errorWithCauseImpl(msg: String, cause: Throwable): M[Unit] = Monad[M].pure(log.error(msg, cause))

        def infoImpl(msg: String): M[Unit] = Monad[M].pure(log.info(msg))

        def infoWithCauseImpl(msg: String, cause: Throwable): M[Unit] = Monad[M].pure(log.info(msg, cause))

        def warnImpl(msg: String): M[Unit] = Monad[M].pure(log.warn(msg))

        def warnWithCauseImpl(msg: String, cause: Throwable): M[Unit] = Monad[M].pure(log.warn(msg, cause))
      }
  }

}