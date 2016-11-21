package io.freestyle

import cats._
import cats.implicits._
import simulacrum._
import scala.util.Try

@typeclass trait Capture[F[_]] {
  def capture[A](a: => A): F[A]
}

object Capture extends CaptureInstances

trait CaptureInstances {

  import scala.concurrent.Future
  import scala.concurrent.ExecutionContext
  import scala.util.control.NonFatal

  implicit def freeStyleFutureCaptureInstance(implicit ec: ExecutionContext): Capture[Future] =
    new Capture[Future] {
      override def capture[A](a: => A): Future[A] = Future(a)
    }

  implicit def freeStyleIdCaptureInstance: Capture[Id] =
    new Capture[Id] {
      override def capture[A](a: => A): Id[A] = a.pure[Id]
    }

  implicit def freeStyleTryCaptureInstance: Capture[Try] =
    new Capture[Try] {
      override def capture[A](a: => A): Try[A] = Try(a)
    }
  
}
