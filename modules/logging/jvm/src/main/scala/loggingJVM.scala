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

import cats.data.Kleisli
import cats.arrow.FunctionK
import cats.Monad
import freestyle.logging._
import journal._
import org.slf4j.LoggerFactory

object loggingJVM {

  private[this] def formatMessage(
      msg: String,
      srcInfo: Boolean,
      line: sourcecode.Line,
      file: sourcecode.File): String =
    if (srcInfo) s"$file:$line: $msg"
    else msg

  trait Implicits {
    implicit def freeStyleLoggingKleisli[M[_], C: Manifest](
        implicit M: Monad[M]): LoggingM.Handler[Kleisli[M, Logger, ?]] =
      new LoggingM.Handler[Kleisli[M, Logger, ?]] {
        import sourcecode.{File, Line}

        type KL[A] = Kleisli[M, Logger, A]

        private def withLogger[A](f: Logger => A): KL[A] =
          Kleisli.ask[M, Logger].map(f)

        def debug(msg: String, srcInfo: Boolean, line: Line, file: File): KL[Unit] =
          withLogger(_.debug(formatMessage(msg, srcInfo, line, file)))

        def debugWithCause(
            msg: String,
            cause: Throwable,
            srcInfo: Boolean,
            line: Line,
            file: File): KL[Unit] =
          withLogger(_.debug(formatMessage(msg, srcInfo, line, file), cause))

        def error(msg: String, srcInfo: Boolean, line: Line, file: File): KL[Unit] =
          withLogger(_.error(formatMessage(msg, srcInfo, line, file)))

        def errorWithCause(
            msg: String,
            cause: Throwable,
            srcInfo: Boolean,
            line: Line,
            file: File): KL[Unit] =
          withLogger(_.error(formatMessage(msg, srcInfo, line, file), cause))

        def info(msg: String, srcInfo: Boolean, line: Line, file: File): KL[Unit] =
          withLogger(_.info(formatMessage(msg, srcInfo, line, file)))

        def infoWithCause(
            msg: String,
            cause: Throwable,
            srcInfo: Boolean,
            line: Line,
            file: File): KL[Unit] =
          withLogger(_.info(formatMessage(msg, srcInfo, line, file), cause))

        def warn(msg: String, srcInfo: Boolean, line: Line, file: File): KL[Unit] =
          withLogger(_.warn(formatMessage(msg, srcInfo, line, file)))

        def warnWithCause(
            msg: String,
            cause: Throwable,
            srcInfo: Boolean,
            line: Line,
            file: File): KL[Unit] =
          withLogger(_.warn(formatMessage(msg, srcInfo, line, file), cause))
      }

    implicit def freeStyleLoggingKleisliRunner[M[_]](
        log: Logger): FSHandler[Kleisli[M, Logger, ?], M] =
      λ[FunctionK[Kleisli[M, Logger, ?], M]](_.run(log))

    implicit def freeStyleLoggingToM[M[_]: Monad](
        implicit log: Logger = Logger("")): FSHandler[LoggingM.Op, M] =
      freeStyleLoggingKleisli andThen freeStyleLoggingKleisliRunner(log)

  }

  object implicits extends Implicits
}
