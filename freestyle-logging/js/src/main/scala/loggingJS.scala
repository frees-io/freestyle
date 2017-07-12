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

import cats.Monad
import freestyle.logging._
import slogging._

object loggingJS {

  trait Implicits {
    implicit def freeStyleLoggingHandler[M[_]](implicit M: Monad[M]): LoggingM.Handler[M] =
      new LoggingM.Handler[M] with LazyLogging {

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
          M.pure(logger.debug(formatMessage(msg, sourceAndLineInfo, line, file)))

        def debugWithCause(
            msg: String,
            cause: Throwable,
            sourceAndLineInfo: Boolean,
            line: sourcecode.Line,
            file: sourcecode.File): M[Unit] =
          M.pure(logger.debug(formatMessage(msg, sourceAndLineInfo, line, file), cause))

        def error(
            msg: String,
            sourceAndLineInfo: Boolean,
            line: sourcecode.Line,
            file: sourcecode.File): M[Unit] =
          M.pure(logger.error(formatMessage(msg, sourceAndLineInfo, line, file)))

        def errorWithCause(
            msg: String,
            cause: Throwable,
            sourceAndLineInfo: Boolean,
            line: sourcecode.Line,
            file: sourcecode.File): M[Unit] =
          M.pure(logger.error(formatMessage(msg, sourceAndLineInfo, line, file), cause))

        def info(
            msg: String,
            sourceAndLineInfo: Boolean,
            line: sourcecode.Line,
            file: sourcecode.File): M[Unit] =
          M.pure(logger.info(formatMessage(msg, sourceAndLineInfo, line, file)))

        def infoWithCause(
            msg: String,
            cause: Throwable,
            sourceAndLineInfo: Boolean,
            line: sourcecode.Line,
            file: sourcecode.File): M[Unit] =
          M.pure(logger.info(formatMessage(msg, sourceAndLineInfo, line, file), cause))

        def warn(
            msg: String,
            sourceAndLineInfo: Boolean,
            line: sourcecode.Line,
            file: sourcecode.File): M[Unit] =
          M.pure(logger.warn(formatMessage(msg, sourceAndLineInfo, line, file)))

        def warnWithCause(
            msg: String,
            cause: Throwable,
            sourceAndLineInfo: Boolean,
            line: sourcecode.Line,
            file: sourcecode.File): M[Unit] =
          M.pure(logger.warn(formatMessage(msg, sourceAndLineInfo, line, file), cause))
      }
  }

  object implicits extends Implicits
}
