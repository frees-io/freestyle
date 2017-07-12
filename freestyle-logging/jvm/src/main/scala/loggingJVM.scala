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
      sourceAndLineInfo: Boolean,
      line: sourcecode.Line,
      file: sourcecode.File): String =
    if (sourceAndLineInfo) s"$file:$line: $msg"
    else msg

  trait Implicits {
    implicit def freeStyleLoggingKleisli[M[_]: Monad, C: Manifest]: FSHandler[
      LoggingM.Op,
      Kleisli[M, Logger, ?]] =
      λ[FunctionK[LoggingM.Op, Kleisli[M, Logger, ?]]](op =>
        Kleisli { logger =>
          Monad[M].pure {
            op match {
              case LoggingM.DebugOP(msg, sourceAndLineInfo, line, file) =>
                logger.debug(formatMessage(msg, sourceAndLineInfo, line, file))
              case LoggingM.DebugWithCauseOP(msg, cause, sourceAndLineInfo, line, file) =>
                logger.debug(formatMessage(msg, sourceAndLineInfo, line, file), cause)
              case LoggingM.ErrorOP(msg, sourceAndLineInfo, line, file) =>
                logger.error(formatMessage(msg, sourceAndLineInfo, line, file))
              case LoggingM.ErrorWithCauseOP(msg, cause, sourceAndLineInfo, line, file) =>
                logger.error(formatMessage(msg, sourceAndLineInfo, line, file), cause)
              case LoggingM.InfoOP(msg, sourceAndLineInfo, line, file) =>
                logger.info(formatMessage(msg, sourceAndLineInfo, line, file))
              case LoggingM.InfoWithCauseOP(msg, cause, sourceAndLineInfo, line, file) =>
                logger.info(formatMessage(msg, sourceAndLineInfo, line, file), cause)
              case LoggingM.WarnOP(msg, sourceAndLineInfo, line, file) =>
                logger.warn(formatMessage(msg, sourceAndLineInfo, line, file))
              case LoggingM.WarnWithCauseOP(msg, cause, sourceAndLineInfo, line, file) =>
                logger.warn(formatMessage(msg, sourceAndLineInfo, line, file), cause)
            }
          }
      })

    implicit def freeStyleLoggingKleisliRunner[M[_]](
        log: Logger): FSHandler[Kleisli[M, Logger, ?], M] =
      λ[FunctionK[Kleisli[M, Logger, ?], M]](_.run(log))

    implicit def freeStyleLoggingToM[M[_]: Monad](
        implicit log: Logger = Logger("")): FSHandler[LoggingM.Op, M] =
      freeStyleLoggingKleisli andThen freeStyleLoggingKleisliRunner(log)

  }

  object implicits extends Implicits
}
