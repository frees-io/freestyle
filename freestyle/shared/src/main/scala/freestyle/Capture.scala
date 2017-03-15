package freestyle

import cats.Id
import simulacrum.typeclass

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

/*
 * The method `Applicative#pure` in `cats.Applicative` is strict on its parameter. Thus, it
 *  forces the evaluation of any expression passed to it.
 *
 * However, since we need to support different types in the Handlers, we need to
 *  define a `Capture` type-class..
 */
@typeclass
trait Capture[F[_]] {
  def capture[A](a: => A): F[A]
}

object Capture extends CaptureInstances

trait CaptureInstances {

  implicit def freeStyleFutureCaptureInstance(implicit ec: ExecutionContext): Capture[Future] =
    new Capture[Future] {
      override def capture[A](a: => A): Future[A] = Future(a)
    }

  implicit val freeStyleIdCaptureInstance: Capture[Id] =
    new Capture[Id] {
      override def capture[A](a: => A): Id[A] = a
    }

  implicit val freeStyleTryCaptureInstance: Capture[Try] =
    new Capture[Try] {
      override def capture[A](a: => A): Try[A] = Try(a)
    }

}
