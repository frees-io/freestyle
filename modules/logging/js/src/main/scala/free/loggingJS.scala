/*
 * Copyright 2017-2018 47 Degrees, LLC. <http://www.47deg.com>
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

package freestyle.free

import cats.Applicative
import freestyle.logging._
import freestyle.free.logging._
import slogging._

object loggingJS {

  sealed abstract class FreeSLoggingMHandler[M[_]] extends LoggingM.Handler[M] with LazyLogging {
    import sourcecode.{File, Line}

    implicit val _ = logger

    protected def withLogger[A](f: Logger => A)(implicit logger: Logger): M[A]

    def debug(msg: String, sourceAndLineInfo: Boolean)(implicit line: Line, file: File): M[Unit] =
      withLogger(_.debug(formatMessage(msg, sourceAndLineInfo, line, file)))

    def debugWithCause(msg: String, cause: Throwable, sourceAndLineInfo: Boolean)(
        implicit
        line: Line,
        file: File): M[Unit] =
      withLogger(_.debug(formatMessage(msg, sourceAndLineInfo, line, file), cause))

    def error(msg: String, sourceAndLineInfo: Boolean)(implicit line: Line, file: File): M[Unit] =
      withLogger(_.error(formatMessage(msg, sourceAndLineInfo, line, file)))

    def errorWithCause(msg: String, cause: Throwable, sourceAndLineInfo: Boolean)(
        implicit
        line: Line,
        file: File): M[Unit] =
      withLogger(_.error(formatMessage(msg, sourceAndLineInfo, line, file), cause))

    def info(msg: String, sourceAndLineInfo: Boolean)(implicit line: Line, file: File): M[Unit] =
      withLogger(_.info(formatMessage(msg, sourceAndLineInfo, line, file)))

    def infoWithCause(msg: String, cause: Throwable, sourceAndLineInfo: Boolean)(
        implicit
        line: Line,
        file: File): M[Unit] =
      withLogger(_.info(formatMessage(msg, sourceAndLineInfo, line, file), cause))

    def warn(msg: String, sourceAndLineInfo: Boolean)(implicit line: Line, file: File): M[Unit] =
      withLogger(_.warn(formatMessage(msg, sourceAndLineInfo, line, file)))

    def warnWithCause(msg: String, cause: Throwable, sourceAndLineInfo: Boolean)(
        implicit
        line: Line,
        file: File): M[Unit] =
      withLogger(_.warn(formatMessage(msg, sourceAndLineInfo, line, file), cause))
  }

  trait Implicits {
    implicit def freeStyleLoggingHandler[M[_]: Applicative]: LoggingM.Handler[M] =
      new FreeSLoggingMHandler[M] {
        protected def withLogger[A](f: Logger => A)(implicit logger: Logger): M[A] =
          Applicative[M].pure(f(logger))
      }
  }

  object implicits extends Implicits
}
