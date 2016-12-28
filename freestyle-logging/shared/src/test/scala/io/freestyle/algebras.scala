package io.freestyle

import io.freestyle.logging.LoggingM

import scala.concurrent.Future

object algebras {

  @free
  trait NonLogging[F[_]] {
    def x: FreeS[F, Int]
  }

  implicit def nonLoggingInterpreter: NonLogging.Interpreter[Future] =
    new NonLogging.Interpreter[Future] {
      def xImpl: Future[Int] = Future.successful(1)
    }

  @module
  trait App[F[_]] {
    val nonLogging: NonLogging[F]
    val loggingM: LoggingM[F]
  }

  val app = App[App.T]
}
