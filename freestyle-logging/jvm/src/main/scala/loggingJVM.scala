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

package freestyle

import cats.MonadError
import cats.data.Kleisli
import cats.arrow.FunctionK
import freestyle.logging._
import journal._
import org.slf4j.LoggerFactory

object loggingJVM {

  object implicits {
    implicit def freeStyleLoggingHandler[M[_]](
        implicit ME: MonadError[M, Throwable]): LoggingM.Handler[M] =
      new LoggingM.Handler[M] {

        val logger = Logger("")

        private[this] def formatMessage(
            msg: String,
            sourceAndLineInfo: Boolean,
            line: sourcecode.Line,
            file: sourcecode.File): String =
          if (sourceAndLineInfo) s"$file:$line: $msg"
          else msg

        def debug(
            msg: String,
            sourceAndLineInfo: Boolean,
            line: sourcecode.Line,
            file: sourcecode.File): M[Unit] =
          ME.catchNonFatal(logger.debug(formatMessage(msg, sourceAndLineInfo, line, file)))

        def debugWithCause(
            msg: String,
            cause: Throwable,
            sourceAndLineInfo: Boolean,
            line: sourcecode.Line,
            file: sourcecode.File): M[Unit] =
          ME.catchNonFatal(logger.debug(formatMessage(msg, sourceAndLineInfo, line, file), cause))

        def error(
            msg: String,
            sourceAndLineInfo: Boolean,
            line: sourcecode.Line,
            file: sourcecode.File): M[Unit] =
          ME.catchNonFatal(logger.error(formatMessage(msg, sourceAndLineInfo, line, file)))

        def errorWithCause(
            msg: String,
            cause: Throwable,
            sourceAndLineInfo: Boolean,
            line: sourcecode.Line,
            file: sourcecode.File): M[Unit] =
          ME.catchNonFatal(logger.error(formatMessage(msg, sourceAndLineInfo, line, file), cause))

        def info(
            msg: String,
            sourceAndLineInfo: Boolean,
            line: sourcecode.Line,
            file: sourcecode.File): M[Unit] =
          ME.catchNonFatal(logger.info(formatMessage(msg, sourceAndLineInfo, line, file)))

        def infoWithCause(
            msg: String,
            cause: Throwable,
            sourceAndLineInfo: Boolean,
            line: sourcecode.Line,
            file: sourcecode.File): M[Unit] =
          ME.catchNonFatal(logger.info(formatMessage(msg, sourceAndLineInfo, line, file), cause))

        def warn(
            msg: String,
            sourceAndLineInfo: Boolean,
            line: sourcecode.Line,
            file: sourcecode.File): M[Unit] =
          ME.catchNonFatal(logger.warn(formatMessage(msg, sourceAndLineInfo, line, file)))

        def warnWithCause(
            msg: String,
            cause: Throwable,
            sourceAndLineInfo: Boolean,
            line: sourcecode.Line,
            file: sourcecode.File): M[Unit] =
          ME.catchNonFatal(logger.warn(formatMessage(msg, sourceAndLineInfo, line, file), cause))
      }
  }

  implicit def freeStyleLoggingKleisli[M[_], C: Manifest](
      implicit ME: MonadError[M, Throwable]): FSHandler[LoggingM.Op, Kleisli[M, Logger, ?]] =
    λ[FunctionK[LoggingM.Op, Kleisli[M, Logger, ?]]](_ match {
      case LoggingM.DebugOP(msg) =>
        Kleisli { logger =>
          ME.catchNonFatal(logger.debug(msg))
        }
      case LoggingM.DebugWithCauseOP(msg, cause) =>
        Kleisli { logger =>
          ME.catchNonFatal(logger.debug(msg, cause))
        }
      case LoggingM.ErrorOP(msg) =>
        Kleisli { logger =>
          ME.catchNonFatal(logger.error(msg))
        }
      case LoggingM.ErrorWithCauseOP(msg, cause) =>
        Kleisli { logger =>
          ME.catchNonFatal(logger.error(msg, cause))
        }
      case LoggingM.InfoOP(msg) =>
        Kleisli { logger =>
          ME.catchNonFatal(logger.info(msg))
        }
      case LoggingM.InfoWithCauseOP(msg, cause) =>
        Kleisli { logger =>
          ME.catchNonFatal(logger.info(msg, cause))
        }
      case LoggingM.WarnOP(msg) =>
        Kleisli { logger =>
          ME.catchNonFatal(logger.warn(msg))
        }
      case LoggingM.WarnWithCauseOP(msg, cause) =>
        Kleisli { logger =>
          ME.catchNonFatal(logger.warn(msg, cause))
        }
    })

  implicit def freeStyleLoggingKleisliRunner[M[_]](
      log: Logger): FunctionK[Kleisli[M, Logger, ?], M] =
    λ[FunctionK[Kleisli[M, Logger, ?], M]](_.run(log))

  implicit def freeStyleLoggingToM[M[_]: MonadError[?[_], Throwable]](
      log: Logger): FunctionK[LoggingM.Op, M] =
    freeStyleLoggingKleisli andThen freeStyleLoggingKleisliRunner(log)

}
