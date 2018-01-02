/*
 * Copyright 2017-2018 47 Degrees, LLC. <http://www.47deg.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package freestyle
package free

import cats.Monad

import scala.concurrent.{ExecutionContext, Future}

object nondeterminism extends NonDeterminismInstances

trait NonDeterminismInstances {

  implicit def freestyleParallelFutureMonad(implicit ec: ExecutionContext): Monad[Future] =
    new Monad[Future] {
      def pure[A](x: A): Future[A]                                   = Future.successful(x)
      def flatMap[A, B](fa: Future[A])(f: A => Future[B]): Future[B] = fa.flatMap(f)

      final def tailRecM[B, C](b: B)(f: B => Future[Either[B, C]]): Future[C] =
        f(b).flatMap {
          case Left(b1) => tailRecM(b1)(f)
          case Right(c) => Future.successful(c)
        }

      override def map[A, B](fa: Future[A])(f: A => B): Future[B] = fa.map(f)

      override def ap[A, B](ff: Future[A => B])(fa: Future[A]): Future[B] =
        fa.zip(ff).map { case (a, f) => f(a) }

      override def product[A, B](fa: Future[A], fb: Future[B]): Future[(A, B)] =
        fa.zip(fb)
    }

}
