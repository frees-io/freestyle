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

import scala.util._
import scala.concurrent._

object async {

  /** An asynchronous computation that might fail. **/
  type Proc[A] = (Either[Throwable, A] => Unit) => Unit

  /** The context required to run an asynchronous computation. **/
  trait AsyncContext[M[_]] {
    def runAsync[A](fa: Proc[A]): M[A]
  }

  def future2AsyncM[F[_], A](
      future: Future[A])(implicit AC: AsyncContext[F], E: ExecutionContext): F[A] =
    AC.runAsync { cb =>
      E.execute(new Runnable {
        def run(): Unit = future.onComplete {
          case Failure(e) => cb(Left(e))
          case Success(r) => cb(Right(r))
        }
      })
    }

  trait Implicits {
    implicit def futureAsyncContext(implicit ec: ExecutionContext) = new AsyncContext[Future] {
      def runAsync[A](fa: Proc[A]): Future[A] = {
        val p = Promise[A]()

        ec.execute(new Runnable {
          def run() = fa(_.fold(p.tryFailure, p.trySuccess))
        })

        p.future
      }
    }
  }

  trait Syntax {

    implicit def futureOps[A](f: Future[A]): FutureOps[A] = new FutureOps(f)

    final class FutureOps[A](f: Future[A]) {

      @deprecated("Use unsafeTo instead.", "0.5.4")
      def to[F[_]](implicit AC: AsyncContext[F], E: ExecutionContext): F[A] = future2AsyncM[F, A](f)

      def unsafeTo[F[_]](implicit AC: AsyncContext[F], E: ExecutionContext): F[A] = future2AsyncM[F, A](f)

    }

  }

}
