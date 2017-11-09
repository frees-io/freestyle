/*
 * Copyright 2017 47 Degrees, LLC. <http://www.47deg.com>
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

import cats.effect.Async
import scala.concurrent._

object async {

  /** An asynchronous computation that might fail. **/
  type Proc[A] = (Either[Throwable, A] => Unit) => Unit

  /** Async computation algebra. **/
  @free sealed trait AsyncM {
    def async[A](fa: Proc[A]): FS[A]
  }

  trait Implicits {
    implicit def futureAsync(implicit ec: ExecutionContext): Async[Future] =
      new Async[Future] {
        def async[A](fa: Proc[A]): Future[A] = {
          val p = Promise[A]()

          ec.execute(new Runnable {
            def run(): Unit = fa(_.fold(p.tryFailure, p.trySuccess))
          })

          p.future
        }

        override def suspend[A](thunk: => Future[A]): Future[A] =
          Future().flatMap(_ => thunk)

        override def raiseError[A](e: Throwable): Future[A] = Future.failed(e)

        override def handleErrorWith[A](fa: Future[A])(f: Throwable => Future[A]): Future[A] =
          fa.recoverWith {
            case t: Throwable => f(t)
          }

        override def pure[A](x: A): Future[A] = Future.successful(x)

        override def flatMap[A, B](fa: Future[A])(f: A => Future[B]): Future[B] =
          fa.flatMap(f)

        override def tailRecM[A, B](a: A)(f: A => Future[Either[A, B]]): Future[B] =
          f(a).flatMap {
            case Left(e)  => tailRecM(e)(f)
            case Right(c) => pure(c)
          }
      }

    implicit def freeStyleAsyncMHandler[M[_]](implicit A: Async[M]): AsyncM.Handler[M] =
      new AsyncM.Handler[M] {
        def async[A](fa: Proc[A]): M[A] =
          A.async(fa)
      }
  }

  object implicits extends Implicits
}
