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

import cats.arrow.FunctionK
import cats.data.Kleisli
import cats.{Applicative, Monad}
import freestyle.logging._
import freestyle.free.logging._
import journal._

object loggingJVM {

  sealed abstract class FreeSLoggingMHandler[M[_]] extends LoggingM.Handler[M] {

    import sourcecode.{File, Line}

    protected def withLogger[A](f: Logger => A): M[A]

    def debug(msg: String, srcInfo: Boolean)(implicit line: Line, file: File): M[Unit] =
      withLogger(_.debug(formatMessage(msg, srcInfo, line, file)))

    def debugWithCause(msg: String, cause: Throwable, srcInfo: Boolean)(
        implicit
        line: Line,
        file: File): M[Unit] =
      withLogger(_.debug(formatMessage(msg, srcInfo, line, file), cause))

    def error(msg: String, srcInfo: Boolean)(implicit line: Line, file: File): M[Unit] =
      withLogger(_.error(formatMessage(msg, srcInfo, line, file)))

    def errorWithCause(msg: String, cause: Throwable, srcInfo: Boolean)(
        implicit
        line: Line,
        file: File): M[Unit] =
      withLogger(_.error(formatMessage(msg, srcInfo, line, file), cause))

    def info(msg: String, srcInfo: Boolean)(implicit line: Line, file: File): M[Unit] =
      withLogger(_.info(formatMessage(msg, srcInfo, line, file)))

    def infoWithCause(msg: String, cause: Throwable, srcInfo: Boolean)(
        implicit
        line: Line,
        file: File): M[Unit] =
      withLogger(_.info(formatMessage(msg, srcInfo, line, file), cause))

    def warn(msg: String, srcInfo: Boolean)(implicit line: Line, file: File): M[Unit] =
      withLogger(_.warn(formatMessage(msg, srcInfo, line, file)))

    def warnWithCause(msg: String, cause: Throwable, srcInfo: Boolean)(
        implicit
        line: Line,
        file: File): M[Unit] =
      withLogger(_.warn(formatMessage(msg, srcInfo, line, file), cause))
  }

  trait Implicits {

    implicit def freeStyleLoggingKleisli[M[_]: Applicative](
        implicit log: Logger = Logger("")): LoggingM.Handler[Kleisli[M, Logger, ?]] =
      new FreeSLoggingMHandler[Kleisli[M, Logger, ?]] {

        protected def withLogger[A](f: Logger => A): Kleisli[M, Logger, A] =
          Kleisli.ask[M, Logger].map(f)
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
