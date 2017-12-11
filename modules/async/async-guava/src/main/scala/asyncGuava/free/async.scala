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

package freestyle.free.asyncGuava

import cats.~>
import com.google.common.util.concurrent._
import freestyle.free._
import freestyle.async.AsyncContext
import java.util.concurrent.{Executor => JavaExecutor}

import scala.concurrent.ExecutionContext

trait AsyncGuavaImplicits {

  class ListenableFuture2AsyncM[F[_]](implicit AC: AsyncContext[F], E: ExecutionContext)
      extends FSHandler[ListenableFuture, F] {
    override def apply[A](listenableFuture: ListenableFuture[A]): F[A] =
      AC.runAsync { cb =>
        Futures.addCallback(
          listenableFuture,
          new FutureCallback[A] {
            override def onSuccess(result: A): Unit = cb(Right(result))

            override def onFailure(t: Throwable): Unit = cb(Left(t))
          },
          new JavaExecutor {
            override def execute(command: Runnable): Unit = E.execute(command)
          }
        )
      }
  }

  implicit def listenableFuture2Async[F[_]](
      implicit AC: AsyncContext[F],
      E: ExecutionContext): ListenableFuture ~> F =
    new ListenableFuture2AsyncM[F]

  implicit def listenableVoidToListenableUnit(future: ListenableFuture[Void])(
      implicit E: ExecutionContext): ListenableFuture[Unit] =
    Futures.transformAsync(
      future,
      new AsyncFunction[Void, Unit] {
        override def apply(input: Void): ListenableFuture[Unit] =
          Futures.immediateFuture((): Unit)
      },
      new JavaExecutor {
        override def execute(command: Runnable): Unit = E.execute(command)
      }
    )

}

object implicits extends AsyncGuavaImplicits