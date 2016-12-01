package io.freestyle

import cats.MonadError
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
    implicit def freeStyleLoggingInterpreter[M[_], C: Manifest](implicit ME: MonadError[M, Throwable]): LoggingM.Interpreter[M] =
      new LoggingM.Interpreter[M] {

        val log = Logger[C]

        def debugImpl(msg: String): M[Unit] = ME.catchNonFatal(log.debug(msg))

        def debugWithCauseImpl(msg: String, cause: Throwable): M[Unit] = ME.catchNonFatal(log.debug(msg, cause))

        def errorImpl(msg: String): M[Unit] = ME.catchNonFatal(log.error(msg))

        def errorWithCauseImpl(msg: String, cause: Throwable): M[Unit] = ME.catchNonFatal(log.error(msg, cause))

        def infoImpl(msg: String): M[Unit] = ME.catchNonFatal(log.info(msg))

        def infoWithCauseImpl(msg: String, cause: Throwable): M[Unit] = ME.catchNonFatal(log.info(msg, cause))

        def warnImpl(msg: String): M[Unit] = ME.catchNonFatal(log.warn(msg))

        def warnWithCauseImpl(msg: String, cause: Throwable): M[Unit] = ME.catchNonFatal(log.warn(msg, cause))
      }
  }

}