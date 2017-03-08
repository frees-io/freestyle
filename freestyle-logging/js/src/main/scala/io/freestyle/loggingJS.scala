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

        def debugImpl(msg: String): M[Unit] = ME.catchNonFatal(logger.debug(msg))

        def debugWithCauseImpl(msg: String, cause: Throwable): M[Unit] =
          ME.catchNonFatal(logger.debug(msg, cause))

        def errorImpl(msg: String): M[Unit] = ME.catchNonFatal(logger.error(msg))

        def errorWithCauseImpl(msg: String, cause: Throwable): M[Unit] =
          ME.catchNonFatal(logger.error(msg, cause))

        def infoImpl(msg: String): M[Unit] = ME.catchNonFatal(logger.info(msg))

        def infoWithCauseImpl(msg: String, cause: Throwable): M[Unit] =
          ME.catchNonFatal(logger.info(msg, cause))

        def warnImpl(msg: String): M[Unit] = ME.catchNonFatal(logger.warn(msg))

        def warnWithCauseImpl(msg: String, cause: Throwable): M[Unit] =
          ME.catchNonFatal(logger.warn(msg, cause))
      }
  }
}
