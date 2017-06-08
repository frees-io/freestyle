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
import freestyle.logging._
import journal._

object loggingJVM {

  trait Implicits {
    implicit def freeStyleLoggingHandler[M[_], C: Manifest](
        implicit ME: MonadError[M, Throwable]): LoggingM.Handler[M] =
      new LoggingM.Handler[M] {

        val logger = Logger[C]

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
  object implicits extends Implicits

}
