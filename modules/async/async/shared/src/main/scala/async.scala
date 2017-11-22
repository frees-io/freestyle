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

import scala.concurrent._
import scala.util._

object async {

  /** An asynchronous computation that might fail. **/
  type Proc[A] = (Either[Throwable, A] => Unit) => Unit

  /** The context required to run an asynchronous computation. **/
  trait AsyncContext[M[_]] {
    def runAsync[A](fa: Proc[A]): M[A]
  }

  /** Async computation algebra. **/
  @free sealed trait AsyncM {
    def async[A](fa: Proc[A]): FS[A]
  }

  class Future2AsyncM[F[_]](implicit AC: AsyncContext[F], E: ExecutionContext)
    extends FSHandler[Future, F] {
    override def apply[A](future: Future[A]): F[A] =
      AC.runAsync { cb =>
        E.execute(new Runnable {
          def run(): Unit = future.onComplete {
            case Failure(e) => cb(Left(e))
            case Success(r) => cb(Right(r))
          }
        })
      }
  }

  trait Implicits {
    implicit def futureAsyncContext(
        implicit ec: ExecutionContext
    ) = new AsyncContext[Future] {
      def runAsync[A](fa: Proc[A]): Future[A] = {
        val p = Promise[A]()

        ec.execute(new Runnable {
          def run() = fa(_.fold(p.tryFailure, p.trySuccess))
        })

        p.future
      }
    }

    implicit def freeStyleAsyncMHandler[M[_]](
        implicit MA: AsyncContext[M]
    ): AsyncM.Handler[M] =
      new AsyncM.Handler[M] {
        def async[A](fa: Proc[A]): M[A] =
          MA.runAsync(fa)
      }
  }

  trait Syntax {

    implicit def futureOps[A](f: Future[A]): FutureOps[A] = new FutureOps(f)

    final class FutureOps[A](f: Future[A]) {

      def to[F[_]](implicit AC: AsyncContext[F], E: ExecutionContext): F[A] =
        new Future2AsyncM[F].apply(f)

    }

  }

  object implicits extends Implicits with Syntax
}
