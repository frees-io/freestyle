package io.freestyle

object logging {

  @free
  trait LoggingM[F[_]] {

    def debug(msg: String): FreeS[F, Unit]

    def debugWithCause(msg: String, cause: Throwable): FreeS[F, Unit]

    def error(msg: String): FreeS[F, Unit]

    def errorWithCause(msg: String, cause: Throwable): FreeS[F, Unit]

    def info(msg: String): FreeS[F, Unit]

    def infoWithCause(msg: String, cause: Throwable): FreeS[F, Unit]

    def warn(msg: String): FreeS[F, Unit]

    def warnWithCause(msg: String, cause: Throwable): FreeS[F, Unit]
  }

}
