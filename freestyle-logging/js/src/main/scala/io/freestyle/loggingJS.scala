package freestyle

import cats.MonadError
import freestyle.logging._
import slogging._

object loggingJS {

  object implicits {
    implicit def freeStyleLoggingHandler[M[_]](
        implicit ME: MonadError[M, Throwable]): LoggingM.Handler[M] =
      new LoggingM.Handler[M] with LazyLogging {

        LoggerConfig.factory = PrintLoggerFactory()

        def debug(msg: String): M[Unit] = ME.catchNonFatal(logger.debug(msg))

        def debugWithCause(msg: String, cause: Throwable): M[Unit] =
          ME.catchNonFatal(logger.debug(msg, cause))

        def error(msg: String): M[Unit] = ME.catchNonFatal(logger.error(msg))

        def errorWithCause(msg: String, cause: Throwable): M[Unit] =
          ME.catchNonFatal(logger.error(msg, cause))

        def info(msg: String): M[Unit] = ME.catchNonFatal(logger.info(msg))

        def infoWithCause(msg: String, cause: Throwable): M[Unit] =
          ME.catchNonFatal(logger.info(msg, cause))

        def warn(msg: String): M[Unit] = ME.catchNonFatal(logger.warn(msg))

        def warnWithCause(msg: String, cause: Throwable): M[Unit] =
          ME.catchNonFatal(logger.warn(msg, cause))
      }
  }
}
