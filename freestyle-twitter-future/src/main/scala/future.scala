package freestyle
package twitter

import com.twitter.util.Future
import cats.MonadError

class FreestyleTwitterMonad extends MonadError[Future, Throwable] with Capture[Future] {

  override def capture[A](a: => A) = Future(a)

  def pure[A](x: A): Future[A]                                   = Future.value(x)

  def flatMap[A, B](fa: Future[A])(f: A => Future[B]): Future[B] = fa.flatMap(f)

  final def tailRecM[B, C](b: B)(f: B => Future[Either[B, C]]): Future[C] =
    f(b).flatMap {
      case Left(b1) => tailRecM(b1)(f)
      case Right(c) => Future.value(c)
    }

  override def map[A, B](fa: Future[A])(f: A => B): Future[B] = fa.map(f)

  def handleErrorWith[A](fa: Future[A])(f: Throwable => Future[A]): Future[A] = fa.rescue({case e => f(e)})

  def raiseError[A](e: Throwable): Future[A] = Future.exception(e)

}

object future {

  object implicits {

    implicit val freestyleTwitterFutureInstances: MonadError[Future, Throwable] with Capture[Future] = new FreestyleTwitterMonad

  }

}
