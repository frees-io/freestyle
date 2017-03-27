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

object async {

  /** An asynchronous computation that might fail. **/
  type Proc[A] = (Either[Throwable, A] => Unit) => Unit

  /** The context required to run an asynchronous computation. **/
  trait AsyncContext[M[_]] {
    def runAsync[A](fa: Proc[A]): M[A]
  }

  /** Async computation algebra. **/
  @free sealed trait AsyncM {
    def async[A](fa: Proc[A]): OpPar[A]
  }

  object implicits {
    implicit def futureAsyncContext(
        implicit ex: ExecutionContext
    ) = new AsyncContext[Future] {
      def runAsync[A](fa: Proc[A]): Future[A] = {
        val p = Promise[A]()

        ex.execute(new Runnable {
          def run() =
            fa({
              case Right(v) => p.trySuccess(v)
              case Left(ex) => p.tryFailure(ex)
            })
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
}
