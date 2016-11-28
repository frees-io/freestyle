package io.freestyle

import cats.Applicative
import cats.instances.future._
import io.freestyle.implicits._
import io.freestyle.logging._
import io.freestyle.logging.implicits._
import org.scalatest._

import scala.concurrent._

class LoggingTests extends AsyncWordSpec with Matchers {

  implicit override def executionContext = ExecutionContext.Implicits.global

  import algebras._

  "Fetch Freestyle integration" should {

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

object algebras {

  @free trait NonLogging[F[_]] {
    def x: FreeS[F, Int]
  }

  implicit def nonLoggingInterpreter: NonLogging.Interpreter[Future] =
    new NonLogging.Interpreter[Future] {
      def xImpl: Future[Int] = Future.successful(1)
    }

  @module trait App[F[_]] {
    val nonLogging: NonLogging[F]
    val loggingM: LoggingM[F]
  }

  val app = App[App.T]

}