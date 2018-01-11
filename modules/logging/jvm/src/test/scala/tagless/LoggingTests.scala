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

package freestyle.tagless

import cats._
import cats.syntax.flatMap._
import cats.syntax.functor._
import freestyle.tagless._
import freestyle.tagless.algebras._
import freestyle.tagless.logging.LoggingM
import journal.Logger
import org.scalatest.{AsyncWordSpec, Matchers}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NoStackTrace

class LoggingTests extends AsyncWordSpec with Matchers {

  implicit override def executionContext = ExecutionContext.Implicits.global

  case object Cause extends Exception("kaboom") with NoStackTrace

  "Logging Freestyle tagless integration" should {

    import cats.instances.future._
    import freestyle.tagless.loggingJVM.implicits._

    "allow a log message to be interleaved inside a program monadic flow" in {

      def program[F[_]: Monad](implicit app: App[F]) =
        for {
          a <- app.nonLogging.x
          _ <- app.loggingM.debug("Debug Message", sourceAndLineInfo = true)
          _ <- app.loggingM.debugWithCause("Debug Message", Cause)
          _ <- app.loggingM.error("Error Message")
          _ <- app.loggingM.errorWithCause("Error Message", Cause)
          _ <- app.loggingM.info("Info Message")
          _ <- app.loggingM.infoWithCause("Info Message", Cause)
          _ <- app.loggingM.warn("Warning Message")
          _ <- app.loggingM.warnWithCause("Warning Message", Cause)
          b <- Monad[F].pure(1)
        } yield a + b

      program[Future] map { _ shouldBe 2 }

    }

    "allow injecting a Logger instance" in {
      def program[F[_]: Monad](implicit app: App[F]) =
        for {
          a <- Monad[F].pure(1)
          _ <- app.loggingM.info("Info Message")
          _ <- app.loggingM.error("Error Message")
          b <- Monad[F].pure(1)
        } yield a + b

      implicit val logger: Logger = journal.Logger("Potatoes")

      program[Future] map { _ shouldBe 2 }
    }
  }
}
