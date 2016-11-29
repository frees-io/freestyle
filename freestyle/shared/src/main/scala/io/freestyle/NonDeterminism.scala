package io.freestyle

import cats.Monad

object nondeterminism extends NonDeterminismInstances

trait NonDeterminismInstances {

  import scala.concurrent.Future
  import scala.concurrent.ExecutionContext
  import scala.util.control.NonFatal

  implicit def freestyleParallelFutureMonad(implicit ec: ExecutionContext): Monad[Future] =
    new Monad[Future] {
      def pure[A](x: A): Future[A] = Future.successful(x)
      def flatMap[A, B](fa: Future[A])(f: A => Future[B]): Future[B] = fa.flatMap(f)

      final def tailRecM[B, C](b: B)(f: B => Future[Either[B, C]]): Future[C] =
        f(b).flatMap {
          case Left(b1) => tailRecM(b1)(f)
          case Right(c) => Future.successful(c)
        }

      override def map[A, B](fa: Future[A])(f: A => B): Future[B] = fa.map(f)

      override def ap[A, B](ff: Future[A => B])(fa: Future[A]): Future[B] =
        fa.zip(ff).map { case (a, f) => f(a) }
    }

}
