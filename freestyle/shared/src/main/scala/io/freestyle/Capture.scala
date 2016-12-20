package io.freestyle

import cats.Id
import simulacrum.typeclass
import scala.util.Try

@typeclass
trait Capture[F[_]] {
  def capture[A](a: => A): F[A]
}

object Capture extends CaptureInstances

trait CaptureInstances {

  import scala.concurrent.{ExecutionContext, Future}
  import scala.util.control.NonFatal

  implicit def freeStyleFutureCaptureInstance(implicit ec: ExecutionContext): Capture[Future] =
    new Capture[Future] {
      override def capture[A](a: => A): Future[A] = Future(a)
    }

  implicit def freeStyleIdCaptureInstance: Capture[Id] =
    new Capture[Id] {
      override def capture[A](a: => A): Id[A] = a
    }

  implicit def freeStyleTryCaptureInstance: Capture[Try] =
    new Capture[Try] {
      override def capture[A](a: => A): Try[A] = Try(a)
    }

}
