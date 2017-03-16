package freestyle

import freestyle.logging.LoggingM

import scala.concurrent.Future

object algebras {

  @free
  trait NonLogging{
    def x: Oper.Seq[Int]
  }

  implicit def nonLoggingHandler: NonLogging.Handler[Future] =
    new NonLogging.Handler[Future] {
      def x: Future[Int] = Future.successful(1)
    }

  @module
  trait App {
    val nonLogging: NonLogging
    val loggingM: LoggingM
  }

  val app = App[App.Op]
}
