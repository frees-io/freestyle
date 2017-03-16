package freestyle

import freestyle.logging.LoggingM

import scala.concurrent.Future

object algebras {

  @free
  trait NonLogging[F[_]] {
    def x: FreeS[F, Int]
  }

  implicit def nonLoggingHandler: NonLogging.Handler[Future] =
    new NonLogging.Handler[Future] {
      def x: Future[Int] = Future.successful(1)
    }

  @module
  trait App[F[_]] {
    val nonLogging: NonLogging[F]
    val loggingM: LoggingM[F]
  }

  val app = App[App.Op]
}
