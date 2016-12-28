package io.freestyle

import cats.Applicative
import cats.instances.future._
import io.freestyle.implicits._
import io.freestyle.loggingJVM.implicits._
import org.scalatest._

import scala.concurrent._

class LoggingTests extends AsyncWordSpec with Matchers {

  implicit override def executionContext = ExecutionContext.Implicits.global

  import algebras._

  "Logging Freestyle integration" should {

    "allow a log message to be interleaved inside a program monadic flow" in {
      val program = for {
        a <- app.nonLogging.x
        _ <- app.loggingM.debug("Message")
        b <- Applicative[FreeS[App.T, ?]].pure(1)
      } yield a + b
      program.exec[Future] map { _ shouldBe 2 }
    }

  }
}
