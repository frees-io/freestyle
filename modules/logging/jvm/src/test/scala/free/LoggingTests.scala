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

import cats.instances.future._
import cats.{Id, Monad}
import freestyle.free.implicits._
import freestyle.free.loggingJVM.implicits._
import org.scalatest.{AsyncWordSpec, Matchers}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NoStackTrace

class LoggingTests extends AsyncWordSpec with Matchers {

  implicit override def executionContext = ExecutionContext.Implicits.global

  import algebras._

  "Logging Freestyle integration" should {

    case object Cause extends Exception("kaboom") with NoStackTrace

    "allow a log message to be interleaved inside a program monadic flow" in {
      val program = for {
        a <- app.nonLogging.x
        _ <- app.loggingM.debug("Debug Message", sourceAndLineInfo = true)
        _ <- app.loggingM.debugWithCause("Debug Message", Cause)
        _ <- app.loggingM.error("Error Message")
        _ <- app.loggingM.errorWithCause("Error Message", Cause)
        _ <- app.loggingM.info("Info Message")
        _ <- app.loggingM.infoWithCause("Info Message", Cause)
        _ <- app.loggingM.warn("Warning Message")
        _ <- app.loggingM.warnWithCause("Warning Message", Cause)
        b <- FreeS.pure(1)
      } yield a + b
      program.interpret[Future] map { _ shouldBe 2 }
    }

    "not depend on MonadError, thus allowing use of Monads without MonadError, like Id, for test algebras" in {
      val program = for {
        a <- app.nonLogging.x
        _ <- app.loggingM.info("Info Message")
        _ <- app.loggingM.infoWithCause("Info Message", Cause)
        b <- FreeS.pure(1)
      } yield a + b
      program.interpret[TestAlgebra].run("configHere") shouldBe 2
    }

    "allow injecting a Logger instance" in {
      val program = for {
        a <- FreeS.pure(1)
        _ <- app.loggingM.info("Info Message")
        _ <- app.loggingM.error("Error Message")
        b <- FreeS.pure(1)
      } yield a + b

      implicit val logger = journal.Logger("Potatoes")

      program
        .interpret[TestAlgebra]
        .run("configHere") shouldEqual 2
    }
  }
}
